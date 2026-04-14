package org.smm.archetype.shared.aspect.operationlog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BusinessLog 注解")
class BusinessLogUTest {

    @Test
    @DisplayName("BusinessLog 注解应有 value 属性")
    void should_have_value_attribute() throws NoSuchMethodException {
        var method = this.getClass().getDeclaredMethod("testMethod");
        BusinessLog annotation = method.getAnnotation(BusinessLog.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("测试");
    }

    @BusinessLog("测试")
    void testMethod() {}

}
