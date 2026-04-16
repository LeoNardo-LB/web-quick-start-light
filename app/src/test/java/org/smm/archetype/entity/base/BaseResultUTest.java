package org.smm.archetype.entity.base;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.exception.CommonErrorCode;
import org.smm.archetype.shared.util.context.BizContext;
import org.smm.archetype.support.UnitTestBase;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BaseResult 基础结果")
class BaseResultUTest extends UnitTestBase {

    @Nested
    @DisplayName("success 工厂方法")
    class Success {

        @Test
        @DisplayName("success(data) 应返回 code=1000、success=true、data=data")
        void should_return_success_with_data() {
            BaseResult<String> result = BaseResult.success("hello");

            assertThat(result.getCode()).isEqualTo(1000);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo("hello");
        }

        @Test
        @DisplayName("success(null) 应返回 data=null、code=1000")
        void should_return_success_with_null_data() {
            BaseResult<Object> result = BaseResult.success(null);

            assertThat(result.getData()).isNull();
            assertThat(result.getCode()).isEqualTo(1000);
        }

        @Test
        @DisplayName("success() 的 message 应为 \"success\"")
        void should_success_have_success_message() {
            BaseResult<Void> result = BaseResult.success(null);

            assertThat(result.getMessage()).isEqualTo("success");
        }
    }

    @Nested
    @DisplayName("fail 工厂方法")
    class Fail {

        @Test
        @DisplayName("fail() 应返回 code=2000、success=false、message=\"操作失败\"")
        void should_return_fail_default() {
            BaseResult<Void> result = BaseResult.fail();

            assertThat(result.getCode()).isEqualTo(2000);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("操作失败");
        }

        @Test
        @DisplayName("fail(ILLEGAL_ARGUMENT) 应返回 code=2001")
        void should_return_fail_with_errorCode() {
            BaseResult<Void> result = BaseResult.fail(CommonErrorCode.ILLEGAL_ARGUMENT);

            assertThat(result.getCode()).isEqualTo(2001);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT.message());
        }

        @Test
        @DisplayName("fail(FAIL, \"自定义消息\") 应返回自定义 message")
        void should_return_fail_with_custom_message() {
            BaseResult<Void> result = BaseResult.fail(CommonErrorCode.FAIL, "自定义消息");

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
            Instant before = Instant.now();

            BaseResult<String> result = BaseResult.success("data");

            Instant after = Instant.now();
            assertThat(result.getTime()).isBetween(before, after);
        }

        @Test
        @DisplayName("fail().time 应在当前 Instant 附近")
        void should_fail_have_time_close_to_current_instant() {
            Instant before = Instant.now();

            BaseResult<Void> result = BaseResult.fail();

            Instant after = Instant.now();
            assertThat(result.getTime()).isBetween(before, after);
        }
    }

    @Nested
    @DisplayName("traceId 来源")
    class TraceId {

        @Test
        @DisplayName("无 OTel Span 时，traceId 为 OTel InvalidSpanContext（全零 32 字符）")
        void should_have_invalid_traceId_without_otel_span() {
            BaseResult<String> result = BaseResult.success("data");

            // OTel 无活跃 Span 时返回 32 个零
            assertThat(result.getTraceId()).isNotNull();
            assertThat(result.getTraceId()).hasSize(32);
        }

        @Test
        @DisplayName("有 BizContext 上下文时，traceId 仍来自 OTel Span（非 BizContext）")
        void should_traceId_from_otel_span_not_biz_context() {
            BizContext.runWithContext(() -> {
                BaseResult<String> result = BaseResult.success("data");
                // traceId 来自 OTel Span，不是 BizContext
                assertThat(result.getTraceId()).isNotNull();
                assertThat(result.getTraceId()).hasSize(32);
            }, BizContext.Key.USER_ID, "user-1");
        }
    }
}
