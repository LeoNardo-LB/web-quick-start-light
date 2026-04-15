package org.smm.archetype.shared.aspect.operationlog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BusinessLog 注解扩展")
class BusinessLogExtensionUTest {

    @Test
    @DisplayName("BusinessLog 应有 module 属性，默认值为空字符串")
    void should_have_module_attribute_with_default_empty() throws NoSuchMethodException {
        var method = this.getClass().getDeclaredMethod("methodWithOnlyValue");
        BusinessLog annotation = method.getAnnotation(BusinessLog.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.module()).isEmpty();
    }

    @Test
    @DisplayName("BusinessLog 应有 operation 属性")
    void should_have_operation_attribute() throws NoSuchMethodException {
        var method = this.getClass().getDeclaredMethod("methodWithOperation");
        BusinessLog annotation = method.getAnnotation(BusinessLog.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.operation()).isEqualTo(OperationType.CREATE);
    }

    @Test
    @DisplayName("BusinessLog 应有 samplingRate 属性，默认值为 1.0")
    void should_have_samplingRate_attribute_with_default_1() throws NoSuchMethodException {
        var method = this.getClass().getDeclaredMethod("methodWithOnlyValue");
        BusinessLog annotation = method.getAnnotation(BusinessLog.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.samplingRate()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("旧版 @BusinessLog(\"描述\") 格式应向后兼容")
    void should_old_format_backward_compatible() throws NoSuchMethodException {
        var method = this.getClass().getDeclaredMethod("oldStyleMethod");
        BusinessLog annotation = method.getAnnotation(BusinessLog.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("旧版描述");
        assertThat(annotation.module()).isEmpty();
        assertThat(annotation.samplingRate()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("新格式应同时支持 value + module + operation + samplingRate")
    void should_new_format_support_all_attributes() throws NoSuchMethodException {
        var method = this.getClass().getDeclaredMethod("fullAttributeMethod");
        BusinessLog annotation = method.getAnnotation(BusinessLog.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("完整描述");
        assertThat(annotation.module()).isEqualTo("系统配置");
        assertThat(annotation.operation()).isEqualTo(OperationType.UPDATE);
        assertThat(annotation.samplingRate()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("注解应只能标注在方法上")
    void should_target_method_only() {
        Target target = BusinessLog.class.getAnnotation(Target.class);
        assertThat(target).isNotNull();
        assertThat(target.value()).containsExactly(ElementType.METHOD);
    }

    @Test
    @DisplayName("注解应在运行时保留")
    void should_retention_runtime() {
        Retention retention = BusinessLog.class.getAnnotation(Retention.class);
        assertThat(retention).isNotNull();
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
    }

    // 测试辅助方法

    @BusinessLog("仅value")
    void methodWithOnlyValue() {}

    @BusinessLog(value = "新增操作", operation = OperationType.CREATE)
    void methodWithOperation() {}

    @BusinessLog("旧版描述")
    void oldStyleMethod() {}

    @BusinessLog(value = "完整描述", module = "系统配置", operation = OperationType.UPDATE, samplingRate = 0.5)
    void fullAttributeMethod() {}

}
