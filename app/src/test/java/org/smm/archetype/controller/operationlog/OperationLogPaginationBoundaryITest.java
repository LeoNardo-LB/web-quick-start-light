package org.smm.archetype.controller.operationlog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.generated.entity.OperationLogDO;
import org.smm.archetype.generated.mapper.OperationLogMapper;
import org.smm.archetype.support.IntegrationTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 操作日志分页边界值测试 — 验证分页参数边界行为
 */
class OperationLogPaginationBoundaryITest extends IntegrationTestBase {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @BeforeEach
    void setUpTestData() {
        // 清空并插入测试数据
        operationLogMapper.delete(new LambdaQueryWrapper<>());

        for (int i = 1; i <= 5; i++) {
            OperationLogDO logDO = new OperationLogDO();
            logDO.setTraceId("trace-" + i);
            logDO.setUserId("user-" + i);
            logDO.setModule(i <= 3 ? "USER" : "SYSTEM");
            logDO.setOperationType(i % 2 == 1 ? "CREATE" : "UPDATE");
            logDO.setDescription("测试操作 " + i);
            logDO.setMethod("com.example.Test.method" + i);
            logDO.setParams("{}");
            logDO.setResult("success");
            logDO.setExecutionTime(100L * i);
            logDO.setIp("127.0.0.1");
            logDO.setStatus("SUCCESS");
            logDO.setCreateTime("2025-01-" + String.format("%02d", i) + "T10:00:00Z");
            operationLogMapper.insert(logDO);
        }
    }

    @Nested
    @DisplayName("GET /api/system/operation-logs — 边界值测试")
    class PaginationBoundary {

        @Test
        @DisplayName("BND: pageNo=100（超出范围）返回空 data 数组，total 仍正确")
        void should_returnEmptyData_whenPageNoExceedsRange() {
            webTestClient.get().uri("/api/system/operation-logs?pageNo=100&pageSize=10")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(5)
                    .jsonPath("$.pageNo").isEqualTo(100)
                    .jsonPath("$.pageSize").isEqualTo(10)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(0);
        }

        @Test
        @DisplayName("BND: pageSize=1 返回恰好 1 条记录")
        void should_returnExactlyOneRecord_whenPageSizeIs1() {
            webTestClient.get().uri("/api/system/operation-logs?pageNo=1&pageSize=1")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(5)
                    .jsonPath("$.pageNo").isEqualTo(1)
                    .jsonPath("$.pageSize").isEqualTo(1)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(1);
        }

        @Test
        @DisplayName("BND: pageSize=100（边界值）返回最多 5 条记录（数据不足 100）")
        void should_returnAllRecords_whenPageSizeIs100() {
            webTestClient.get().uri("/api/system/operation-logs?pageNo=1&pageSize=100")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(5)
                    .jsonPath("$.pageNo").isEqualTo(1)
                    .jsonPath("$.pageSize").isEqualTo(100)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(5);
        }

        @Test
        @DisplayName("BND: 无分页参数（缺失 pageNo/pageSize）返回校验失败")
        void should_returnValidationError_whenPaginationParamsMissing() {
            // OperationLogPageQuery 的 pageNo 和 pageSize 都是 @Min(1)，为原始类型 int
            // 缺失时会使用默认值 0，触发校验失败
            webTestClient.get().uri("/api/system/operation-logs")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false);
        }

        @Test
        @DisplayName("BND: module 过滤 + pageSize=1 返回过滤后分页结果")
        void should_returnFilteredPaginatedResult_whenModuleFilterWithPageSize1() {
            // USER 模块有 3 条记录
            webTestClient.get().uri("/api/system/operation-logs?pageNo=1&pageSize=1&module=USER")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(3)
                    .jsonPath("$.pageNo").isEqualTo(1)
                    .jsonPath("$.pageSize").isEqualTo(1)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(1)
                    .jsonPath("$.data[0].module").isEqualTo("USER");

            // SYSTEM 模块有 2 条记录
            webTestClient.get().uri("/api/system/operation-logs?pageNo=1&pageSize=1&module=SYSTEM")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(2)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(1)
                    .jsonPath("$.data[0].module").isEqualTo("SYSTEM");
        }

        @Test
        @DisplayName("BND: module 过滤 + pageNo 超出范围返回空 data")
        void should_returnEmptyData_whenFilteredPageNoExceedsRange() {
            webTestClient.get().uri("/api/system/operation-logs?pageNo=100&pageSize=10&module=USER")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(3)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(0);
        }
    }
}
