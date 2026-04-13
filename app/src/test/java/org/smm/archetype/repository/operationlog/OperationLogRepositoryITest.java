package org.smm.archetype.repository.operationlog;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.entity.operationlog.OperationLog;
import org.smm.archetype.entity.operationlog.OperationLogPageQuery;
import org.smm.archetype.generated.entity.OperationLogDO;
import org.smm.archetype.generated.mapper.OperationLogMapper;
import org.smm.archetype.support.IntegrationTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 操作日志仓储集成测试 — 验证分页查询（真实 SQLite 数据库）
 */
class OperationLogRepositoryITest extends IntegrationTestBase {

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Autowired
    private OperationLogMapper operationLogMapper;

    @BeforeEach
    void setUpTestData() {
        // 清空并插入测试数据
        operationLogMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>());

        // 插入 5 条测试数据
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
    @DisplayName("findByPage — 无过滤条件")
    class FindByPageNoFilter {

        @Test
        @DisplayName("MFT: 无过滤条件返回所有记录，分页正确")
        void should_returnAllRecords_paginated() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 3, null, null, null, null);

            IPage<OperationLog> result = operationLogRepository.findByPage(query);

            assertThat(result.getTotal()).isEqualTo(5);
            assertThat(result.getRecords()).hasSize(3);
            assertThat(result.getCurrent()).isEqualTo(1);
            assertThat(result.getSize()).isEqualTo(3);
        }

        @Test
        @DisplayName("MFT: 第二页返回剩余记录")
        void should_returnSecondPage() {
            OperationLogPageQuery query = new OperationLogPageQuery(2, 3, null, null, null, null);

            IPage<OperationLog> result = operationLogRepository.findByPage(query);

            assertThat(result.getTotal()).isEqualTo(5);
            assertThat(result.getRecords()).hasSize(2);
            assertThat(result.getCurrent()).isEqualTo(2);
        }

        @Test
        @DisplayName("DIR: pageSize 超过总记录数时返回所有记录")
        void should_returnAllRecords_whenPageSizeExceedsTotal() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 100, null, null, null, null);

            IPage<OperationLog> result = operationLogRepository.findByPage(query);

            assertThat(result.getTotal()).isEqualTo(5);
            assertThat(result.getRecords()).hasSize(5);
        }
    }

    @Nested
    @DisplayName("findByPage — 按模块过滤")
    class FindByPageModuleFilter {

        @Test
        @DisplayName("MFT: 按 USER 模块过滤返回 3 条记录")
        void should_filterByUserModule() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 10, "USER", null, null, null);

            IPage<OperationLog> result = operationLogRepository.findByPage(query);

            assertThat(result.getTotal()).isEqualTo(3);
            assertThat(result.getRecords()).allMatch(log -> "USER".equals(log.getModule()));
        }

        @Test
        @DisplayName("MFT: 按 SYSTEM 模块过滤返回 2 条记录")
        void should_filterBySystemModule() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 10, "SYSTEM", null, null, null);

            IPage<OperationLog> result = operationLogRepository.findByPage(query);

            assertThat(result.getTotal()).isEqualTo(2);
            assertThat(result.getRecords()).allMatch(log -> "SYSTEM".equals(log.getModule()));
        }

        @Test
        @DisplayName("DIR: 不存在的模块返回空列表")
        void should_returnEmpty_whenModuleNotFound() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 10, "NONEXISTENT", null, null, null);

            IPage<OperationLog> result = operationLogRepository.findByPage(query);

            assertThat(result.getTotal()).isEqualTo(0);
            assertThat(result.getRecords()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByPage — 按操作类型过滤")
    class FindByPageOperationTypeFilter {

        @Test
        @DisplayName("MFT: 按 CREATE 类型过滤返回奇数 ID 记录")
        void should_filterByCreateType() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 10, null, "CREATE", null, null);

            IPage<OperationLog> result = operationLogRepository.findByPage(query);

            assertThat(result.getTotal()).isEqualTo(3);
            assertThat(result.getRecords()).allMatch(log -> "CREATE".equals(log.getOperationType()));
        }

        @Test
        @DisplayName("MFT: 按 UPDATE 类型过滤返回偶数 ID 记录")
        void should_filterByUpdateType() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 10, null, "UPDATE", null, null);

            IPage<OperationLog> result = operationLogRepository.findByPage(query);

            assertThat(result.getTotal()).isEqualTo(2);
            assertThat(result.getRecords()).allMatch(log -> "UPDATE".equals(log.getOperationType()));
        }
    }

    @Nested
    @DisplayName("findByPage — 按时间范围过滤")
    class FindByPageTimeRangeFilter {

        @Test
        @DisplayName("MFT: 时间范围内返回匹配记录")
        void should_filterByTimeRange() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 10, null, null,
                    "2025-01-02T00:00:00Z", "2025-01-04T23:59:59Z");

            IPage<OperationLog> result = operationLogRepository.findByPage(query);

            assertThat(result.getTotal()).isEqualTo(3); // 01-02, 01-03, 01-04
        }

        @Test
        @DisplayName("DIR: 超出范围返回空列表")
        void should_returnEmpty_whenTimeRangeOutOfRange() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 10, null, null,
                    "2030-01-01T00:00:00Z", "2030-12-31T23:59:59Z");

            IPage<OperationLog> result = operationLogRepository.findByPage(query);

            assertThat(result.getTotal()).isEqualTo(0);
            assertThat(result.getRecords()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByPage — 组合过滤")
    class FindByPageCombinedFilter {

        @Test
        @DisplayName("MFT: 模块 + 操作类型组合过滤")
        void should_filterByModuleAndType() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 10, "USER", "CREATE", null, null);

            IPage<OperationLog> result = operationLogRepository.findByPage(query);

            // USER 模块有 3 条 (id 1,2,3)，其中 CREATE 类型有 id=1,3 → 2 条
            assertThat(result.getTotal()).isEqualTo(2);
            assertThat(result.getRecords()).allMatch(log ->
                    "USER".equals(log.getModule()) && "CREATE".equals(log.getOperationType()));
        }
    }

    @Nested
    @DisplayName("findByPage — 分页参数验证")
    class FindByPagePaginationParams {

        @Test
        @DisplayName("MFT: pageNo=1,pageSize=1 返回 1 条记录，total=5")
        void should_returnOneRecord_perPage() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 1, null, null, null, null);

            IPage<OperationLog> result = operationLogRepository.findByPage(query);

            assertThat(result.getTotal()).isEqualTo(5);
            assertThat(result.getRecords()).hasSize(1);
        }
    }
}
