package org.smm.archetype.cases.integrationtest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.IntegrationTestBase;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L3 集成测试 — 验证 ContextFillFilter + ScopedThreadContext traceId 全链路传递
 * <p>
 * 覆盖能力: thread-context (ContextFillFilter, ScopedThreadContext, traceId propagation)
 */
class TraceIdPropagationITest extends IntegrationTestBase {

    @Test
    @DisplayName("MFT: 不携带 X-Trace-Id → 自动生成并返回在响应头中")
    void should_generateTraceId_whenNotProvided() {
        webTestClient.get().uri("/api/test/hello")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().value("X-Trace-Id", traceId ->
                        assertThat(traceId).isNotNull().isNotBlank().hasSize(32)); // UUID without dashes
    }

    @Test
    @DisplayName("INV: 携带 X-Trace-Id → 原样返回在响应头中")
    void should_returnProvidedTraceId() {
        String customTraceId = "my-custom-trace-id-12345";

        webTestClient.get().uri("/api/test/hello")
                .header("X-Trace-Id", customTraceId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Trace-Id", customTraceId)
                .expectBody()
                .jsonPath("$.traceId").isEqualTo(customTraceId);
    }

    @Test
    @DisplayName("INV: 响应体中的 traceId 与响应头中的 X-Trace-Id 一致")
    void should_traceIdInBodyMatchHeader() {
        webTestClient.get().uri("/api/test/hello")
                .header("X-Trace-Id", "sync-trace-123")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Trace-Id", "sync-trace-123")
                .expectBody()
                .jsonPath("$.traceId").isEqualTo("sync-trace-123");
    }

    @Test
    @DisplayName("DIR: 多个请求使用不同 traceId → 每个请求独立不串扰")
    void should_differentRequestsHaveIndependentTraceIds() {
        // Request 1
        webTestClient.get().uri("/api/test/hello")
                .header("X-Trace-Id", "trace-request-001")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.traceId").isEqualTo("trace-request-001");

        // Request 2
        webTestClient.get().uri("/api/test/hello")
                .header("X-Trace-Id", "trace-request-002")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.traceId").isEqualTo("trace-request-002");

        // Request 3 without traceId
        webTestClient.get().uri("/api/test/hello")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .consumeWith(result -> {
                    Map body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat((String) body.get("traceId"))
                            .isNotIn("trace-request-001", "trace-request-002");
                });
    }
}
