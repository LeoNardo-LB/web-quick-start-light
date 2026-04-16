## ADDED Requirements

### Requirement: spring-boot-starter-opentelemetry 依赖引入

app 模块 SHALL 引入 `spring-boot-starter-opentelemetry` 依赖，由 Spring Boot BOM 统一管理版本。

#### Scenario: Maven 依赖声明

- **WHEN** 查看 app/pom.xml
- **THEN** SHALL 包含 `<artifactId>spring-boot-starter-opentelemetry</artifactId>` 依赖声明（无版本号，由 BOM 管理）
- **AND** SHALL NOT 包含 `io.opentelemetry:opentelemetry-bom` 的显式声明（由 Spring Boot BOM 传递管理）
- **AND** 显式的 `micrometer-core` 依赖 SHALL 被移除（OTel starter 已传递包含）

### Requirement: Jaeger v2 Testcontainers 开发环境

开发/测试环境 SHALL 通过 Testcontainers 自动启动 Jaeger v2 All-in-One 容器，提供 OTLP 接收端和 Web UI。

#### Scenario: Jaeger v2 容器配置

- **WHEN** 开发者运行 TestApplication 或执行集成测试
- **THEN** SHALL 自动启动 `jaegertracing/all-in-one:2.x` 容器（具体版本由 Spring Boot BOM 管理）
- **AND** 容器 SHALL 暴露以下端口：16686（Jaeger UI）、4317（OTLP gRPC）、4318（OTLP HTTP）
- **AND** 容器 SHALL 使用内存存储模式（开发环境不依赖持久化）
- **AND** 容器 SHALL 挂载 `jaeger-config.yaml` 配置文件

#### Scenario: Jaeger v2 配置文件

- **WHEN** 查看 `src/test/resources/jaeger-config.yaml`
- **THEN** SHALL 声明 `otlp` receiver，监听 `0.0.0.0:4317`（gRPC）和 `0.0.0.0:4318`（HTTP）
- **AND** SHALL 声明内存存储后端
- **AND** SHALL 配置 traces pipeline 连接 otlp receiver 到存储后端

#### Scenario: TestApplication 开发入口

- **WHEN** 开发者在 IDE 中运行 TestApplication 的 main 方法
- **THEN** SHALL 先启动 Jaeger v2 容器，再启动主应用
- **AND** 主应用 SHALL 通过 `@ServiceConnection` 自动获取 Jaeger 容器的 OTLP 端口信息

#### Scenario: Testcontainers 依赖

- **WHEN** 查看 app/pom.xml
- **THEN** SHALL 包含 `spring-boot-testcontainers` 依赖（test scope）
- **AND** SHALL 包含 `testcontainers` 依赖（test scope）
- **AND** 这些依赖 SHALL NOT 出现在 compile scope 中

### Requirement: OTLP 导出配置

应用 SHALL 通过 OTLP 协议将 Traces、Metrics、Logs 三信号导出到配置的后端端点。

#### Scenario: Traces 导出配置

- **WHEN** 应用启动且 `management.opentelemetry.tracing.export.otlp.endpoint` 已配置
- **THEN** 应用 SHALL 通过 OTLP HTTP 协议将 trace 数据导出到指定端点
- **AND** 默认端点 SHALL 为 `http://localhost:4318/v1/traces`

#### Scenario: Metrics 导出配置

- **WHEN** 应用启动且 `management.otlp.metrics.export.url` 已配置
- **THEN** 应用 SHALL 通过 OTLP HTTP 协议将 metrics 数据导出到指定端点
- **AND** 默认端点 SHALL 为 `http://localhost:4318/v1/metrics`

#### Scenario: Logs 导出配置

- **WHEN** 应用启动且 `management.opentelemetry.logging.export.otlp.endpoint` 已配置
- **THEN** 应用 SHALL 通过 OTLP HTTP 协议将 structured log 数据导出到指定端点
- **AND** 默认端点 SHALL 为 `http://localhost:4318/v1/logs`

#### Scenario: 采样率配置

- **WHEN** `management.tracing.sampling.probability` 设置为 1.0
- **THEN** 所有请求 SHALL 被采样（100% 采样率，适用于开发环境）
- **WHEN** 设置为 0.1
- **THEN** 约 10% 的请求 SHALL 被采样

#### Scenario: 无后端时应用正常启动

- **WHEN** OTLP 端点不可达（如生产环境未部署 Jaeger）
- **THEN** 应用 SHALL 正常启动和运行（OTel SDK 优雅降级）
- **AND** 不 SHALL 因导出失败而影响业务逻辑

### Requirement: Actuator 端点暴露

应用 SHALL 暴露必要的 Actuator 端点以支持可观测性。

#### Scenario: 端点暴露配置

- **WHEN** 查看 application.yaml 配置
- **THEN** `management.endpoints.web.exposure.include` SHALL 包含 `health`、`info`、`prometheus`、`metrics`
- **AND** Actuator 端点 SHALL 被认证排除（`component.auth.exclude-paths` 包含 `/actuator/**`）

#### Scenario: Prometheus 端点可用

- **WHEN** 访问 `/actuator/prometheus`
- **THEN** SHALL 返回 Prometheus 格式的指标数据（由 OTel starter 自动配置 Micrometer → Prometheus registry 桥接）

### Requirement: Jaeger UI 验证链路

集成测试 SHALL 验证 HTTP 请求的追踪链路能在 Jaeger UI 中可见。

#### Scenario: 集成测试验证 Jaeger 数据

- **WHEN** 运行 OTel 集成测试
- **THEN** SHALL 发送 HTTP 请求到应用
- **AND** SHALL 通过 Jaeger API 查询到对应的 trace 数据
- **AND** trace 中 SHALL 包含 HTTP 请求的 Span（含 URL、状态码、耗时）
