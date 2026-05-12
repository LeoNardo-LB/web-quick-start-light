package org.smm.archetype.systemconfig.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.shared.result.BasePageResult;
import org.smm.archetype.support.IntegrationTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SystemConfigFacade 分页查询集成测试
 * <p>
 * 注意：此测试需要新 Facade Bean 注册后才能通过（Task 7）。
 */
@DisplayName("SystemConfigFacade — findByPage")
class SystemConfigFacadePaginationITest extends IntegrationTestBase {

    @Autowired
    private org.smm.archetype.systemconfig.SystemConfigFacade systemConfigFacade;

    @Nested
    @DisplayName("findByPage")
    class FindByPage {

        @Test
        @DisplayName("MFT: 分页查询返回正确结果")
        void should_returnPagedResult() {
            SystemConfigPageQuery query = new SystemConfigPageQuery(1, 10, null);
            BasePageResult<SystemConfigVO> result = systemConfigFacade.findByPage(query);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getTotal()).isGreaterThanOrEqualTo(15);
            assertThat(result.getPageNo()).isEqualTo(1);
            assertThat(result.getPageSize()).isEqualTo(10);
            assertThat(result.getData()).hasSize(10);
        }

        @Test
        @DisplayName("MFT: 按 groupCode 过滤返回对应分组的配置")
        void should_filterByGroupCode() {
            SystemConfigPageQuery query = new SystemConfigPageQuery(1, 10, "EMAIL");
            BasePageResult<SystemConfigVO> result = systemConfigFacade.findByPage(query);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotEmpty();
            assertThat(result.getData()).allMatch(c -> "EMAIL".equals(c.groupCode()));
        }

        @Test
        @DisplayName("MFT: 不存在的分组返回空结果")
        void should_returnEmptyResult_forNonexistentGroup() {
            SystemConfigPageQuery query = new SystemConfigPageQuery(1, 10, "NONEXISTENT");
            BasePageResult<SystemConfigVO> result = systemConfigFacade.findByPage(query);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getTotal()).isEqualTo(0);
            assertThat(result.getData()).isEmpty();
        }

        @Test
        @DisplayName("MFT: 第二页分页信息正确")
        void should_returnSecondPage() {
            SystemConfigPageQuery query = new SystemConfigPageQuery(2, 10, null);
            BasePageResult<SystemConfigVO> result = systemConfigFacade.findByPage(query);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getPageNo()).isEqualTo(2);
            assertThat(result.getTotal()).isGreaterThanOrEqualTo(15);
            assertThat(result.getData()).isNotEmpty();
        }
    }
}
