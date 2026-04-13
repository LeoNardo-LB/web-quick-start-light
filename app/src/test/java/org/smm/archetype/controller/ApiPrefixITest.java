package org.smm.archetype.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.IntegrationTestBase;

/**
 * API 前缀集成测试 — 验证所有 Controller 端点统一使用 /api 前缀
 * <p>
 * 覆盖场景:
 * 1. GET /api/system/configs → 正常返回
 * 2. GET /api/test/hello → 正常返回
 * 3. GET /system/configs（无 /api 前缀）→ 404
 * 4. GET /test/hello（无 /api 前缀）→ 404
 */
class ApiPrefixITest extends IntegrationTestBase {

    @Nested
    @DisplayName("带 /api 前缀的端点")
    class WithApiPrefix {

        @Test
        @DisplayName("MFT: GET /api/system/configs → 正常返回 15 条配置")
        void should_returnConfigs_whenApiPrefixPresent() {
            webTestClient.get().uri("/api/system/configs")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(15);
        }

        @Test
        @DisplayName("MFT: GET /api/test/hello → 正常返回 Hello World")
        void should_returnHello_whenApiPrefixPresent() {
            webTestClient.get().uri("/api/test/hello")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true);
        }
    }

    @Nested
    @DisplayName("不带 /api 前缀的端点（应被拒绝）")
    class WithoutApiPrefix {

        @Test
        @DisplayName("DIR: GET /system/configs（无 /api 前缀）→ 被认证拦截器拒绝（401）")
        void should_return401_whenNoApiPrefix_systemConfigs() {
            webTestClient.get().uri("/system/configs")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("DIR: GET /test/hello（无 /api 前缀）→ 被认证拦截器拒绝（401）")
        void should_return401_whenNoApiPrefix_testHello() {
            webTestClient.get().uri("/test/hello")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }
    }
}
