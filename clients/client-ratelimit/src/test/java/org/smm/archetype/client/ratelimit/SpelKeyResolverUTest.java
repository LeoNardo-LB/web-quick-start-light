package org.smm.archetype.client.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SpelKeyResolver")
class SpelKeyResolverUTest {

    @Nested
    @DisplayName("resolve")
    class Resolve {

        @Test
        @DisplayName("null keyExpression → 返回空字符串")
        void should_return_empty_for_null_expression() throws NoSuchMethodException {
            Method method = TestHelper.class.getMethod("testMethod", Long.class, String.class);
            String result = SpelKeyResolver.resolve(method, new Object[]{42L, "alice"}, null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("空白 keyExpression → 返回空字符串")
        void should_return_empty_for_blank_expression() throws NoSuchMethodException {
            Method method = TestHelper.class.getMethod("testMethod", Long.class, String.class);
            String result = SpelKeyResolver.resolve(method, new Object[]{42L, "alice"}, "   ");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("有效表达式 #userId → 返回参数值字符串")
        void should_resolve_valid_expression() throws NoSuchMethodException {
            Method method = TestHelper.class.getMethod("testMethod", Long.class, String.class);
            String result = SpelKeyResolver.resolve(method, new Object[]{42L, "alice"}, "#userId");
            assertThat(result).isEqualTo("42");
        }

        @Test
        @DisplayName("表达式求值为 null → 返回 \"null\" 字符串")
        void should_return_null_string_when_value_is_null() throws NoSuchMethodException {
            Method method = TestHelper.class.getMethod("testMethodWithNull", Long.class);
            String result = SpelKeyResolver.resolve(method, new Object[]{null}, "#userId");
            assertThat(result).isEqualTo("null");
        }

        @Test
        @DisplayName("无效表达式（语法错误） → 返回原始表达式")
        void should_return_original_expression_on_syntax_error() throws NoSuchMethodException {
            Method method = TestHelper.class.getMethod("testMethod", Long.class, String.class);
            String expression = "##invalid{{";
            String result = SpelKeyResolver.resolve(method, new Object[]{42L, "alice"}, expression);
            assertThat(result).isEqualTo(expression);
        }

        @Test
        @DisplayName("null args → 无参数绑定，引用变量返回 null 字符串")
        void should_return_null_string_when_args_null() throws NoSuchMethodException {
            Method method = TestHelper.class.getMethod("testMethod", Long.class, String.class);
            String result = SpelKeyResolver.resolve(method, null, "#userId");
            // 变量未绑定，SpEL 对未绑定变量返回 null → 转为 "null" 字符串
            assertThat(result).isEqualTo("null");
        }

        @Test
        @DisplayName("args 长度超过参数数量 → 仅绑定可用参数")
        void should_bind_only_available_parameters() throws NoSuchMethodException {
            Method method = TestHelper.class.getMethod("testMethod", Long.class, String.class);
            // args 有 3 个元素，方法只有 2 个参数
            String result = SpelKeyResolver.resolve(method, new Object[]{42L, "alice", "extra"}, "#name");
            assertThat(result).isEqualTo("alice");
        }

        @Test
        @DisplayName("拼接表达式 #userId + ':' + #name → 返回拼接结果")
        void should_resolve_concatenation_expression() throws NoSuchMethodException {
            Method method = TestHelper.class.getMethod("testMethod", Long.class, String.class);
            String result = SpelKeyResolver.resolve(method, new Object[]{42L, "alice"}, "#userId + ':' + #name");
            assertThat(result).isEqualTo("42:alice");
        }

        @Test
        @DisplayName("引用不存在的变量 → 返回 null 字符串")
        void should_return_null_string_for_unknown_variable() throws NoSuchMethodException {
            Method method = TestHelper.class.getMethod("testMethod", Long.class, String.class);
            String result = SpelKeyResolver.resolve(method, new Object[]{42L, "alice"}, "#nonExistent");
            // 未绑定的变量在 SpEL 中求值为 null → 转为 "null" 字符串
            assertThat(result).isEqualTo("null");
        }
    }

    // ========== 测试辅助类 ==========

    @SuppressWarnings("unused")
    static class TestHelper {

        public String testMethod(Long userId, String name) {
            return "";
        }

        public String testMethodWithNull(Long userId) {
            return "";
        }
    }
}
