package org.smm.archetype.util.context;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.UnitTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ScopedThreadContext - 线程上下文管理")
class ScopedThreadContextUTest extends UnitTestBase {

    @Nested
    @DisplayName("上下文绑定后获取值")
    class ContextBound {

        @Test
        @DisplayName("绑定上下文后应能获取 userId")
        void should_get_userId_in_context() {
            ScopedThreadContext.runWithContext(() -> {
                assertThat(ScopedThreadContext.getUserId()).isEqualTo("user1");
            }, "user1", "trace1");
        }

        @Test
        @DisplayName("绑定上下文后应能获取 traceId")
        void should_get_traceId_in_context() {
            ScopedThreadContext.runWithContext(() -> {
                assertThat(ScopedThreadContext.getTraceId()).isEqualTo("trace1");
            }, "user1", "trace1");
        }

        @Test
        @DisplayName("绑定上下文后 getContext 应返回包含正确 userId 和 traceId 的 Context 对象")
        void should_get_context_object() {
            ScopedThreadContext.runWithContext(() -> {
                ScopedThreadContext.Context ctx = ScopedThreadContext.getContext();
                assertThat(ctx).isNotNull();
                assertThat(ctx.userId()).isEqualTo("user1");
                assertThat(ctx.traceId()).isEqualTo("trace1");
            }, "user1", "trace1");
        }
    }

    @Nested
    @DisplayName("未绑定上下文时")
    class NoContext {

        @Test
        @DisplayName("未绑定上下文时 getUserId 应返回 null")
        void should_return_null_userId_without_context() {
            assertThat(ScopedThreadContext.getUserId()).isNull();
        }

        @Test
        @DisplayName("未绑定上下文时 getTraceId 应返回 null")
        void should_return_null_traceId_without_context() {
            assertThat(ScopedThreadContext.getTraceId()).isNull();
        }

        @Test
        @DisplayName("未绑定上下文时 getContext 应返回 null")
        void should_return_null_context_without_context() {
            assertThat(ScopedThreadContext.getContext()).isNull();
        }
    }

    @Nested
    @DisplayName("上下文生命周期")
    class Lifecycle {

        @Test
        @DisplayName("runWithContext 执行完毕后上下文应被自动清理")
        void should_clean_up_after_run() {
            ScopedThreadContext.runWithContext(() -> {
                assertThat(ScopedThreadContext.getUserId()).isEqualTo("user1");
            }, "user1", "trace1");

            assertThat(ScopedThreadContext.getUserId()).isNull();
            assertThat(ScopedThreadContext.getTraceId()).isNull();
            assertThat(ScopedThreadContext.getContext()).isNull();
        }

        @Test
        @DisplayName("runWithContext 中抛出异常时上下文仍应被清理，异常应向上传播")
        void should_propagate_exception_and_clean_up() {
            RuntimeException expected = new RuntimeException("测试异常");

            assertThatThrownBy(() -> ScopedThreadContext.runWithContext(() -> {
                throw expected;
            }, "user1", "trace1"))
                    .isSameAs(expected);

            assertThat(ScopedThreadContext.getUserId()).isNull();
            assertThat(ScopedThreadContext.getTraceId()).isNull();
        }
    }

    @Nested
    @DisplayName("边界值处理")
    class EdgeCases {

        @Test
        @DisplayName("userId 为 null 时 getUserId 应返回 null")
        void should_handle_null_userId() {
            ScopedThreadContext.runWithContext(() -> {
                assertThat(ScopedThreadContext.getUserId()).isNull();
                assertThat(ScopedThreadContext.getTraceId()).isEqualTo("trace");
            }, null, "trace");
        }

        @Test
        @DisplayName("traceId 为 null 时 getTraceId 应返回 null")
        void should_handle_null_traceId() {
            ScopedThreadContext.runWithContext(() -> {
                assertThat(ScopedThreadContext.getUserId()).isEqualTo("user");
                assertThat(ScopedThreadContext.getTraceId()).isNull();
            }, "user", null);
        }

        @Test
        @DisplayName("userId 和 traceId 均为空字符串时应正常返回空字符串")
        void should_handle_empty_strings() {
            ScopedThreadContext.runWithContext(() -> {
                assertThat(ScopedThreadContext.getUserId()).isEmpty();
                assertThat(ScopedThreadContext.getTraceId()).isEmpty();

                ScopedThreadContext.Context ctx = ScopedThreadContext.getContext();
                assertThat(ctx.userId()).isEmpty();
                assertThat(ctx.traceId()).isEmpty();
            }, "", "");
        }
    }

    @Nested
    @DisplayName("嵌套上下文")
    class NestedContext {

        @Test
        @DisplayName("嵌套 runWithContext 时内层上下文应覆盖外层，退出内层后恢复外层")
        void should_support_nested_contexts() {
            ScopedThreadContext.runWithContext(() -> {
                assertThat(ScopedThreadContext.getUserId()).isEqualTo("outer-user");
                assertThat(ScopedThreadContext.getTraceId()).isEqualTo("outer-trace");

                ScopedThreadContext.runWithContext(() -> {
                    assertThat(ScopedThreadContext.getUserId()).isEqualTo("inner-user");
                    assertThat(ScopedThreadContext.getTraceId()).isEqualTo("inner-trace");
                }, "inner-user", "inner-trace");

                // 内层退出后应恢复外层上下文
                assertThat(ScopedThreadContext.getUserId()).isEqualTo("outer-user");
                assertThat(ScopedThreadContext.getTraceId()).isEqualTo("outer-trace");
            }, "outer-user", "outer-trace");
        }
    }
}
