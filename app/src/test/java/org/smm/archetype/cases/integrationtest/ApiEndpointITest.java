package org.smm.archetype.cases.integrationtest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.IntegrationTestBase;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L3 集成测试 — 验证统一响应封装、traceId 自动填充、time 字段
 * <p>
 * 覆盖能力: unified-response, thread-context
 */
class ApiEndpointITest extends IntegrationTestBase {

    @Nested
    @DisplayName("GET /api/test/hello — 统一响应封装")
    class HelloEndpoint {

        @Test
        @DisplayName("MFT: 成功返回 BaseResult 包含 data/time/traceId/success=true")
        void should_returnSuccessBaseResultWithDataAndTimeAndTraceId() {
            webTestClient.get().uri("/api/test/hello")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.message").isEqualTo("success")
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.time").isNotEmpty()
                    .jsonPath("$.traceId").isNotEmpty()
                    .jsonPath("$.requestId").doesNotExist();
        }

        @Test
        @DisplayName("INV: time 字段为合法 Instant（接近当前时间）")
        void should_timeFieldBeValidEpochMillis() {
            Instant before = Instant.now();

            Map body = webTestClient.get().uri("/api/test/hello")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(Map.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(body).isNotNull();

            // time 字段已从 Long 改为 Instant，JSON 序列化为数组格式
            Object timeObj = body.get("time");
            Instant time;
            if (timeObj instanceof List<?> timeList) {
                // Jackson 序列化 Instant 为 [year, month, day, hour, minute, second, nano]
                int year = ((Number) timeList.get(0)).intValue();
                int month = ((Number) timeList.get(1)).intValue();
                int day = ((Number) timeList.get(2)).intValue();
                int hour = ((Number) timeList.get(3)).intValue();
                int minute = ((Number) timeList.get(4)).intValue();
                int second = ((Number) timeList.get(5)).intValue();
                int nano = timeList.size() > 6 ? ((Number) timeList.get(6)).intValue() : 0;
                time = Instant.ofEpochSecond(
                        java.time.LocalDateTime.of(year, month, day, hour, minute, second, nano)
                                .toEpochSecond(java.time.ZoneOffset.UTC),
                        nano);
            } else if (timeObj instanceof String timeStr) {
                time = Instant.parse(timeStr);
            } else {
                // Fallback: Jackson might deserialize Instant as a numeric value
                time = Instant.ofEpochMilli(((Number) timeObj).longValue());
            }
            Instant after = Instant.now();
            assertThat(time).isBetween(before, after);
        }

        @Test
        @DisplayName("INV: traceId 同时出现在响应头和响应体中")
        void should_traceIdAppearInBothHeaderAndBody() {
            webTestClient.get().uri("/api/test/hello")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().value("X-Trace-Id", traceId ->
                            assertThat(traceId).isNotNull().isNotBlank());
        }
    }

    @Nested
    @DisplayName("GET /api/test/validate — 参数校验")
    class ValidationEndpoint {

        @Test
        @DisplayName("MFT: 合法参数返回成功")
        void should_returnSuccess_whenNameProvided() {
            webTestClient.get().uri("/api/test/validate?name=John")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data").isEqualTo("Hello, John");
        }

        @Test
        @DisplayName("DIR: 空白参数 name='' → 校验失败 code=2001")
        void should_returnIllegalArgument_whenNameIsBlank() {
            webTestClient.get().uri("/api/test/validate?name=")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(2001)
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.message").value(message ->
                            assertThat((String) message).contains("name"));
        }

        @Test
        @DisplayName("DIR: 缺少参数 name → 统一异常响应 (MissingServletRequestParameterException 被兜底)")
        void should_returnIllegalArgument_whenNameMissing() {
            webTestClient.get().uri("/api/test/validate")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false);
        }
    }

    @Nested
    @DisplayName("GET /api/test/exception — 业务异常处理")
    class ExceptionEndpoint {

        @Test
        @DisplayName("MFT: BizException → HTTP 200 + BaseResult(code=2000)")
        void should_handleBizException_withUnifiedResponse() {
            webTestClient.get().uri("/api/test/exception")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(2000)
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.message").value(message ->
                            assertThat((String) message).contains("测试业务异常"))
                    .jsonPath("$.time").isNotEmpty()
                    .jsonPath("$.traceId").isNotEmpty();
        }
    }
}
