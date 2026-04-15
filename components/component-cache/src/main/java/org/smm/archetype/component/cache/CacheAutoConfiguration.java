package org.smm.archetype.component.cache;

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
 *   <li>component.cache.enabled=true（默认 true）</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnClass(Caffeine.class)
@ConditionalOnProperty(prefix = "component.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(CacheProperties.class)
public class CacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CacheComponent.class)
    public CacheComponent cacheClient(CacheProperties properties) {
        return new CaffeineCacheComponent(
                properties.getInitialCapacity(),
                properties.getMaximumSize(),
                properties.getExpireAfterWrite()
        );
    }
}
