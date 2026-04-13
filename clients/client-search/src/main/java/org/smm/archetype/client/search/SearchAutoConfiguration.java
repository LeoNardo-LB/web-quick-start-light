package org.smm.archetype.client.search;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 搜索客户端自动配置。
 * <p>
 * 条件：middleware.search.enabled=true（默认 true）
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "middleware.search", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SearchProperties.class)
public class SearchAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SearchClient.class)
    public SearchClient searchClient() {
        return new SimpleSearchClient();
    }
}
