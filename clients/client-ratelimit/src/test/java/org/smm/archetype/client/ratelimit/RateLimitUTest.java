package org.smm.archetype.client.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("@RateLimit 注解")
class RateLimitUTest {

    // ========== 默认值测试 ==========

    @Test
    @DisplayName("注解应有 METHOD 级别的 Target")
    void should_target_method() {
        Target target = RateLimit.class.getAnnotation(Target.class);
        assertThat(target).isNotNull();
        assertThat(target.value()).containsExactly(ElementType.METHOD);
    }

    @Test
    @DisplayName("注解应有 RUNTIME Retention")
    void should_have_runtime_retention() {
        Retention retention = RateLimit.class.getAnnotation(Retention.class);
        assertThat(retention).isNotNull();
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    @DisplayName("默认 capacity 应为 10")
    void should_have_default_capacity_10() throws NoSuchMethodException {
        var method = getClass().getDeclaredMethod("defaultAnnotatedMethod");
        RateLimit annotation = method.getAnnotation(RateLimit.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.capacity()).isEqualTo(10);
    }

    @Test
    @DisplayName("默认 refillTokens 应为 10")
    void should_have_default_refillTokens_10() throws NoSuchMethodException {
        var method = getClass().getDeclaredMethod("defaultAnnotatedMethod");
        RateLimit annotation = method.getAnnotation(RateLimit.class);
        assertThat(annotation.refillTokens()).isEqualTo(10);
    }

    @Test
    @DisplayName("默认 refillDuration 应为 1")
    void should_have_default_refillDuration_1() throws NoSuchMethodException {
        var method = getClass().getDeclaredMethod("defaultAnnotatedMethod");
        RateLimit annotation = method.getAnnotation(RateLimit.class);
        assertThat(annotation.refillDuration()).isEqualTo(1);
    }

    @Test
    @DisplayName("默认 refillUnit 应为 SECONDS")
    void should_have_default_refillUnit_seconds() throws NoSuchMethodException {
        var method = getClass().getDeclaredMethod("defaultAnnotatedMethod");
        RateLimit annotation = method.getAnnotation(RateLimit.class);
        assertThat(annotation.refillUnit()).isEqualTo(TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("默认 key 应为空字符串")
    void should_have_default_key_empty() throws NoSuchMethodException {
        var method = getClass().getDeclaredMethod("defaultAnnotatedMethod");
        RateLimit annotation = method.getAnnotation(RateLimit.class);
        assertThat(annotation.key()).isEmpty();
    }

    @Test
    @DisplayName("默认 fallback 应为 REJECT")
    void should_have_default_fallback_reject() throws NoSuchMethodException {
        var method = getClass().getDeclaredMethod("defaultAnnotatedMethod");
        RateLimit annotation = method.getAnnotation(RateLimit.class);
        assertThat(annotation.fallback()).isEqualTo(LimitFallback.REJECT);
    }

    // ========== 自定义属性值测试 ==========

    @Test
    @DisplayName("自定义 capacity 应生效")
    void should_accept_custom_capacity() throws NoSuchMethodException {
        var method = getClass().getDeclaredMethod("customCapacityMethod");
        RateLimit annotation = method.getAnnotation(RateLimit.class);
        assertThat(annotation.capacity()).isEqualTo(100);
    }

    @Test
    @DisplayName("自定义 refillTokens 应生效")
    void should_accept_custom_refillTokens() throws NoSuchMethodException {
        var method = getClass().getDeclaredMethod("customRefillMethod");
        RateLimit annotation = method.getAnnotation(RateLimit.class);
        assertThat(annotation.refillTokens()).isEqualTo(5);
    }

    @Test
    @DisplayName("自定义 refillDuration 应生效")
    void should_accept_custom_refillDuration() throws NoSuchMethodException {
        var method = getClass().getDeclaredMethod("customRefillMethod");
        RateLimit annotation = method.getAnnotation(RateLimit.class);
        assertThat(annotation.refillDuration()).isEqualTo(60);
    }

    @Test
    @DisplayName("自定义 refillUnit 应生效")
    void should_accept_custom_refillUnit() throws NoSuchMethodException {
        var method = getClass().getDeclaredMethod("customRefillMethod");
        RateLimit annotation = method.getAnnotation(RateLimit.class);
        assertThat(annotation.refillUnit()).isEqualTo(TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("自定义 key 应生效")
    void should_accept_custom_key() throws NoSuchMethodException {
        var method = getClass().getDeclaredMethod("customKeyMethod");
        RateLimit annotation = method.getAnnotation(RateLimit.class);
        assertThat(annotation.key()).isEqualTo("#userId");
    }

    @Test
    @DisplayName("自定义 fallback 应生效")
    void should_accept_custom_fallback() throws NoSuchMethodException {
        var method = getClass().getDeclaredMethod("customFallbackMethod");
        RateLimit annotation = method.getAnnotation(RateLimit.class);
        assertThat(annotation.fallback()).isEqualTo(LimitFallback.WAIT);
    }

    // ========== LimitFallback 枚举测试 ==========

    @Test
    @DisplayName("LimitFallback 应包含 REJECT/WAIT/FALLBACK 三个值")
    void should_have_three_fallback_values() {
        assertThat(LimitFallback.values())
                .containsExactly(LimitFallback.REJECT, LimitFallback.WAIT, LimitFallback.FALLBACK);
    }

    // ========== 被注解的方法（测试用） ==========

    @RateLimit
    void defaultAnnotatedMethod() {}

    @RateLimit(capacity = 100)
    void customCapacityMethod() {}

    @RateLimit(refillTokens = 5, refillDuration = 60, refillUnit = TimeUnit.MINUTES)
    void customRefillMethod() {}

    @RateLimit(key = "#userId")
    void customKeyMethod() {}

    @RateLimit(fallback = LimitFallback.WAIT)
    void customFallbackMethod() {}
}
