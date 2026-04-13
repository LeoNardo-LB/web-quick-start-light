package org.smm.archetype.client.idempotent;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.smm.archetype.exception.BizException;
import org.smm.archetype.exception.CommonErrorCode;

/**
 * 幂等防护 AOP 切面。
 * <p>
 * 拦截标注了 {@link Idempotent} 注解的方法，通过 Caffeine 缓存实现幂等校验。
 * <p>
 * Key 存储时间戳，切面内手动检查是否超过 {@link Idempotent#timeout()} 窗口。
 */
@Slf4j
@Aspect
public class IdempotentAspect {

    private final Cache<String, Long> cache;
    private final IdempotentKeyResolver keyResolver;

    public IdempotentAspect(Cache<String, Long> cache) {
        this.cache = cache;
        this.keyResolver = new IdempotentKeyResolver();
    }

    /**
     * 幂等校验切面
     */
    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String key = resolveKey(joinPoint, idempotent);
        long timeoutMillis = idempotent.timeUnit().toMillis(idempotent.timeout());
        long now = System.currentTimeMillis();

        // 检查缓存中是否已存在未过期的 Key
        Long existingTimestamp = cache.getIfPresent(key);
        if (existingTimestamp != null && (now - existingTimestamp) < timeoutMillis) {
            // 幂等窗口内重复调用 → 抛 BizException
            log.warn("幂等拦截：key={}, message={}", key, idempotent.message());
            throw new BizException(CommonErrorCode.ILLEGAL_ARGUMENT, idempotent.message());
        }

        // 首次调用或已过期 → 放入缓存
        cache.put(key, now);

        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            // 执行失败，移除标记允许重试
            cache.invalidate(key);
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
