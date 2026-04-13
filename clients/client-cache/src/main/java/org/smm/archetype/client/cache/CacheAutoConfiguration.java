package org.smm.archetype.client.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 缓存客户端自动配置。
 * <p>
 * 条件：
 * <ul>
 *   <li>classpath 中存在 Caffeine</li>
 *   <li>middleware.cache.enabled=true（默认 true）</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnClass(Caffeine.class)
@ConditionalOnProperty(prefix = "middleware.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(CacheProperties.class)
public class CacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CacheClient.class)
    public CacheClient cacheClient(CacheProperties properties) {
        return new CaffeineCacheClient(
                properties.getInitialCapacity(),
                properties.getMaximumSize(),
                properties.getExpireAfterWrite()
        );
    }
}
