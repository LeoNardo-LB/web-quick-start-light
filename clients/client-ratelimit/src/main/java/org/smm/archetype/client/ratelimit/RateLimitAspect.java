package org.smm.archetype.client.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.smm.archetype.exception.BizException;
import org.smm.archetype.exception.CommonErrorCode;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * {@link RateLimit} 注解的 AOP 切面。
 * <p>
 * 基于 Bucket4j 令牌桶算法实现方法级限流。
 * <ul>
 *   <li>REJECT：拒绝请求，抛出 BizException</li>
 *   <li>WAIT：阻塞等待令牌补充</li>
 *   <li>FALLBACK：调用降级方法</li>
 * </ul>
 */
@Aspect
@Slf4j
public class RateLimitAspect {

    /**
     * Bucket 缓存（生产环境由 AutoConfiguration 注入 Caffeine Cache，
     * 此 Map 作为默认实现，也用于单元测试注入 mock bucket）。
     */
    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    /**
     * 构建 Bucket Key。
     * <p>
     * 如果 SpEL 表达式非空，解析后拼接方法签名；
     * 否则使用方法全限定名。
     *
     * @param method        目标方法
     * @param args          方法参数
     * @param keyExpression SpEL 表达式
     * @return bucket key
     */
    public static String buildBucketKey(Method method, Object[] args, String keyExpression) {
        String baseKey = method.getDeclaringClass().getSimpleName() + "." + method.getName();

        if (keyExpression == null || keyExpression.isBlank()) {
            return baseKey;
        }

        String resolvedKey = SpelKeyResolver.resolve(method, args, keyExpression);
        return baseKey + ":" + resolvedKey;
    }

    /**
     * 测试辅助方法：向 bucket 缓存中注入 mock bucket。
     */
    public void putBucket(String key, Bucket bucket) {
        bucketCache.put(key, bucket);
    }

    @Pointcut("@annotation(org.smm.archetype.client.ratelimit.RateLimit)")
    public void rateLimitCut() {}

    @Around("rateLimitCut()")
    public Object doRateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        if (rateLimit == null) {
            return joinPoint.proceed();
        }

        // 构建 bucket key
        String keyExpression = rateLimit.key();
        String bucketKey = buildBucketKey(method, joinPoint.getArgs(), keyExpression);

        // 获取或创建 Bucket
        Bucket bucket = bucketCache.computeIfAbsent(bucketKey, k ->
                BucketFactory.createBucket(rateLimit.capacity(), rateLimit.refillTokens(),
                        rateLimit.refillDuration(), rateLimit.refillUnit()));

        // 尝试消费令牌
        if (bucket.tryConsume(1)) {
            return joinPoint.proceed();
        }

        // 令牌不足，根据 fallback 策略处理
        return handleFallback(joinPoint, rateLimit, bucket);
    }

    private Object handleFallback(ProceedingJoinPoint joinPoint, RateLimit rateLimit, Bucket bucket) throws Throwable {
        switch (rateLimit.fallback()) {
            case REJECT -> {
                log.debug("[RateLimit] Request rejected for key: {}", buildBucketKey(
                        ((MethodSignature) joinPoint.getSignature()).getMethod(),
                        joinPoint.getArgs(), rateLimit.key()));
                throw new BizException(CommonErrorCode.RATE_LIMIT_EXCEEDED);
            }
            case WAIT -> {
                ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
                if (probe.isConsumed()) {
                    return joinPoint.proceed();
                }
                long nanosToWait = probe.getNanosToWaitForRefill();
                log.debug("[RateLimit] Waiting {} ns for token refill", nanosToWait);
                try {
                    TimeUnit.NANOSECONDS.sleep(nanosToWait);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new BizException(CommonErrorCode.RATE_LIMIT_EXCEEDED);
                }
                // 等待后再次尝试消费
                if (bucket.tryConsume(1)) {
                    return joinPoint.proceed();
                }
                throw new BizException(CommonErrorCode.RATE_LIMIT_EXCEEDED);
            }
            case FALLBACK -> {
                String fallbackMethodName = rateLimit.fallbackMethod();
                if (fallbackMethodName == null || fallbackMethodName.isBlank()) {
                    throw new BizException(CommonErrorCode.RATE_LIMIT_EXCEEDED);
                }
                Object target = joinPoint.getTarget();
                try {
                    Method fallbackMethod = target.getClass().getMethod(fallbackMethodName);
                    return fallbackMethod.invoke(target);
                } catch (NoSuchMethodException e) {
                    log.error("[RateLimit] Fallback method '{}' not found on {}",
                            fallbackMethodName, target.getClass().getSimpleName());
                    throw new BizException(CommonErrorCode.RATE_LIMIT_EXCEEDED);
                }
            }
            default -> throw new BizException(CommonErrorCode.RATE_LIMIT_EXCEEDED);
        }
    }
}
