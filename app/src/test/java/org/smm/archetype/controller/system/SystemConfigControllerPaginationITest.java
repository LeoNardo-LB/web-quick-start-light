package org.smm.archetype.controller.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.IntegrationTestBase;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * 系统配置控制器分页集成测试 — 验证分页 API 端点
 */
class SystemConfigControllerPaginationITest extends IntegrationTestBase {

    @Nested
    @DisplayName("GET /api/system/configs/page")
    class FindByPage {

        @Test
        @DisplayName("MFT: 无参数分页查询返回分页结果，total=15")
        void should_returnPaginatedResult_withDefaults() {
            webTestClient.get().uri("/api/system/configs/page")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(15)
                    .jsonPath("$.pageNo").isEqualTo(1)
                    .jsonPath("$.pageSize").isEqualTo(20)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(15);
        }

        @Test
        @DisplayName("MFT: 指定 pageNo=1&pageSize=5 返回 5 条记录")
        void should_return5Records_whenPageSizeIs5() {
            webTestClient.get().uri("/api/system/configs/page?pageNo=1&pageSize=5")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(15)
                    .jsonPath("$.pageNo").isEqualTo(1)
                    .jsonPath("$.pageSize").isEqualTo(5)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(5);
        }

        @Test
        @DisplayName("MFT: 第二页 pageNo=2&pageSize=5 返回 5 条记录")
        void should_returnSecondPage_whenPageNoIs2() {
            webTestClient.get().uri("/api/system/configs/page?pageNo=2&pageSize=5")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(15)
                    .jsonPath("$.pageNo").isEqualTo(2)
                    .jsonPath("$.pageSize").isEqualTo(5)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(5);
        }

        @Test
        @DisplayName("MFT: 按 groupCode=basic 过滤返回 4 条 BASIC 配置")
        void should_filterByGroupCode() {
            webTestClient.get().uri("/api/system/configs/page?groupCode=BASIC")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(4)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(4)
                    .jsonPath("$.data[0].groupCode").isEqualTo("BASIC");
        }

        @Test
        @DisplayName("MFT: groupCode=EMAIL&pageNo=1&pageSize=2 返回 2 条 EMAIL 配置")
        void should_filterByGroupCode_withPagination() {
            webTestClient.get().uri("/api/system/configs/page?groupCode=EMAIL&pageNo=1&pageSize=2")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(4)
                    .jsonPath("$.pageNo").isEqualTo(1)
                    .jsonPath("$.pageSize").isEqualTo(2)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(2)
                    .jsonPath("$.data[0].groupCode").isEqualTo("EMAIL");
        }

        @Test
        @DisplayName("DIR: pageSize > 100 返回校验失败")
        void should_returnValidationError_whenPageSizeExceeds100() {
            webTestClient.get().uri("/api/system/configs/page?pageSize=101")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.code").isEqualTo(2001);
        }

        @Test
        @DisplayName("MFT: pageSize=0 自动使用默认值 20")
        void should_useDefaultPageSize_whenPageSizeIs0() {
            webTestClient.get().uri("/api/system/configs/page?pageSize=0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.pageSize").isEqualTo(20);
        }

        @Test
        @DisplayName("MFT: pageNo=0 自动使用默认值 1")
        void should_useDefaultPageNo_whenPageNoIs0() {
            webTestClient.get().uri("/api/system/configs/page?pageNo=0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.pageNo").isEqualTo(1);
        }
    }
}
