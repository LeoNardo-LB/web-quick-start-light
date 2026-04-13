package org.smm.archetype.controller.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.IntegrationTestBase;

/**
 * 系统配置分页边界值测试 — 验证分页参数边界行为
 * <p>
 * 使用 init.sql 中的 15 条预置配置数据。
 */
class SystemConfigPaginationBoundaryITest extends IntegrationTestBase {

    @Nested
    @DisplayName("GET /api/system/configs/page — 边界值测试")
    class PaginationBoundary {

        @Test
        @DisplayName("BND: pageNo=100（超出范围）返回空 data 数组，total=15")
        void should_returnEmptyData_whenPageNoExceedsRange() {
            webTestClient.get().uri("/api/system/configs/page?pageNo=100&pageSize=10")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(15)
                    .jsonPath("$.pageNo").isEqualTo(100)
                    .jsonPath("$.pageSize").isEqualTo(10)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(0);
        }

        @Test
        @DisplayName("BND: pageSize=1 返回恰好 1 条记录")
        void should_returnExactlyOneRecord_whenPageSizeIs1() {
            webTestClient.get().uri("/api/system/configs/page?pageNo=1&pageSize=1")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(15)
                    .jsonPath("$.pageNo").isEqualTo(1)
                    .jsonPath("$.pageSize").isEqualTo(1)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(1);
        }

        @Test
        @DisplayName("BND: pageSize=100（边界值）返回全部 15 条配置")
        void should_returnAllRecords_whenPageSizeIs100() {
            webTestClient.get().uri("/api/system/configs/page?pageNo=1&pageSize=100")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(15)
                    .jsonPath("$.pageNo").isEqualTo(1)
                    .jsonPath("$.pageSize").isEqualTo(100)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(15);
        }

        @Test
        @DisplayName("BND: pageNo 超出 total（pageNo=100）→ data 为空，total=15")
        void should_returnEmptyDataWithCorrectTotal_whenPageNoBeyondTotal() {
            webTestClient.get().uri("/api/system/configs/page?pageNo=100&pageSize=5")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(15)
                    .jsonPath("$.pageNo").isEqualTo(100)
                    .jsonPath("$.pageSize").isEqualTo(5)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(0);
        }

        @Test
        @DisplayName("BND: groupCode=NONEXISTENT 返回 total=0, data=[]")
        void should_returnEmptyResult_whenGroupCodeNonExistent() {
            webTestClient.get().uri("/api/system/configs/page?groupCode=NONEXISTENT")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(0)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(0);
        }
    }
}
