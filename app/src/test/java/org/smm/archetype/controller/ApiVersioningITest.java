package org.smm.archetype.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.IntegrationTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API 版本控制集成测试 — 验证 Spring Boot 4 MVC API Versioning 配置
 * <p>
 * 覆盖场景:
 * 1. 携带 API-Version: 1.0 Header → 正常返回（匹配无版本标注方法）
 * 2. 不携带 API-Version Header → 正常返回（使用默认版本）
 * 3. 携带不匹配版本 API-Version: 99.0 → 路由到无版本方法
 * 4. WebMvcProperties 配置验证（默认版本、Header 名称）
 */
class ApiVersioningITest extends IntegrationTestBase {

    @Autowired
    private WebMvcProperties webMvcProperties;

    @Nested
    @DisplayName("API Versioning 配置验证")
    class ConfigurationCheck {

        @Test
        @DisplayName("MFT: spring.mvc.apiversion.default 已配置为 1.0")
        void should_defaultVersionBeConfigured() {
            assertThat(webMvcProperties.getApiversion().getDefaultVersion())
                    .as("spring.mvc.apiversion.default 应配置为 1.0")
                    .isEqualTo("1.0");
        }

        @Test
        @DisplayName("MFT: spring.mvc.apiversion.use.header 已配置为 API-Version")
        void should_headerNameBeConfigured() {
            assertThat(webMvcProperties.getApiversion().getUse().getHeader())
                    .as("spring.mvc.apiversion.use.header 应配置为 API-Version")
                    .isEqualTo("API-Version");
        }
    }

    @Nested
    @DisplayName("API-Version Header 路由行为")
    class HeaderRouting {

        @Test
        @DisplayName("MFT: 携带 API-Version: 1.0 Header → 正常返回（匹配无版本标注方法）")
        void should_returnOk_whenApiVersionHeaderIsDefaultVersion() {
            webTestClient.get().uri("/api/test/hello")
                    .header("API-Version", "1.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("MFT: 不携带 API-Version Header → 正常返回（使用默认版本）")
        void should_returnOk_whenNoApiVersionHeader() {
            webTestClient.get().uri("/api/test/hello")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("DIR: 携带不匹配版本 API-Version: 99.0 → 无版本方法不匹配，返回失败")
        void should_returnFailure_whenVersionNotMatched_noVersionedMethod() {
            // Spring Boot 4 API Versioning: 无版本标注方法仅匹配默认版本（1.0），
            // 不匹配非默认版本（99.0），框架抛出异常由全局异常处理器兜底
            webTestClient.get().uri("/api/test/hello")
                    .header("API-Version", "99.0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false);
        }
    }
}
