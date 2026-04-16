package org.smm.archetype.support.otel;

import org.smm.archetype.WebStartLightApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/**
 * 开发环境入口：启动 Jaeger Testcontainers + 主应用。
 * <p>
 * 在 IDE 中运行此类的 main 方法即可：
 * <ol>
 *   <li>自动启动 Jaeger 容器（V3 API，原生 OTLP 支持）</li>
 *   <li>自动配置 OTLP 导出端点</li>
 *   <li>启动主应用</li>
 * </ol>
 * <p>
 * Jaeger UI: http://localhost:16686
 * <br>
 * V3 HTTP API: http://localhost:16686/api/v3/{traces,services}
 * <br>
 * V3 gRPC API: localhost:16685
 */
@TestConfiguration(proxyBeanMethods = false)
@Import(JaegerTestConfiguration.class)
public class TestWebStartLightApplication {

    public static void main(String[] args) {
        SpringApplication.from(WebStartLightApplication::main)
                .with(TestWebStartLightApplication.class)
                .run(args);
    }
}
