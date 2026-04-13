package org.smm.archetype.client.log;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
