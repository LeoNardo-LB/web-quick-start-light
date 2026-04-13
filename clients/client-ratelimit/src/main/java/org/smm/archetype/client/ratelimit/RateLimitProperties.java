package org.smm.archetype.client.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 限流客户端配置属性。
 * <p>
 * 前缀：middleware.ratelimit
 *
 * <pre>
 * middleware:
 *   ratelimit:
 *     enabled: true
 *     default-capacity: 10
 *     default-refill-tokens: 10
 *     default-refill-duration: 1
 * </pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "middleware.ratelimit")
public class RateLimitProperties {

    /**
     * 是否启用限流，默认 true。
     */
    private boolean enabled = true;

    /**
     * 默认桶容量（最大突发请求数），默认 10。
     */
    private double defaultCapacity = 10;

    /**
     * 默认每次补充的令牌数，默认 10。
     */
    private double defaultRefillTokens = 10;

    /**
     * 默认补充令牌的时间窗口长度（秒），默认 1。
     */
    private long defaultRefillDuration = 1;
}
