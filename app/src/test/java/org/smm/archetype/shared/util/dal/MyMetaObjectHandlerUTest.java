package org.smm.archetype.shared.util.dal;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smm.archetype.shared.util.context.ScopedThreadContext;
import org.smm.archetype.support.UnitTestBase;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

/**
 * {@link MyMetaObjectHandler} 单元测试
 * <p>
 * 通过 Spy 拦截 strictInsertFill/strictUpdateFill 调用，
 * 避免深入 MyBatis-Plus 内部 MetaObject/TableInfo 依赖。
 *
 * @author Leonardo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MyBatis-Plus 元数据填充处理器 - MyMetaObjectHandler")
class MyMetaObjectHandlerUTest extends UnitTestBase {

    @Mock
    private MetaObject metaObject;

    @Spy
    private MyMetaObjectHandler handler;

    @Captor
    private ArgumentCaptor<Instant> instantCaptor;

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    @BeforeEach
    void setUp() {
        // 拦截 strictInsertFill/strictUpdateFill，阻止其内部访问 MetaObject/TableInfo
        // 使用 any(Class.class) 消除 (Class, value) 与 (Supplier, Class) 重载的歧义
        // strictInsertFill/strictUpdateFill 返回 MetaObjectHandler（非 void），需用 doReturn
        // 使用 lenient 避免某些测试未调用其中某个方法时报 UnnecessaryStubbingException
        lenient().doReturn(handler).when(handler).strictInsertFill(any(), anyString(), any(Class.class), any());
        lenient().doReturn(handler).when(handler).strictUpdateFill(any(), anyString(), any(Class.class), any());
    }

    // ================================================================
    // insertFill
    // ================================================================

    @Nested
    @DisplayName("insertFill - 插入时自动填充")
    class InsertFill {

        @Test
        @DisplayName("正常 userId：4 个字段正确填充（createTime, updateTime, createUser, updateUser）")
        void should_fill_all_four_fields_with_userId() {
            // arrange
            String userId = "user-001";
            Instant before = Instant.now();

            // act
            ScopedThreadContext.runWithContext(() -> handler.insertFill(metaObject), userId, "trace-001");

            Instant after = Instant.now();

            // assert - strictInsertFill 被调用 4 次
            verify(handler).strictInsertFill(eq(metaObject), eq("createTime"), eq(Instant.class), instantCaptor.capture());
            verify(handler).strictInsertFill(eq(metaObject), eq("updateTime"), eq(Instant.class), instantCaptor.capture());
            verify(handler).strictInsertFill(eq(metaObject), eq("createUser"), eq(String.class), stringCaptor.capture());
            verify(handler).strictInsertFill(eq(metaObject), eq("updateUser"), eq(String.class), stringCaptor.capture());

            // 验证 Instant 字段值在合理时间范围内
            assertThat(instantCaptor.getAllValues())
                    .hasSize(2)
                    .allSatisfy(val -> {
                        assertThat(val).isBetween(before.minus(1, ChronoUnit.SECONDS), after.plus(1, ChronoUnit.SECONDS));
                        assertThat(val).isInstanceOf(Instant.class);
                    });

            // 验证用户字段值
            assertThat(stringCaptor.getAllValues())
                    .hasSize(2)
                    .allSatisfy(val -> assertThat(val).isEqualTo(userId));
        }

        @Test
        @DisplayName("userId 为 null：createUser/updateUser 回退为 system")
        void should_use_system_when_userId_is_null() {
            // arrange — ScopedValue 未绑定，getUserId() 返回 null

            // act
            handler.insertFill(metaObject);

            // assert
            verify(handler).strictInsertFill(eq(metaObject), eq("createTime"), eq(Instant.class), any(Instant.class));
            verify(handler).strictInsertFill(eq(metaObject), eq("updateTime"), eq(Instant.class), any(Instant.class));
            verify(handler).strictInsertFill(eq(metaObject), eq("createUser"), eq(String.class), stringCaptor.capture());
            verify(handler).strictInsertFill(eq(metaObject), eq("updateUser"), eq(String.class), stringCaptor.capture());

            assertThat(stringCaptor.getAllValues())
                    .hasSize(2)
                    .allSatisfy(val -> assertThat(val).isEqualTo("system"));
        }

        @Test
        @DisplayName("strictInsertFill 调用时传入正确的字段名和类型")
        void should_call_strictInsertFill_with_correct_field_names() {
            // arrange
            String userId = "user-003";

            // act
            ScopedThreadContext.runWithContext(() -> handler.insertFill(metaObject), userId, "trace-003");

            // assert - 验证调用了 4 次 strictInsertFill
            verify(handler).strictInsertFill(eq(metaObject), eq("createTime"), eq(Instant.class), any());
            verify(handler).strictInsertFill(eq(metaObject), eq("updateTime"), eq(Instant.class), any());
            verify(handler).strictInsertFill(eq(metaObject), eq("createUser"), eq(String.class), eq(userId));
            verify(handler).strictInsertFill(eq(metaObject), eq("updateUser"), eq(String.class), eq(userId));
        }
    }

    // ================================================================
    // updateFill
    // ================================================================

    @Nested
    @DisplayName("updateFill - 更新时自动填充")
    class UpdateFill {

        @Test
        @DisplayName("正常 userId：2 个字段正确填充（updateTime, updateUser）")
        void should_fill_two_fields_with_userId() {
            // arrange
            String userId = "user-002";
            Instant before = Instant.now();

            // act
            ScopedThreadContext.runWithContext(() -> handler.updateFill(metaObject), userId, "trace-002");

            Instant after = Instant.now();

            // assert
            verify(handler).strictUpdateFill(eq(metaObject), eq("updateTime"), eq(Instant.class), instantCaptor.capture());
            verify(handler).strictUpdateFill(eq(metaObject), eq("updateUser"), eq(String.class), stringCaptor.capture());

            assertThat(instantCaptor.getValue())
                    .isBetween(before.minus(1, ChronoUnit.SECONDS), after.plus(1, ChronoUnit.SECONDS));
            assertThat(stringCaptor.getValue()).isEqualTo(userId);
        }

        @Test
        @DisplayName("userId 为 null：updateUser 回退为 system")
        void should_use_system_when_userId_is_null() {
            // arrange — ScopedValue 未绑定

            // act
            handler.updateFill(metaObject);

            // assert
            verify(handler).strictUpdateFill(eq(metaObject), eq("updateTime"), eq(Instant.class), any(Instant.class));
            verify(handler).strictUpdateFill(eq(metaObject), eq("updateUser"), eq(String.class), stringCaptor.capture());

            assertThat(stringCaptor.getValue()).isEqualTo("system");
        }

        @Test
        @DisplayName("strictUpdateFill 调用时传入正确的字段名和类型")
        void should_call_strictUpdateFill_with_correct_field_names() {
            // arrange
            String userId = "user-004";

            // act
            ScopedThreadContext.runWithContext(() -> handler.updateFill(metaObject), userId, "trace-004");

            // assert
            verify(handler).strictUpdateFill(eq(metaObject), eq("updateTime"), eq(Instant.class), any());
            verify(handler).strictUpdateFill(eq(metaObject), eq("updateUser"), eq(String.class), eq(userId));
        }
    }
}
