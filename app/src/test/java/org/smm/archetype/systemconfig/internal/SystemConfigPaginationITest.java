package org.smm.archetype.systemconfig.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.shared.pagination.PageResult;
import org.smm.archetype.support.IntegrationTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 系统配置仓储分页集成测试 — 验证分页查询（真实 SQLite 数据库）
 * <p>
 * 注意：此测试需要新 Repository Bean 注册后才能通过（Task 7）。
 */
class SystemConfigPaginationITest extends IntegrationTestBase {

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Nested
    @DisplayName("findByPage — 无过滤条件")
    class FindByPageNoFilter {

        @Test
        @DisplayName("MFT: 无过滤条件分页查询返回所有配置，分页信息正确")
        void should_returnAllConfigs_paginated() {
            SystemConfigPageQuery query = new SystemConfigPageQuery(1, 10, null);

            PageResult<SystemConfig> result = systemConfigRepository.findByPage(query);

            assertThat(result.list()).hasSize(10);
            assertThat(result.total()).isEqualTo(15);
            assertThat(result.pageNo()).isEqualTo(1);
            assertThat(result.pageSize()).isEqualTo(10);
            assertThat(result.totalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("MFT: 第二页返回剩余 5 条配置")
        void should_returnSecondPage_withRemainingRecords() {
            SystemConfigPageQuery query = new SystemConfigPageQuery(2, 10, null);

            PageResult<SystemConfig> result = systemConfigRepository.findByPage(query);

            assertThat(result.list()).hasSize(5);
            assertThat(result.total()).isEqualTo(15);
            assertThat(result.pageNo()).isEqualTo(2);
        }

        @Test
        @DisplayName("MFT: pageSize 超过总数时返回全部记录")
        void should_returnAllRecords_whenPageSizeExceedsTotal() {
            SystemConfigPageQuery query = new SystemConfigPageQuery(1, 100, null);

            PageResult<SystemConfig> result = systemConfigRepository.findByPage(query);

            assertThat(result.list()).hasSize(15);
            assertThat(result.total()).isEqualTo(15);
            assertThat(result.totalPages()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("findByPage — 按 groupCode 过滤")
    class FindByPageWithGroupFilter {

        @Test
        @DisplayName("MFT: 按 BASIC 分组过滤，返回 4 条配置")
        void should_returnBasicConfigs_whenFilterByGroup() {
            SystemConfigPageQuery query = new SystemConfigPageQuery(1, 10, "BASIC");

            PageResult<SystemConfig> result = systemConfigRepository.findByPage(query);

            assertThat(result.list()).hasSize(4);
            assertThat(result.total()).isEqualTo(4);
            assertThat(result.list())
                    .allMatch(c -> c.getGroupCode().getCode().equals("BASIC"));
        }

        @Test
        @DisplayName("MFT: 按 EMAIL 分组过滤，返回 4 条配置")
        void should_returnEmailConfigs_whenFilterByGroup() {
            SystemConfigPageQuery query = new SystemConfigPageQuery(1, 10, "EMAIL");

            PageResult<SystemConfig> result = systemConfigRepository.findByPage(query);

            assertThat(result.list()).hasSize(4);
            assertThat(result.total()).isEqualTo(4);
            assertThat(result.list())
                    .allMatch(c -> c.getGroupCode().getCode().equals("EMAIL"));
        }

        @Test
        @DisplayName("MFT: 按 STORAGE 分组过滤，返回 3 条配置")
        void should_returnStorageConfigs_whenFilterByGroup() {
            SystemConfigPageQuery query = new SystemConfigPageQuery(1, 10, "STORAGE");

            PageResult<SystemConfig> result = systemConfigRepository.findByPage(query);

            assertThat(result.list()).hasSize(3);
            assertThat(result.total()).isEqualTo(3);
        }

        @Test
        @DisplayName("DIR: groupCode 无匹配时返回空列表，total 为 0")
        void should_returnEmpty_whenGroupCodeNotMatch() {
            SystemConfigPageQuery query = new SystemConfigPageQuery(1, 10, "NONEXISTENT");

            PageResult<SystemConfig> result = systemConfigRepository.findByPage(query);

            assertThat(result.list()).isEmpty();
            assertThat(result.total()).isEqualTo(0);
        }
    }
}
