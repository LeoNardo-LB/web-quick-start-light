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
 * 操作日志控制器集成测试 — 验证 API 端点
 */
class OperationLogControllerITest extends IntegrationTestBase {

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
    @DisplayName("GET /api/system/operation-logs")
    class FindByPage {

        @Test
        @DisplayName("MFT: 默认分页返回所有记录")
        void should_returnPaginatedData() {
            webTestClient.get().uri("/api/system/operation-logs?pageNo=1&pageSize=10")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(5)
                    .jsonPath("$.pageNo").isEqualTo(1)
                    .jsonPath("$.pageSize").isEqualTo(10)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(5);
        }

        @Test
        @DisplayName("MFT: 分页参数正确工作")
        void should_respectPaginationParams() {
            webTestClient.get().uri("/api/system/operation-logs?pageNo=1&pageSize=2")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.total").isEqualTo(5)
                    .jsonPath("$.pageNo").isEqualTo(1)
                    .jsonPath("$.pageSize").isEqualTo(2)
                    .jsonPath("$.data.length()").isEqualTo(2);
        }

        @Test
        @DisplayName("MFT: 按模块过滤")
        void should_filterByModule() {
            webTestClient.get().uri("/api/system/operation-logs?pageNo=1&pageSize=10&module=USER")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.total").isEqualTo(3)
                    .jsonPath("$.data.length()").isEqualTo(3)
                    .jsonPath("$.data[0].module").isEqualTo("USER");
        }

        @Test
        @DisplayName("MFT: 按操作类型过滤")
        void should_filterByOperationType() {
            webTestClient.get().uri("/api/system/operation-logs?pageNo=1&pageSize=10&operationType=CREATE")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.total").isEqualTo(3)
                    .jsonPath("$.data.length()").isEqualTo(3)
                    .jsonPath("$.data[0].operationType").isEqualTo("CREATE");
        }

        @Test
        @DisplayName("DIR: pageSize 超过 100 返回校验失败")
        void should_returnValidationError_whenPageSizeExceeds100() {
            webTestClient.get().uri("/api/system/operation-logs?pageNo=1&pageSize=101")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false);
        }

        @Test
        @DisplayName("MFT: 返回数据包含正确字段")
        void should_returnCorrectFields() {
            webTestClient.get().uri("/api/system/operation-logs?pageNo=1&pageSize=10")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.data[0].id").isNumber()
                    .jsonPath("$.data[0].traceId").exists()
                    .jsonPath("$.data[0].userId").exists()
                    .jsonPath("$.data[0].module").isEqualTo("SYSTEM")
                    .jsonPath("$.data[0].operationType").isEqualTo("CREATE")
                    .jsonPath("$.data[0].description").exists()
                    .jsonPath("$.data[0].method").exists()
                    .jsonPath("$.data[0].executionTime").isNumber()
                    .jsonPath("$.data[0].ip").exists()
                    .jsonPath("$.data[0].status").isEqualTo("SUCCESS")
                    .jsonPath("$.data[0].createTime").exists();
        }

        @Test
        @DisplayName("MFT: 按时间范围过滤")
        void should_filterByTimeRange() {
            webTestClient.get().uri("/api/system/operation-logs?pageNo=1&pageSize=10"
                            + "&startTime=2025-01-02T00:00:00Z&endTime=2025-01-04T23:59:59Z")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.total").isEqualTo(3)
                    .jsonPath("$.data.length()").isEqualTo(3);
        }
    }
}
