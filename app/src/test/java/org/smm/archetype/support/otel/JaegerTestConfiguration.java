package org.smm.archetype.support.otel;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Jaeger All-in-One Testcontainers 配置（V3 API）。
 * <p>
 * 使用 {@code jaegertracing/jaeger} 镜像，原生支持 OTLP 采集和 V3 查询 API。
 * Jaeger 2.x 默认启用 OTLP Receiver，无需 {@code COLLECTOR_OTLP_ENABLED} 环境变量。
 * <p>
 * 开发环境可通过 {@code SpringApplication.from(WebStartLightApplication::main)
 * .with(JaegerTestConfiguration.class).run(args)} 启动 Jaeger + 应用。
 * <p>
 * 暴露端口：
 * <ul>
 *   <li>16686 — Jaeger UI + V3 HTTP Query API（{@code /api/v3/*}）</li>
 *   <li>16685 — V3 gRPC Query API（{@code api_v3.QueryService}）</li>
 *   <li>4317 — OTLP gRPC 采集（V3 标准）</li>
 *   <li>4318 — OTLP HTTP 采集（V3 标准）</li>
 * </ul>
 */
@TestConfiguration(proxyBeanMethods = false)
public class JaegerTestConfiguration {

    /** Jaeger v2.17.0 — 固定版本确保构建可复现（2026-02-09 发布）。 */
    private static final String JAEGER_IMAGE = "jaegertracing/jaeger:2.17.0";

    @Bean
    public GenericContainer<?> jaegerContainer(DynamicPropertyRegistry registry) {
        GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse(JAEGER_IMAGE))
                .withExposedPorts(16686, 16685, 4317, 4318)
                .waitingFor(Wait.forHttp("/").forPort(16686))
                .withReuse(true);

        // 动态注册 OTLP 端点到 Spring 配置（OTLP 协议本身已是 V3 标准）
        registry.add("management.opentelemetry.tracing.export.otlp.endpoint",
                () -> "http://localhost:" + container.getMappedPort(4318) + "/v1/traces");
        registry.add("management.opentelemetry.logging.export.otlp.endpoint",
                () -> "http://localhost:" + container.getMappedPort(4318) + "/v1/logs");
        registry.add("management.otlp.metrics.export.url",
                () -> "http://localhost:" + container.getMappedPort(4318) + "/v1/metrics");

        return container;
    }
}
