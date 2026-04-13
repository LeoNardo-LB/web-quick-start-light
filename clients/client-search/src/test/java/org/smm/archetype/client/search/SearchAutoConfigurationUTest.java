package org.smm.archetype.client.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SearchAutoConfiguration 条件装配测试。
 * <p>
 * 使用 ApplicationContextRunner 验证：
 * - 默认属性（matchIfMissing=true）→ SearchClient Bean 创建
 * - enabled=false → 无 SearchClient Bean
 * - 自定义 Bean 存在 → 不被覆盖
 */
@DisplayName("SearchAutoConfiguration 条件装配")
class SearchAutoConfigurationUTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SearchAutoConfiguration.class));

    @Test
    @DisplayName("enabled 未设置（默认 true）→ SearchClient Bean 存在")
    void should_create_searchClient_when_enabled_default() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasSingleBean(SearchClient.class);
                    assertThat(context.getBean(SearchClient.class))
                            .isInstanceOf(SimpleSearchClient.class);
                });
    }

    @Test
    @DisplayName("enabled=true → SearchClient Bean 存在")
    void should_create_searchClient_when_enabled() {
        contextRunner
                .withPropertyValues("middleware.search.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(SearchClient.class);
                    assertThat(context.getBean(SearchClient.class))
                            .isInstanceOf(SimpleSearchClient.class);
                });
    }

    @Test
    @DisplayName("enabled=false → 无 SearchClient Bean")
    void should_not_create_searchClient_when_disabled() {
        contextRunner
                .withPropertyValues("middleware.search.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(SearchClient.class);
                });
    }

    @Test
    @DisplayName("自定义 SearchClient Bean 存在 → 不被覆盖")
    void should_not_override_custom_searchClient() {
        contextRunner
                .withUserConfiguration(CustomSearchClientConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(SearchClient.class);
                    assertThat(context.getBean(SearchClient.class))
                            .isInstanceOf(CustomSearchClient.class)
                            .isNotInstanceOf(SimpleSearchClient.class);
                });
    }

    @Test
    @DisplayName("SearchProperties 应正确绑定")
    void should_bind_properties() {
        contextRunner
                .withPropertyValues(
                        "middleware.search.default-page-size=20",
                        "middleware.search.max-page-size=200"
                )
                .run(context -> {
                    SearchProperties props = context.getBean(SearchProperties.class);
                    assertThat(props.getDefaultPageSize()).isEqualTo(20);
                    assertThat(props.getMaxPageSize()).isEqualTo(200);
                });
    }

    @Test
    @DisplayName("SearchProperties 默认值应正确")
    void should_use_default_properties() {
        contextRunner
                .run(context -> {
                    SearchProperties props = context.getBean(SearchProperties.class);
                    assertThat(props.getDefaultPageSize()).isEqualTo(10);
                    assertThat(props.getMaxPageSize()).isEqualTo(100);
                });
    }

    /**
     * 自定义 SearchClient 配置，验证 ConditionalOnMissingBean 不覆盖。
     */
    @Configuration
    static class CustomSearchClientConfig {

        @Bean
        SearchClient customSearchClient() {
            return new CustomSearchClient();
        }
    }

    /**
     * 自定义 SearchClient 实现，用于验证 Bean 不被覆盖。
     */
    static class CustomSearchClient implements SearchClient {

        @Override
        public void index(String indexName, String id, Object document) {
        }

        @Override
        public void bulkIndex(String indexName, java.util.List<java.util.Map<String, Object>> documents) {
        }

        @Override
        public void delete(String indexName, String id) {
        }

        @Override
        public java.util.Map<String, Object> get(String indexName, String id) {
            return java.util.Collections.emptyMap();
        }

        @Override
        public org.smm.archetype.client.dto.SearchResult search(org.smm.archetype.client.dto.SearchQuery query) {
            return new org.smm.archetype.client.dto.SearchResult(0L, java.util.Collections.emptyList(), 1, 10);
        }

        @Override
        public java.util.Map<String, Long> aggregate(String indexName, String fieldName, org.smm.archetype.client.dto.SearchQuery query) {
            return java.util.Collections.emptyMap();
        }

        @Override
        public boolean exists(String indexName, String id) {
            return false;
        }

        @Override
        public boolean existsIndex(String indexName) {
            return false;
        }

        @Override
        public boolean createIndex(String indexName) {
            return false;
        }

        @Override
        public boolean deleteIndex(String indexName) {
            return false;
        }

        @Override
        public void refresh(String indexName) {
        }

        @Override
        public long count(String indexName) {
            return 0;
        }

        @Override
        public void bulkDelete(String indexName, java.util.List<String> ids) {
        }

        @Override
        public void update(String indexName, String id, java.util.Map<String, Object> document) {
        }

        @Override
        public org.smm.archetype.client.dto.SearchResult search(String indexName, String keyword, int pageNo, int pageSize) {
            return new org.smm.archetype.client.dto.SearchResult(0L, java.util.Collections.emptyList(), pageNo, pageSize);
        }
    }
}
