package org.smm.archetype.shared.aspect.operationlog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OperationType 枚举")
class OperationTypeUTest {

    @Test
    @DisplayName("应包含所有6种操作类型")
    void should_contain_all_six_operation_types() {
        OperationType[] values = OperationType.values();
        assertThat(values).hasSize(6);
        assertThat(values).containsExactly(
                OperationType.CREATE,
                OperationType.UPDATE,
                OperationType.DELETE,
                OperationType.QUERY,
                OperationType.EXPORT,
                OperationType.IMPORT
        );
    }

    @Test
    @DisplayName("每个枚举值应有 code 属性")
    void should_have_code_for_each_type() {
        for (OperationType type : OperationType.values()) {
            assertThat(type.code()).isNotNull();
            assertThat(type.code()).isNotEmpty();
        }
    }

    @Test
    @DisplayName("每个枚举值应有 description 属性")
    void should_have_description_for_each_type() {
        for (OperationType type : OperationType.values()) {
            assertThat(type.description()).isNotNull();
            assertThat(type.description()).isNotEmpty();
        }
    }

    @Test
    @DisplayName("CREATE 的 code 和 description 应正确")
    void should_create_have_correct_code_and_description() {
        assertThat(OperationType.CREATE.code()).isEqualTo("CREATE");
        assertThat(OperationType.CREATE.description()).isEqualTo("新增");
    }

    @Test
    @DisplayName("UPDATE 的 code 和 description 应正确")
    void should_update_have_correct_code_and_description() {
        assertThat(OperationType.UPDATE.code()).isEqualTo("UPDATE");
        assertThat(OperationType.UPDATE.description()).isEqualTo("修改");
    }

    @Test
    @DisplayName("DELETE 的 code 和 description 应正确")
    void should_delete_have_correct_code_and_description() {
        assertThat(OperationType.DELETE.code()).isEqualTo("DELETE");
        assertThat(OperationType.DELETE.description()).isEqualTo("删除");
    }

    @Test
    @DisplayName("QUERY 的 code 和 description 应正确")
    void should_query_have_correct_code_and_description() {
        assertThat(OperationType.QUERY.code()).isEqualTo("QUERY");
        assertThat(OperationType.QUERY.description()).isEqualTo("查询");
    }

    @Test
    @DisplayName("EXPORT 的 code 和 description 应正确")
    void should_export_have_correct_code_and_description() {
        assertThat(OperationType.EXPORT.code()).isEqualTo("EXPORT");
        assertThat(OperationType.EXPORT.description()).isEqualTo("导出");
    }

    @Test
    @DisplayName("IMPORT 的 code 和 description 应正确")
    void should_import_have_correct_code_and_description() {
        assertThat(OperationType.IMPORT.code()).isEqualTo("IMPORT");
        assertThat(OperationType.IMPORT.description()).isEqualTo("导入");
    }

    @Test
    @DisplayName("code 值应与枚举名称一致")
    void should_code_match_enum_name() {
        for (OperationType type : OperationType.values()) {
            assertThat(type.code()).isEqualTo(type.name());
        }
    }

}
