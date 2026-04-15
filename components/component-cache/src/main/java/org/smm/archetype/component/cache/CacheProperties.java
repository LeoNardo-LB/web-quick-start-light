package org.smm.archetype.component.cache;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 缓存客户端配置属性。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "component.cache")
public class CacheProperties {
    /** 初始容量 */
    private Integer initialCapacity = 1000;
    /** 最大缓存条目数 */
    private Long maximumSize = 10000L;
    /** 写入后过期时间 */
    private Duration expireAfterWrite = Duration.ofDays(30);
    /** 访问后过期时间 */
    private Duration expireAfterAccess = Duration.ofDays(30);
}
