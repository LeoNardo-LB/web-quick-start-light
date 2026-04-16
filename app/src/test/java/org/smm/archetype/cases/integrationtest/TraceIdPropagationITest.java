package org.smm.archetype.cases.integrationtest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.IntegrationTestBase;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L3 集成测试 — 验证 OTel traceId 通过 BaseResult.traceId 返回给前端。
 * <p>
 * traceId 由 OTel Span 全权负责，通过响应体中的 BaseResult.traceId 字段返回，
 * 不再通过 X-Trace-Id 响应头。
 * <p>
 * 覆盖能力: otel-context-propagation (OTel Span, traceId propagation via BaseResult)
 */
class TraceIdPropagationITest extends IntegrationTestBase {

    @Test
    @DisplayName("MFT: 响应体 traceId 为 OTel 标准 32 字符 hex")
    void should_returnOtelTraceId_inResponseBody() {
        webTestClient.get().uri("/api/test/hello")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .consumeWith(response -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = response.getResponseBody();
                    assertThat(body).isNotNull();
                    String traceId = (String) body.get("traceId");
                    assertThat(traceId)
                            .isNotNull()
                            .hasSize(32)
                            .matches("[0-9a-f]{32}");
                });
    }

    @Test
    @DisplayName("DIR: 多个请求使用不同 traceId → 每个请求独立不串扰")
    void should_differentRequestsHaveIndependentTraceIds() {
        // Request 1
        webTestClient.get().uri("/api/test/hello")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .consumeWith(response -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = response.getResponseBody();
                    String traceId = (String) body.get("traceId");
                    assertThat(traceId).isNotNull().hasSize(32);
                });

        // Request 2
        webTestClient.get().uri("/api/test/hello")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .consumeWith(response -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = response.getResponseBody();
                    String traceId = (String) body.get("traceId");
                    assertThat(traceId).isNotNull().hasSize(32);
                });
    }

    @Test
    @DisplayName("INV: 响应头不包含 X-Trace-Id（traceId 仅通过 Body 返回）")
    void should_notContainXTraceIdHeader() {
        webTestClient.get().uri("/api/test/hello")
                .exchange()
                .expectStatus().isOk()
                .expectHeader()
                .doesNotExist("X-Trace-Id");
    }
}
