package org.smm.archetype.shared.aspect.idempotent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("@Idempotent 注解")
class IdempotentAnnotationUTest {

    @Test
    @DisplayName("注解默认值：timeout=3000, timeUnit=MILLISECONDS, field=\"\", message=\"请勿重复操作\"")
    void should_have_correct_default_values() throws NoSuchMethodException {
        Method method = this.getClass().getDeclaredMethod("defaultAnnotatedMethod");
        Idempotent annotation = method.getAnnotation(Idempotent.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.timeout()).isEqualTo(3000L);
        assertThat(annotation.timeUnit()).isEqualTo(TimeUnit.MILLISECONDS);
        assertThat(annotation.field()).isEmpty();
        assertThat(annotation.message()).isEqualTo("请勿重复操作");
    }

    @Test
    @DisplayName("注解支持自定义属性值")
    void should_support_custom_values() throws NoSuchMethodException {
        Method method = this.getClass().getDeclaredMethod("customAnnotatedMethod");
        Idempotent annotation = method.getAnnotation(Idempotent.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.timeout()).isEqualTo(5000L);
        assertThat(annotation.timeUnit()).isEqualTo(TimeUnit.SECONDS);
        assertThat(annotation.field()).isEqualTo("#request.orderId");
        assertThat(annotation.message()).isEqualTo("订单已提交，请勿重复操作");
    }

    @Test
    @DisplayName("注解仅可标注在方法上")
    void should_only_target_method() {
        Target target = Idempotent.class.getAnnotation(Target.class);
        assertThat(target).isNotNull();
        assertThat(target.value()).containsExactly(ElementType.METHOD);
    }

    @Test
    @DisplayName("注解在运行时保留")
    void should_be_retained_at_runtime() {
        Retention retention = Idempotent.class.getAnnotation(Retention.class);
        assertThat(retention).isNotNull();
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Idempotent
    void defaultAnnotatedMethod() {}

    @Idempotent(timeout = 5000, timeUnit = TimeUnit.SECONDS, field = "#request.orderId", message = "订单已提交，请勿重复操作")
    void customAnnotatedMethod() {}

}
