package org.smm.archetype.shared.aspect.idempotent;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.smm.archetype.client.cache.CacheClient;
import org.smm.archetype.exception.BizException;
import org.smm.archetype.exception.CommonErrorCode;

import java.time.Duration;

/**
 * 幂等防护 AOP 切面。
 * <p>
 * 拦截标注了 {@link Idempotent} 注解的方法，通过 CacheClient 实现幂等校验。
 * <p>
 * Key 存储使用 CacheClient 的 TTL 能力自动过期。
 */
@Slf4j
@Aspect
public class IdempotentAspect {

    private final CacheClient           cacheClient;
    private final IdempotentKeyResolver keyResolver;

    public IdempotentAspect(CacheClient cacheClient) {
        this.cacheClient = cacheClient;
        this.keyResolver = new IdempotentKeyResolver();
    }

    /**
     * 幂等校验切面
     */
    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String key = resolveKey(joinPoint, idempotent);
        long timeoutMillis = idempotent.timeUnit().toMillis(idempotent.timeout());

        // 检查缓存中是否已存在未过期的 Key
        if (Boolean.TRUE.equals(cacheClient.hasKey(key))) {
            log.warn("幂等拦截：key={}, message={}", key, idempotent.message());
            throw new BizException(CommonErrorCode.ILLEGAL_ARGUMENT, idempotent.message());
        }

        // 首次调用 → 放入缓存，TTL 为幂等窗口时间
        cacheClient.put(key, "1", Duration.ofMillis(timeoutMillis));

        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            // 执行失败，移除标记允许重试
            cacheClient.delete(key);
            throw ex;
        }
    }

    /**
     * 解析幂等 Key（委托给 {@link IdempotentKeyResolver}）
     */
    String resolveKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return keyResolver.resolve(
                signature.getDeclaringTypeName(),
                signature.getName(),
                signature.getParameterNames(),
                joinPoint.getArgs(),
                idempotent
        );
    }

}
