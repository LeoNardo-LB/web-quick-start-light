## Why

项目当前的链路追踪机制存在根本性缺陷：自研的 `ContextFillFilter` 生成 UUID traceId 并存入 `ScopedValue`，但**从未写入 MDC**，导致日志中 `%X{traceId:--}` 始终显示 `--`，`SlowQueryInterceptor` 中 `MDC.get("traceId")` 始终为 null，异步线程传播的 MDC 快照也是空的。traceId 仅在 API 响应体（`BaseResult.traceId`）和响应头（`X-Trace-Id`）中有效，无法用于日志关联和问题排查。

同时，项目没有任何分布式追踪、指标导出或可视化监控能力。Micrometer 仅在 `LogAspect` 中注册了 3 个指标（Timer + 2 Counter），无 `@Timed`、无 `Observation` API、无 Actuator 端点暴露、无 Prometheus 导出。

Spring Boot 4.0 正式推出了 `spring-boot-starter-opentelemetry`，提供 Traces/Metrics/Logs 三信号的标准化采集与 OTLP 导出。这是全面替代自研 traceId 机制并引入生产级可观测性的最佳时机。

## What Changes

- **BREAKING**: 删除自研 `ScopedThreadContext`（基于 Java 25 ScopedValue 的 traceId/userId 传播）、`ContextRunnable`、`ContextCallable`，全面替换为 OTel 标准的 Context 传播 + Baggage API
- **BREAKING**: 重写 `ContextFillFilter`，从"生成 UUID traceId + 绑定 ScopedValue"改为"从 OTel Span 读取 traceId 写入响应头 + 将 userId 注入 OTel Baggage"
- **BREAKING**: `BaseResult` / `BasePageResult` 的 `traceId` 字段从 `ScopedThreadContext.getTraceId()` 改为从 OTel 当前 Span 读取
- **BREAKING**: `LogAspect` 移除手动 Micrometer `Timer`/`Counter` 指标采集，由 OTel 自动 instrumentation 覆盖
- **BREAKING**: `MyMetaObjectHandler` 的 userId 来源从 `ScopedThreadContext.getUserId()` 改为 OTel Baggage
- **BREAKING**: `OperationLogFacadeImpl` 的 traceId 来源从 `ScopedThreadContext.getTraceId()` 改为 OTel Span
- 新增 `spring-boot-starter-opentelemetry` 依赖，引入 OTel SDK 自动配置
- 新增 Jaeger v2 Testcontainers 配置，开发环境一键启动可视化后端
- 新增 `UserContext` 工具类（基于 OTel Baggage API）替代 `ScopedThreadContext` 的 userId 功能
- 新增 `management.opentelemetry.*` / `management.otlp.*` YAML 配置，启用 OTLP 导出
- 新增 Actuator 端点暴露配置（health, info, prometheus, metrics）
- `SlowQueryInterceptor` 的 `MDC.get("traceId")` 无需代码改动即可生效（OTel 自动填充 MDC）
- 移除 `app/pom.xml` 中显式的 `micrometer-core` 依赖（OTel starter 已传递包含）

## Capabilities

### New Capabilities

- `otel-context-propagation`: OTel 标准化的上下文传播机制。涵盖 traceId（W3C Trace Context）、userId（OTel Baggage）、MDC 自动填充、异步线程上下文传播。替代自研的 ScopedThreadContext + ContextRunnable + ContextCallable 体系
- `otel-jaeger-visualization`: Jaeger v2 可视化后端集成。涵盖 Testcontainers 伴随启动（开发/测试环境）、Jaeger v2 YAML 配置、OTLP 协议导出配置、Actuator 端点暴露。提供 Web UI 查看分布式追踪链路
- `otel-metrics-migration`: Micrometer 指标迁移至 OTel 体系。涵盖从 LogAspect 移除手动 Timer/Counter、OTel 自动 HTTP 请求追踪替代、保留 @BusinessLog 业务日志功能

### Modified Capabilities

- `logging-enhancement`: 日志基础设施变更 — LoggingConfigure 不再注入 MeterRegistry、LogAspect 不再包含 Micrometer 指标逻辑、logback-spring.xml 的 `%X{traceId}` 由 OTel 自动填充而非依赖手动 MDC.put

## Impact

**代码变更**:
- 删除 3 个文件：`ScopedThreadContext.java`、`ContextRunnable.java`、`ContextCallable.java`
- 重构 11 个文件：`ContextFillFilter.java`、`WebConfigure.java`、`BaseResult.java`、`BasePageResult.java`、`LogAspect.java`、`LoggingConfigure.java`、`SlowQueryInterceptor.java`（可能无需改动）、`MyMetaObjectHandler.java`、`OperationLogFacadeImpl.java`、`ThreadPoolConfigure.java`（可能需添加 TaskDecorator）、`logback-spring.xml`（微调）
- 新增 ~5 个文件：`UserContext.java`、`JaegerTestConfiguration.java`、`jaeger-config.yaml`、`TestApplication.java`、YAML 配置片段

**依赖变更**:
- 新增：`spring-boot-starter-opentelemetry`、`spring-boot-testcontainers`、`testcontainers`
- 移除：显式 `micrometer-core`（OTel starter 传递包含）

**API 变更**:
- `BaseResult.traceId` 字段值从 UUID（无横线）变为 OTel 32 字符 hex traceId
- `X-Trace-Id` 响应头值从 UUID 变为 OTel W3C traceId
- 新增 Actuator 端点：`/actuator/prometheus`、`/actuator/metrics`

**无影响**:
- 所有 component 模块（auth/cache/oss/email/sms/search）
- 异常体系（common 模块）
- 限流 / 幂等切面
- MyBatis-Plus 配置
- Sa-Token 认证拦截器
