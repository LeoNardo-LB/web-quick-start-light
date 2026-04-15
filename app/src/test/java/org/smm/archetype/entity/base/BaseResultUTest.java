package org.smm.archetype.entity.base;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.exception.CommonErrorCode;
import org.smm.archetype.shared.util.context.ScopedThreadContext;
import org.smm.archetype.support.UnitTestBase;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BaseResult 基础结果")
class BaseResultUTest extends UnitTestBase {

    @Nested
    @DisplayName("success 工厂方法")
    class Success {

        @Test
        @DisplayName("success(data) 应返回 code=1000、success=true、data=data")
        void should_return_success_with_data() {
            // when
            BaseResult<String> result = BaseResult.success("hello");

            // then
            assertThat(result.getCode()).isEqualTo(1000);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo("hello");
        }

        @Test
        @DisplayName("success(null) 应返回 data=null、code=1000")
        void should_return_success_with_null_data() {
            // when
            BaseResult<Object> result = BaseResult.success(null);

            // then
            assertThat(result.getData()).isNull();
            assertThat(result.getCode()).isEqualTo(1000);
        }

        @Test
        @DisplayName("success() 的 message 应为 \"success\"")
        void should_success_have_success_message() {
            // when
            BaseResult<Void> result = BaseResult.success(null);

            // then
            assertThat(result.getMessage()).isEqualTo("success");
        }
    }

    @Nested
    @DisplayName("fail 工厂方法")
    class Fail {

        @Test
        @DisplayName("fail() 应返回 code=2000、success=false、message=\"操作失败\"")
        void should_return_fail_default() {
            // when
            BaseResult<Void> result = BaseResult.fail();

            // then
            assertThat(result.getCode()).isEqualTo(2000);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("操作失败");
        }

        @Test
        @DisplayName("fail(ILLEGAL_ARGUMENT) 应返回 code=2001")
        void should_return_fail_with_errorCode() {
            // when
            BaseResult<Void> result = BaseResult.fail(CommonErrorCode.ILLEGAL_ARGUMENT);

            // then
            assertThat(result.getCode()).isEqualTo(2001);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT.message());
        }

        @Test
        @DisplayName("fail(FAIL, \"自定义消息\") 应返回自定义 message")
        void should_return_fail_with_custom_message() {
            // when
            BaseResult<Void> result = BaseResult.fail(CommonErrorCode.FAIL, "自定义消息");

            // then
            assertThat(result.getCode()).isEqualTo(CommonErrorCode.FAIL.code());
            assertThat(result.getMessage()).isEqualTo("自定义消息");
            assertThat(result.isSuccess()).isFalse();
        }
    }

    @Nested
    @DisplayName("时间戳")
    class TimeStamp {

        @Test
        @DisplayName("success().time 应在当前 Instant 附近")
        void should_have_time_close_to_current_instant() {
            // given
            Instant before = Instant.now();

            // when
            BaseResult<String> result = BaseResult.success("data");

            Instant after = Instant.now();

            // then
            assertThat(result.getTime()).isBetween(before, after);
        }

        @Test
        @DisplayName("fail().time 应在当前 Instant 附近")
        void should_fail_have_time_close_to_current_instant() {
            // given
            Instant before = Instant.now();

            // when
            BaseResult<Void> result = BaseResult.fail();

            Instant after = Instant.now();

            // then
            assertThat(result.getTime()).isBetween(before, after);
        }
    }

    @Nested
    @DisplayName("traceId 线程上下文")
    class TraceId {

        @Test
        @DisplayName("无 ScopedThreadContext 时，traceId 应为 null")
        void should_have_null_traceId_without_context() {
            // when
            BaseResult<String> result = BaseResult.success("data");

            // then
            assertThat(result.getTraceId()).isNull();
        }

        @Test
        @DisplayName("runWithContext 内创建的 result，traceId 应匹配上下文值")
        void should_have_traceId_from_context() {
            // given
            String expectedTraceId = "trace-abc-123";
            AtomicReference<BaseResult<String>> captured = new AtomicReference<>();

            // when
            ScopedThreadContext.runWithContext(
                    () -> captured.set(BaseResult.success("data")),
                    "user-1",
                    expectedTraceId
            );

            // then
            assertThat(captured.get().getTraceId()).isEqualTo(expectedTraceId);
        }
    }
}
