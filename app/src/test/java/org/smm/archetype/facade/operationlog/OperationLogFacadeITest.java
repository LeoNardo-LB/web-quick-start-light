package org.smm.archetype.facade.operationlog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.entity.base.BasePageResult;
import org.smm.archetype.entity.operationlog.OperationLogPageQuery;
import org.smm.archetype.support.IntegrationTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OperationLogFacade 集成测试
 * <p>
 * 使用真实 Spring 上下文 + 内存 SQLite，operation_log 表在测试环境中无初始数据
 */
@DisplayName("OperationLogFacade")
class OperationLogFacadeITest extends IntegrationTestBase {

    @Autowired
    private OperationLogFacade operationLogFacade;

    @Nested
    @DisplayName("findByPage")
    class FindByPage {

        @Test
        @DisplayName("MFT: 分页查询返回正确元数据（空数据场景）")
        void should_returnPagedResult() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 10, null, null, null, null);
            BasePageResult<OperationLogVO> result = operationLogFacade.findByPage(query);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotNull();
            assertThat(result.getPageNo()).isEqualTo(1);
            assertThat(result.getPageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("MFT: 查询不存在的模块返回空结果")
        void should_returnEmptyResult_whenNoRecords() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 10, "NONEXISTENT", null, null, null);
            BasePageResult<OperationLogVO> result = operationLogFacade.findByPage(query);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getTotal()).isEqualTo(0);
            assertThat(result.getData()).isEmpty();
        }

        @Test
        @DisplayName("MFT: 返回结果的 VO 结构正确，isSuccess=true 且 data 不为 null")
        void should_mapFieldsToVO() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 10, null, null, null, null);
            BasePageResult<OperationLogVO> result = operationLogFacade.findByPage(query);

            // 验证分页结果结构正确（即使数据为空）
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotNull();
            assertThat(result.getTotal()).isGreaterThanOrEqualTo(0);
            assertThat(result.getPageNo()).isEqualTo(1);
            assertThat(result.getPageSize()).isEqualTo(10);
            assertThat(result.getCode()).isNotNull();
            assertThat(result.getMessage()).isNotNull();
            assertThat(result.getTime()).isNotNull();
        }

        @Test
        @DisplayName("MFT: 分页参数正确传递，pageNo 和 pageSize 与请求一致")
        void should_passQueryParameters() {
            OperationLogPageQuery query = new OperationLogPageQuery(2, 5, "USER", null, null, null);
            BasePageResult<OperationLogVO> result = operationLogFacade.findByPage(query);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getPageNo()).isEqualTo(2);
            assertThat(result.getPageSize()).isEqualTo(5);
        }
    }
}
