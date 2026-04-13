package org.smm.archetype.client.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 方法级限流注解。
 * <p>
 * 基于 Bucket4j 令牌桶算法，支持 SpEL 表达式提取限流 Key，
 * 允许不同维度（如用户ID、IP等）独立限流。
 * <p>
 * 使用示例：
 * <pre>{@code
 * @RateLimit(capacity = 100, refillTokens = 10, refillDuration = 1, key = "#userId")
 * public Response doSomething(Long userId) { ... }
 * }</pre>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 令牌桶容量（最大突发请求数）。
     */
    double capacity() default 10;

    /**
     * 每次补充的令牌数。
     */
    double refillTokens() default 10;

    /**
     * 补充令牌的时间窗口长度。
     */
    long refillDuration() default 1;

    /**
     * 补充令牌的时间单位。
     */
    TimeUnit refillUnit() default TimeUnit.SECONDS;

    /**
     * 限流 Key 的 SpEL 表达式。
     * <p>
     * 支持方法参数引用，如 {@code #userId}、{@code #request.id}。
     * 为空时使用方法全限定名作为 Key。
     */
    String key() default "";

    /**
     * 限流时的降级策略。
     */
    LimitFallback fallback() default LimitFallback.REJECT;

    /**
     * 降级方法名（仅当 fallback = FALLBACK 时生效）。
     * <p>
     * 降级方法必须在同一个 Bean 中，签名与原方法兼容。
     */
    String fallbackMethod() default "";
}
