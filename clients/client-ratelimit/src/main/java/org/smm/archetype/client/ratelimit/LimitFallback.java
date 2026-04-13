package org.smm.archetype.client.ratelimit;

/**
 * 限流降级策略枚举。
 */
public enum LimitFallback {
    /**
     * 拒绝请求，抛出 {@link org.smm.archetype.exception.BizException}。
     */
    REJECT,

    /**
     * 阻塞等待，直到获取到令牌。
     */
    WAIT,

    /**
     * 执行降级方法（由 {@link RateLimit#fallbackMethod()} 指定）。
     */
    FALLBACK
}
