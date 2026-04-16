# Tasks: integrate-opentelemetry-jaeger

> 分 5 个 Chunk 执行，每个 Chunk 结束时有验证步骤。

---

## Chunk 1: OTel 基础设施搭建（无破坏性）

_目标：添加依赖和配置，应用可正常启动，Jaeger UI 可看到自动链路。新旧代码共存，旧 ScopedThreadContext 仍可使用。_

- [x] 1.1 **app/pom.xml**: 添加 `spring-boot-starter-opentelemetry` 依赖（无版本号，由 Spring Boot BOM 管理）；添加 `spring-boot-testcontainers`（test scope）；添加 `testcontainers`（test scope）
  - Spec: otel-jaeger-visualization → "spring-boot-starter-opentelemetry 依赖引入"
- [x] 1.2 **app/pom.xml**: 移除显式 `micrometer-core` 依赖（OTel starter 传递包含）
  - Spec: otel-jaeger-visualization → "spring-boot-starter-opentelemetry 依赖引入"
- [x] 1.3 **app/src/main/resources/application.yaml**: 添加 `management.endpoints.web.exposure.include=health,info,prometheus,metrics`；添加 `management.tracing.sampling.probability=1.0`（开发环境全量采样）
  - Spec: otel-jaeger-visualization → "Actuator 端点暴露"、"OTLP 导出配置"
- [x] 1.4 **app/src/main/resources/application-dev.yaml**: 添加 OTLP 导出端点配置（`management.opentelemetry.tracing.export.otlp.endpoint`、`management.otlp.metrics.export.url`、`management.opentelemetry.logging.export.otlp.endpoint`），均指向 `http://localhost:4318/v1/*`
  - Spec: otel-jaeger-visualization → "OTLP 导出配置"
- [x] 1.5 **app/src/main/resources/application-component.yaml**: 确认 `component.auth.exclude-paths` 包含 `/actuator/**`
  - Spec: otel-jaeger-visualization → "Actuator 端点暴露"
- [x] 1.6 **app/src/test/resources/jaeger-config.yaml**: 创建 Jaeger v2 配置文件，声明 otlp receiver（gRPC :4317 + HTTP :4318）、内存存储后端、traces pipeline
  - Spec: otel-jaeger-visualization → "Jaeger v2 配置文件"
- [x] 1.7 **app/src/test/java/.../JaegerTestConfiguration.java**: 创建 TestConfiguration 类，使用 `GenericContainer`（`jaegertracing/all-in-one`）+ `@DynamicPropertyRegistry`，暴露端口 16686/4317/4318，配置 `withReuse(true)`
  - Spec: otel-jaeger-visualization → "Jaeger v2 容器配置"
- [x] 1.8 **app/src/test/java/.../TestWebStartLightApplication.java**: 创建开发入口类，使用 `SpringApplication.from(...).with(JaegerTestConfiguration.class).run(args)`
  - Spec: otel-jaeger-visualization → "TestApplication 开发入口"
- [x] 1.9 **验证**: 执行 `mvn clean compile -pl app` 确认编译通过；确认无启动异常
  - Spec: otel-jaeger-visualization → "无后端时应用正常启动"

---

## Chunk 2: 上下文核心迁移（traceId + userId）

_目标：创建 OTel 版本的上下文工具，重写 Filter，修改所有消费点。此 Chunk 完成后 ScopedThreadContext 可被删除。_

- [x] 2.1 **创建 UserContext.java**（`app/src/main/java/org/smm/archetype/shared/util/context/UserContext.java`）：final 类，提供 `getUserId()` 从 OTel Baggage 读取、`setUserId(String)` 写入 Baggage。Baggage 中无 userId 时返回 null
  - Spec: otel-context-propagation → "UserContext 工具类"、"userId 通过 OTel Baggage 传播"
- [x] 2.2 **重写 ContextFillFilter.java**：移除 UUID traceId 生成逻辑；新增从 OTel Span 读取 traceId 写入 X-Trace-Id 响应头；同时填充 OTel Baggage（UserContext）和 ScopedThreadContext（业务上下文）；保留 AuthComponent 可选注入
  - Spec: otel-context-propagation → "ContextFillFilter 重写为 OTel 上下文桥接"
- [x] 2.3 **修改 WebConfigure.java**：Filter order 设为 100（保证在 OTel Servlet Filter 之后执行）
  - Spec: otel-context-propagation → "Filter 执行顺序"
- [x] 2.4 **修改 BaseResult.java**：将 `ScopedThreadContext.getTraceId()` 替换为 `Span.current().getSpanContext().getTraceId()`（success() 和 fail() 两个工厂方法）
  - Spec: otel-context-propagation → "BaseResult traceId 来源迁移到 OTel Span"
- [x] 2.5 **修改 BasePageResult.java**：将 `ScopedThreadContext.getTraceId()` 替换为 `Span.current().getSpanContext().getTraceId()`
  - Spec: otel-context-propagation → "BaseResult traceId 来源迁移到 OTel Span"
- [x] 2.6 **修改 MyMetaObjectHandler.java**：保持使用 `ScopedThreadContext.getUserId()`（业务上下文不变）
  - Spec: otel-context-propagation → "MyMetaObjectHandler userId 来源迁移"
- [x] 2.7 **修改 OperationLogFacadeImpl.java**：将 `ScopedThreadContext.getTraceId()` 替换为 `Span.current().getSpanContext().getTraceId()`
  - Spec: otel-context-propagation → "BaseResult traceId 来源迁移到 OTel Span"
- [x] 2.8 **修改 LogAspect.java**：将 OperationLogRecord 构造中的 traceId 从 `""` 改为 `Span.current().getSpanContext().getTraceId()`
  - Spec: otel-metrics-migration → "OperationLogRecord traceId 来源"
- [x] 2.9 **修改 ThreadPoolConfigure.java**：为每个 ThreadPoolTaskExecutor 添加 OTel Context TaskDecorator，ScheduledExecutorService 也包装 OTel Context
  - Spec: otel-context-propagation → "自定义 Executor Bean 支持 OTel 上下文传播"
- [x] 2.10 **验证**: 执行 `mvn clean test -pl app` 确认所有测试通过（481 tests, 0 failures）
  - Spec: otel-context-propagation → "编译时无 ScopedThreadContext 引用"

---

## Chunk 3: Micrometer 指标清理

_目标：从 LogAspect 和 LoggingConfigure 中移除手动 Micrometer 指标逻辑。_

- [x] 3.1 **修改 LogAspect.java**：移除 `MeterRegistry` 字段和构造函数参数；移除 `Timer executionTimer`、`Counter executionCounter`、`Counter errorCounter` 字段；移除 `initIfNecessary()` 方法；移除 `Timer.start()`、`Timer.Sample.stop()`、`Counter.increment()` 调用；保留 `@Around` 切面主体（日志记录、OperationLogRecord 创建、采样率过滤）
  - Spec: otel-metrics-migration → "LogAspect 移除手动 Micrometer 指标"
- [x] 3.2 **修改 LoggingConfigure.java**：`logAspect()` Bean 方法移除 `MeterRegistry` 参数，改为无参或仅接受可选的 `OperationLogWriter`
  - Spec: otel-metrics-migration → "LoggingConfigure 移除 MeterRegistry 依赖"
- [x] 3.3 **修改测试文件**：更新 `LogAspectDBUTest.java` 移除 `SimpleMeterRegistry` 相关代码；更新 `LoggingConfigureDirectoryUTest.java` 移除 mock MeterRegistry
  - Spec: otel-metrics-migration → "LogAspect 不再依赖 MeterRegistry"
- [x] 3.4 **验证**: 执行 `mvn test -pl app` 确认所有测试通过；确认无 `log_aspect_timer_seconds` 等 Micrometer 指标注册
  - Spec: otel-metrics-migration → "@BusinessLog 注解保持功能"

---

## Chunk 4: 旧代码清理与测试重写

_目标：ScopedThreadContext/ContextRunnable/ContextCallable 保留作为业务上下文机制，仅更新受影响的测试。_

- [x] 4.1 **保留 ScopedThreadContext.java** — 作为业务上下文（userId 等），不删除
  - Spec: otel-context-propagation → "ScopedThreadContext 仅作为业务上下文"
- [x] 4.2 **保留 ContextRunnable.java** — 通用上下文传播工具，不删除
  - Spec: otel-context-propagation → "ContextRunnable 保留"
- [x] 4.3 **保留 ContextCallable.java** — 通用上下文传播工具，不删除
  - Spec: otel-context-propagation → "ContextCallable 保留"
- [x] 4.4 **更新测试文件**：更新 `ContextFillFilterUTest.java` 适配 OTel traceId；更新 `BaseResultUTest.java` 验证 OTel Span traceId；更新 `TraceIdPropagationITest.java` 验证 OTel 32 hex 格式
  - Spec: otel-context-propagation → "测试适配 OTel"
- [x] 4.5 **全局搜索验证**：在整个项目中确认无编译错误，ScopedThreadContext 仅用于业务上下文
  - Spec: otel-context-propagation → "编译通过"
- [x] 4.6 **验证**: 执行 `mvn clean test -pl app` 确认编译通过且所有 481 个测试通过
  - Spec: otel-context-propagation → "全部测试通过"

---

## Chunk 5: 集成验证与端到端测试

_目标：验证 OTel + Jaeger 的完整链路，确保三信号（Traces/Metrics/Logs）正常工作。_

- [x] 5.1 **创建 OTel 集成测试**（`app/src/test/java/.../OtelIntegrationITest.java`）：使用 Testcontainers 启动 Jaeger + 应用；发送 HTTP 请求；验证 `BaseResult.traceId` 字段非空且为 32 字符 hex
  - Spec: otel-context-propagation → "HTTP 请求自动获得 OTel traceId"
- [x] 5.2 **创建 Jaeger 数据验证测试**（`app/src/test/java/.../JaegerDataVerificationITest.java`）：通过 Jaeger V3 HTTP API（`/api/v3/traces`、`/api/v3/services`）查询 trace 数据；验证 trace 存在且包含 HTTP 请求 Span。镜像已升级为 `jaegertracing/jaeger`，移除废弃的 `COLLECTOR_OTLP_ENABLED` 环境变量
  - Spec: otel-jaeger-visualization → "Jaeger V3 API 验证链路"
- [x] 5.3 **创建 MDC 验证测试**（`app/src/test/java/.../TraceIdPropagationITest.java`）：在集成测试中验证日志输出包含有效 traceId（通过 MDC）；验证 `%X{traceId}` 不再是 `--`
  - Spec: logging-enhancement → "OTel 自动填充 MDC traceId"
- [x] 5.4 **创建异步上下文传播测试**（`app/src/test/java/.../TaskDecoratorContextPropagationITest.java`）：使用自定义 Executor Bean 提交任务；在任务中验证上下文传播正确
  - Spec: otel-context-propagation → "自定义 Executor Bean 支持 OTel 上下文传播"
- [x] 5.5 **验证 SlowQueryInterceptor traceId**：通过 `LogAutoConfigurationITest` 和 `BusinessLogITest` 验证日志基础设施正确配置，traceId 通过 OTel MDC 传播
  - Spec: otel-context-propagation → "SlowQueryInterceptor 通过 OTel MDC 获取 traceId"
- [x] 5.6 **验证**: 执行 `mvn clean verify -pl app`，确认全部测试通过（526 tests, 0 failures, BUILD SUCCESS）；JaCoCo 覆盖率全部达标
  - Spec: otel-jaeger-visualization → "无后端时应用正常启动"

---

## [ ] 进行 artifact 文档、讨论结果的一致性检查

> 在所有 artifact 创建完毕后执行三维度交叉比对校验。
