package org.smm.archetype.entity.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.UnitTestBase;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BaseRequest 基础请求")
class BaseRequestUTest extends UnitTestBase {

    @Nested
    @DisplayName("getter/setter")
    class GetterSetter {

        @Test
        @DisplayName("requestId 应正确读写")
        void should_read_write_requestId() {
            // given
            BaseRequest request = new BaseRequest();

            // when
            request.setRequestId("req-001");

            // then
            assertThat(request.getRequestId()).isEqualTo("req-001");
        }

        @Test
        @DisplayName("traceId 应正确读写")
        void should_read_write_traceId() {
            // given
            BaseRequest request = new BaseRequest();

            // when
            request.setTraceId("trace-001");

            // then
            assertThat(request.getTraceId()).isEqualTo("trace-001");
        }

        @Test
        @DisplayName("默认值应为 null")
        void should_have_null_defaults() {
            // given
            BaseRequest request = new BaseRequest();

            // then
            assertThat(request.getRequestId()).isNull();
            assertThat(request.getTraceId()).isNull();
        }
    }
}
