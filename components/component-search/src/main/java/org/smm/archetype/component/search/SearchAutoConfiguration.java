package org.smm.archetype.component.search;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 搜索组件自动配置。
 * <p>
 * 条件：component.search.enabled=true（默认 true）
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "component.search", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SearchProperties.class)
public class SearchAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SearchComponent.class)
    public SearchComponent searchComponent() {
        return new SimpleSearchComponent();
    }
}
