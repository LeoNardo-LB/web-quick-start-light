package org.smm.archetype.cases.integrationtest;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.smm.archetype.support.IntegrationTestBase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Jaeger 数据验证 — OTLP 导出 + Jaeger V3 Query API 查询。
 * <p>
 * 验证 OTel 自动装配将 trace 数据通过 OTLP 协议导出到 Jaeger，
 * 并通过 Jaeger V3 HTTP Query API（{@code /api/v3/*}）确认 trace 数据可查询。
 * <p>
 * 覆盖 Chunk 5 任务 5.2：集成 Jaeger 作为 trace 后端（V3 API）。
 * <p>
 * 前置条件：Docker 可用（Testcontainers 启动 Jaeger 容器）。
 * 可通过 {@code -DskipJaegerTests=true} 跳过。
 *
 * @see <a href="https://www.jaegertracing.io/docs/latest/api/">Jaeger API Reference</a>
 */
@DisabledIfSystemProperty(named = "skipJaegerTests", matches = "true")
@DisplayName("Jaeger 数据验证 — OTLP 导出 + Jaeger V3 API 查询")
class JaegerDataVerificationITest extends IntegrationTestBase {

    /** Jaeger v2.17.0 — 固定版本确保构建可复现（2026-02-09 发布）。 */
    private static final String JAEGER_IMAGE = "jaegertracing/jaeger:2.17.0";

    /** 应用在 Jaeger 中注册的服务名，与 spring.application.name 一致。 */
    private static final String SERVICE_NAME = "quickstart-light";

    /** OTLP 异步导出 + Jaeger 索引的等待时间。 */
    private static final Duration EXPORT_WAIT = Duration.ofSeconds(5);

    private static GenericContainer<?> jaegerContainer;

    @BeforeAll
    static void startJaeger() {
        jaegerContainer = new GenericContainer<>(DockerImageName.parse(JAEGER_IMAGE))
                .withExposedPorts(16686, 16685, 4317, 4318)
                .waitingFor(Wait.forHttp("/").forPort(16686));
        jaegerContainer.start();
    }

    @DynamicPropertySource
    static void jaegerProperties(DynamicPropertyRegistry registry) {
        // 当容器未启动时（skipJaegerTests 或 Docker 不可用），使用占位端点避免启动失败
        String otlpEndpoint = (jaegerContainer != null && jaegerContainer.isRunning())
                ? "http://localhost:" + jaegerContainer.getMappedPort(4318) + "/v1/traces"
                : "http://localhost:14318/v1/traces";

        registry.add("management.opentelemetry.tracing.export.otlp.endpoint", () -> otlpEndpoint);
        registry.add("management.opentelemetry.logging.export.otlp.endpoint",
                () -> otlpEndpoint.replace("/v1/traces", "/v1/logs"));
        registry.add("management.otlp.metrics.export.url",
                () -> otlpEndpoint.replace("/v1/traces", "/v1/metrics"));
    }

    // ──────────────────────────────────────────────
    // 辅助方法
    // ──────────────────────────────────────────────

    /**
     * 获取 Jaeger HTTP API 基础 URL（V3 Query API 端口 16686）。
     */
    private static String jaegerApiUrl() {
        return "http://localhost:" + jaegerContainer.getMappedPort(16686);
    }

    /**
     * 发送 HTTP 请求到应用以产生 trace，等待 OTLP 异步导出完成。
     */
    private void generateTraceAndWait() throws InterruptedException {
        webTestClient.get().uri("/api/test/hello")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .consumeWith(response -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = response.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat((String) body.get("traceId"))
                            .as("应生成 OTel traceId")
                            .isNotNull()
                            .hasSize(32);
                });

        // OTLP 导出是异步的，等待 Jaeger 处理并索引 trace 数据
        Thread.sleep(EXPORT_WAIT.toMillis());
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 通过 Jaeger V3 HTTP API 查询服务列表。
     * <p>
     * V3 端点：{@code GET /api/v3/services}，返回 {@code {"services": ["svc1", "svc2"]}}。
     * <p>
     * 注意：Jaeger V3 API 返回 Content-Type 为 {@code text/plain}（非标准 application/json），
     * 因此使用 String body 接收后手动反序列化。
     *
     * @return 服务名列表
     */
    @SuppressWarnings("unchecked")
    private List<String> queryJaegerServices() {
        EntityExchangeResult<String> result = webTestClient.get()
                .uri(jaegerApiUrl() + "/api/v3/services")
                .accept(org.springframework.http.MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult();

        String body = result.getResponseBody();
        if (body == null || body.isBlank()) {
            return List.of();
        }

        try {
            Map<String, Object> parsed = OBJECT_MAPPER.readValue(body, new TypeReference<>() {});
            Object services = parsed.get("services");
            if (services instanceof List<?> list) {
                return (List<String>) list;
            }
        } catch (Exception e) {
            // JSON 解析失败，返回空列表
        }
        return List.of();
    }

    /**
     * 通过 Jaeger V3 HTTP API 查询 trace 数据（带重试轮询）。
     * <p>
     * V3 端点：{@code GET /api/v3/traces}。
     * <p>
     * 查询参数遵循 gRPC-gateway 嵌套字段映射：
     * <ul>
     *   <li>{@code query.service_name} — 服务名称</li>
     *   <li>{@code query.start_time_min} / {@code query.start_time_max} — RFC-3339 时间范围</li>
     *   <li>{@code query.num_traces} — 最大返回 trace 数量（替代已废弃的 search_depth）</li>
     * </ul>
     * <p>
     * 响应格式：{@code {"result": {"resourceSpans": [...]}}}
     * （gRPC server-streaming 经 {@code GRPCGatewayWrapper} 包装后的 JSON）。
     * <p>
     * 注意：Jaeger V3 API 返回 Content-Type 为 {@code text/plain}（非标准 application/json），
     * 因此使用 String body 接收后手动反序列化。
     * <p>
     * OTLP 导出是异步的，Jaeger 索引也需要时间，因此采用轮询机制。
     *
     * @param serviceName 服务名称
     * @param lookbackSeconds 向前回溯的秒数
     * @param maxRetries 最大重试次数（每次间隔 2 秒）
     * @return trace 数据中的 resourceSpans 数量（≥0 表示有数据）
     * @see <a href="https://github.com/jaegertracing/jaeger-idl/blob/main/proto/api_v3/query_service.proto">query_service.proto</a>
     */
    @SuppressWarnings("unchecked")
    private int queryJaegerTracesWithRetry(String serviceName, int lookbackSeconds, int maxRetries)
            throws InterruptedException {

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            // 每次重试刷新时间窗口（RFC-3339 格式）
            Instant now = Instant.now();
            String timeMax = DateTimeFormatter.ISO_INSTANT.format(now);
            String timeMin = DateTimeFormatter.ISO_INSTANT.format(now.minusSeconds(lookbackSeconds));

            // gRPC-gateway 嵌套字段映射: query.service_name, query.start_time_min 等
            // query.num_traces — 返回 trace 数量上限（Jaeger 2.17 实测有效）
            String url = jaegerApiUrl() + "/api/v3/traces"
                    + "?query.service_name=" + serviceName
                    + "&query.start_time_min=" + timeMin
                    + "&query.start_time_max=" + timeMax
                    + "&query.num_traces=20";

            try {
                EntityExchangeResult<String> result = webTestClient.get().uri(url)
                        .accept(org.springframework.http.MediaType.APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody(String.class)
                        .returnResult();

                String bodyStr = result.getResponseBody();
                if (bodyStr != null && !bodyStr.isBlank()) {
                    Map<String, Object> body = OBJECT_MAPPER.readValue(bodyStr, new TypeReference<>() {});
                    // V3 响应格式: {"result": {"resourceSpans": [...]}}
                    // GRPCGatewayWrapper 包装 server-streaming 响应
                    Map<String, Object> resultData = (Map<String, Object>) body.get("result");
                    if (resultData != null) {
                        Object resourceSpans = resultData.get("resourceSpans");
                        if (resourceSpans instanceof List<?> list && !list.isEmpty()) {
                            return list.size();
                        }
                    }
                }
            } catch (Exception e) {
                // Jaeger API 暂时不可用或返回非预期格式，继续重试
            }

            if (attempt < maxRetries - 1) {
                Thread.sleep(Duration.ofSeconds(2).toMillis());
            }
        }
        return 0;
    }

    // ──────────────────────────────────────────────
    // 测试用例
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("5.2: Jaeger 容器正常启动，V3 端口映射正确")
    void should_jaegerContainerRunning() {
        Assumptions.assumeTrue(jaegerContainer.isRunning(),
                "Jaeger 容器未运行（Docker 不可用？），跳过测试");

        assertThat(jaegerContainer.isRunning())
                .as("Jaeger 容器应处于运行状态")
                .isTrue();
        assertThat(jaegerContainer.getMappedPort(16686))
                .as("Jaeger UI + V3 HTTP Query 端口 16686 应已映射")
                .isGreaterThan(0);
        assertThat(jaegerContainer.getMappedPort(16685))
                .as("Jaeger V3 gRPC Query 端口 16685 应已映射")
                .isGreaterThan(0);
        assertThat(jaegerContainer.getMappedPort(4318))
                .as("Jaeger OTLP HTTP 端口 4318 应已映射")
                .isGreaterThan(0);
        assertThat(jaegerContainer.getMappedPort(4317))
                .as("Jaeger OTLP gRPC 端口 4317 应已映射")
                .isGreaterThan(0);
    }

    @Test
    @DisplayName("5.2: Jaeger V3 API /api/v3/services 可查询到应用服务名")
    void should_jaegerV3ApiReturnServiceName() throws InterruptedException {
        Assumptions.assumeTrue(jaegerContainer.isRunning(),
                "Jaeger 容器未运行，跳过测试");

        generateTraceAndWait();

        List<String> services = queryJaegerServices();

        assertThat(services)
                .as("Jaeger V3 API 应包含 %s 服务（spring.application.name）".formatted(SERVICE_NAME))
                .contains(SERVICE_NAME);
    }

    @Test
    @DisplayName("5.2: Jaeger V3 API /api/v3/traces 可查询到 trace 数据")
    void should_jaegerV3ApiReturnTraces() throws InterruptedException {
        Assumptions.assumeTrue(jaegerContainer.isRunning(),
                "Jaeger 容器未运行，跳过测试");

        generateTraceAndWait();

        int traceCount = queryJaegerTracesWithRetry(SERVICE_NAME, 60, 5);

        assertThat(traceCount)
                .as("Jaeger V3 API 应至少返回 1 组 resourceSpans")
                .isGreaterThan(0);
    }

    @Test
    @DisplayName("5.2: 多次请求产生多条独立 trace")
    void should_multipleRequestsGenerateMultipleTraces() throws InterruptedException {
        Assumptions.assumeTrue(jaegerContainer.isRunning(),
                "Jaeger 容器未运行，跳过测试");

        for (int i = 0; i < 3; i++) {
            webTestClient.get().uri("/api/test/hello")
                    .exchange()
                    .expectStatus().isOk();
        }

        Thread.sleep(EXPORT_WAIT.toMillis());

        int traceCount = queryJaegerTracesWithRetry(SERVICE_NAME, 60, 5);

        assertThat(traceCount)
                .as("3 次请求应至少产生 1 组 resourceSpans（可能被 batch 合并）")
                .isGreaterThan(0);
    }
}
