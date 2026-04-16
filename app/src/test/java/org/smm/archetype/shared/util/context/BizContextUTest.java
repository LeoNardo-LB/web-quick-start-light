package org.smm.archetype.shared.util.context;

import io.opentelemetry.api.baggage.Baggage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.UnitTestBase;

import java.util.EnumMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BizContext 单元测试 — 验证业务上下文 API 契约。
 * <p>
 * 覆盖能力: bizcontext-propagation (BizContext API 契约、Holder 副本机制、便捷方法、文件位置)
 */
@DisplayName("BizContext - 业务上下文管理")
class BizContextUTest extends UnitTestBase {

    @Nested
    @DisplayName("便捷方法读取")
    class ConvenienceRead {

        @Test
        @DisplayName("runWithContext(action, Key, value) 内 getUserId 返回绑定值")
        void should_readUserId_inRunWithContext() {
            BizContext.runWithContext(() -> {
                assertThat(BizContext.getUserId()).isEqualTo("user1");
            }, BizContext.Key.USER_ID, "user1");
        }

        @Test
        @DisplayName("未绑定时 getUserId 返回 null")
        void should_returnNull_whenNotBound() {
            assertThat(BizContext.getUserId()).isNull();
        }

        @Test
        @DisplayName("runWithContext 结束后 getUserId 返回 null")
        void should_return_null_after_scope() {
            BizContext.runWithContext(() -> {}, BizContext.Key.USER_ID, "user1");
            assertThat(BizContext.getUserId()).isNull();
        }
    }

    @Nested
    @DisplayName("枚举键 API")
    class EnumKeyApi {

        @Test
        @DisplayName("Key.USER_ID.get() 返回绑定值")
        void should_get_user_id_via_key() {
            BizContext.runWithContext(() -> {
                assertThat(BizContext.Key.USER_ID.get()).isEqualTo("user1");
            }, BizContext.Key.USER_ID, "user1");
        }

        @Test
        @DisplayName("Key.USER_ID.set() 修改当前作用域值")
        void should_set_user_id_via_key() {
            BizContext.runWithContext(() -> {
                BizContext.Key.USER_ID.set("new-user");
                assertThat(BizContext.getUserId()).isEqualTo("new-user");
            }, BizContext.Key.USER_ID, "user1");
        }

        @Test
        @DisplayName("Key 枚举不包含 TRACE_ID")
        void should_notContainTraceId_key() {
            assertThat(BizContext.Key.values()).containsExactly(BizContext.Key.USER_ID);
        }

        @Test
        @DisplayName("Key.baggageKey() 返回 OTel Baggage 键名")
        void should_return_baggage_key() {
            assertThat(BizContext.Key.USER_ID.getBaggageKey()).isEqualTo("userId");
        }

        @Test
        @DisplayName("Key.USER_ID.isPropagated() 返回 true")
        void should_userId_be_propagated() {
            assertThat(BizContext.Key.USER_ID.isPropagated()).isTrue();
        }

        @Test
        @DisplayName("无绑定时 Key.get() 返回 null")
        void should_return_null_without_binding() {
            assertThat(BizContext.Key.USER_ID.get()).isNull();
        }

        @Test
        @DisplayName("无绑定时 Key.set() 不抛异常")
        void should_not_throw_on_set_without_binding() {
            BizContext.Key.USER_ID.set("value"); // 应静默忽略
        }
    }

    @Nested
    @DisplayName("Baggage 自动同步")
    class BaggageSync {

        @Test
        @DisplayName("请求线程中 Key.USER_ID.set() 自动同步 OTel Baggage")
        void should_syncToBaggage_onRequestThread() {
            BizContext.runWithContext(() -> {
                // set 新值
                BizContext.Key.USER_ID.set("updated-user");
                // 验证 Baggage 同步
                assertThat(Baggage.current().getEntryValue("userId")).isEqualTo("updated-user");
            }, BizContext.Key.USER_ID, "original-user");
        }

        @Test
        @DisplayName("runWithContext 时 userId 自动写入 Baggage")
        void should_writeUserId_toBaggage_onBind() {
            BizContext.runWithContext(() -> {
                assertThat(Baggage.current().getEntryValue("userId")).isEqualTo("user1");
            }, BizContext.Key.USER_ID, "user1");
        }
    }

        @Test
        @DisplayName("请求线程中 Key.USER_ID.set(null) 同步空字符串到 Baggage")
        void should_syncEmptyStringToBaggage_onSetNull() {
            BizContext.runWithContext(() -> {
                BizContext.Key.USER_ID.set(null);
                assertThat(Baggage.current().getEntryValue("userId")).isEmpty();
            }, BizContext.Key.USER_ID, "original-user");
        }

        @Nested
        @DisplayName("EnumMap 重载")
    class EnumMapOverload {

        @Test
        @DisplayName("runWithContext(action, EnumMap) 绑定自定义 Map")
        void should_bind_custom_map() {
            EnumMap<BizContext.Key, String> ctx = new EnumMap<>(BizContext.Key.class);
            ctx.put(BizContext.Key.USER_ID, "user-enum");

            BizContext.runWithContext(() -> {
                assertThat(BizContext.getUserId()).isEqualTo("user-enum");
            }, ctx);
        }

        @Test
        @DisplayName("空 Map 绑定时 getUserId 返回 null")
        void should_return_null_for_empty_map() {
            EnumMap<BizContext.Key, String> ctx = new EnumMap<>(BizContext.Key.class);

            BizContext.runWithContext(() -> {
                assertThat(BizContext.getUserId()).isNull();
            }, ctx);
        }
    }

    @Nested
    @DisplayName("getContext / copyContext")
    class ContextExport {

        @Test
        @DisplayName("getContext() 返回引用（非 null）")
        void should_return_reference() {
            BizContext.runWithContext(() -> {
                EnumMap<BizContext.Key, String> ctx = BizContext.getContext();
                assertThat(ctx).isNotNull();
                assertThat(ctx.get(BizContext.Key.USER_ID)).isEqualTo("user1");
            }, BizContext.Key.USER_ID, "user1");
        }

        @Test
        @DisplayName("copyContext() 返回新对象（非同一引用）")
        void should_return_copy() {
            BizContext.runWithContext(() -> {
                EnumMap<BizContext.Key, String> ref = BizContext.getContext();
                EnumMap<BizContext.Key, String> copy = BizContext.copyContext();

                assertThat(copy).isNotSameAs(ref);
                assertThat(copy).containsExactlyInAnyOrderEntriesOf(ref);
            }, BizContext.Key.USER_ID, "user1");
        }

        @Test
        @DisplayName("copyContext() 返回的副本修改不影响原始")
        void should_copy_modifications_not_affect_original() {
            BizContext.runWithContext(() -> {
                EnumMap<BizContext.Key, String> ref = BizContext.getContext();
                EnumMap<BizContext.Key, String> copy = BizContext.copyContext();

                copy.put(BizContext.Key.USER_ID, "modified");

                assertThat(ref.get(BizContext.Key.USER_ID)).isEqualTo("user1");
                assertThat(copy.get(BizContext.Key.USER_ID)).isEqualTo("modified");
            }, BizContext.Key.USER_ID, "user1");
        }

        @Test
        @DisplayName("无绑定时 getContext/copyContext 返回 null")
        void should_return_null_without_binding() {
            assertThat(BizContext.getContext()).isNull();
            assertThat(BizContext.copyContext()).isNull();
        }
    }

    @Nested
    @DisplayName("copyAsReplica")
    class CopyAsReplica {

        @Test
        @DisplayName("copyAsReplica 创建深拷贝且 replica=true")
        void should_copyAsReplica_deepCopy() {
            BizContext.runWithContext(() -> {
                BizContext.Holder replica = BizContext.copyAsReplica();
                assertThat(replica).isNotNull();
                assertThat(replica.replica).isTrue();
                assertThat(replica.map.get(BizContext.Key.USER_ID)).isEqualTo("user1");
            }, BizContext.Key.USER_ID, "user1");
        }

        @Test
        @DisplayName("未绑定时 copyAsReplica 返回 null")
        void should_copyAsReplica_returnNull_whenNotBound() {
            assertThat(BizContext.copyAsReplica()).isNull();
        }
    }

    @Nested
    @DisplayName("作用域嵌套")
    class Nesting {

        @Test
        @DisplayName("嵌套 runWithContext 内层覆盖，外层恢复")
        void should_override_inner_and_restore_outer() {
            BizContext.runWithContext(() -> {
                assertThat(BizContext.getUserId()).isEqualTo("outer-user");

                BizContext.runWithContext(() -> {
                    assertThat(BizContext.getUserId()).isEqualTo("inner-user");
                }, BizContext.Key.USER_ID, "inner-user");

                assertThat(BizContext.getUserId()).isEqualTo("outer-user");
            }, BizContext.Key.USER_ID, "outer-user");
        }
    }

    @Nested
    @DisplayName("异常场景")
    class Exceptions {

        @Test
        @DisplayName("action 抛异常后上下文应恢复")
        void should_restore_context_after_exception() {
            assertThatThrownBy(() -> BizContext.runWithContext(() -> {
                throw new RuntimeException("boom");
            }, BizContext.Key.USER_ID, "user1")).isInstanceOf(RuntimeException.class);

            assertThat(BizContext.getUserId()).isNull();
        }
    }

    @Nested
    @DisplayName("空值处理")
    class NullHandling {

        @Test
        @DisplayName("userId 为 null 时 getUserId 返回 null")
        void should_handle_null_user_id() {
            BizContext.runWithContext(() -> {
                assertThat(BizContext.getUserId()).isNull();
            }, BizContext.Key.USER_ID, (String) null);
        }

        @Test
        @DisplayName("userId 为空字符串时正常存储")
        void should_handle_empty_string() {
            BizContext.runWithContext(() -> {
                assertThat(BizContext.getUserId()).isEmpty();
            }, BizContext.Key.USER_ID, "");
        }
    }
}
