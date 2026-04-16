## Context

### 当前状态

项目（web-quick-start-light）是一个基于 Java 25 + Spring Boot 4.0.2 的单体应用脚手架，采用多模块四层架构（Controller → Facade → Service → Repository）。当前的可观测性状况如下：

**链路追踪**：自研方案，存在根本性缺陷。

```
ContextFillFilter (Servlet Filter, order=1)
  ├─ 生成/读取 UUID traceId（X-Trace-Id Header）
  ├─ 绑定到 ScopedThreadContext（Java 25 ScopedValue）
  ├─ ⚠️ 从未调用 MDC.put("traceId", ...)
  └─ 通过 ScopedValue.runWithContext() 传递

ScopedThreadContext.Context(userId, traceId)
  ├─ ContextRunnable / ContextCallable 手动传播到异步线程
  └─ MDC 传播代码存在但捕获的是空 Map（上游未写入）

消费点:
  ├─ BaseResult.success()/fail() → ScopedThreadContext.getTraceId()  ✅
  ├─ BasePageResult.fromPage()   → ScopedThreadContext.getTraceId()  ✅
  ├─ MyMetaObjectHandler         → ScopedThreadContext.getUserId()   ✅
  ├─ logback %X{traceId:--}      → MDC.get("traceId")               ❌ 永远 "--"
  ├─ SlowQueryInterceptor        → MDC.get("traceId")               ❌ 永远 null
  └─ LogAspect                   → traceId="" 传空，由 writer 填充    ⚠️ writer 不存在
```

**指标监控**：极有限。仅 `LogAspect` 中 3 个手动 Micrometer 指标（`log_aspect_timer_seconds` + `log_aspect_counter_total` + `log_aspect_errors_total`），无 `@Timed`、无 `Observation` API、无 Prometheus 导出、无 `management.*` YAML 配置。

**日志**：Logback + Logstash JSON 编码器，结构完善但 traceId 关联断裂。

**组件模块**：6 个 component 模块（auth/cache/oss/email/sms/search），均不涉及可观测性。

### 约束

| 约束 | 说明 |
|------|------|
| Java 25 | 使用 ScopedValue（已在项目中使用），OTel SDK 需兼容 |
| Spring Boot 4.0.2 | 原生支持 `spring-boot-starter-opentelemetry` |
| Sa-Token 认证 | 拦截器与 OTel Filter 需协调顺序 |
| 手动 SqlSessionFactory | 因 Spring Boot 4.0.2 的 DataSource 类型变更，MyBatis-Plus 自动配置失效 |
| SQLite 数据源 | OTel JDBC instrumentation 在 driver 层工作，不受数据库类型影响 |
| 无远程仓库 | 项目仅本地 Git，无需考虑 CI/CD 集成 |

## Goals / Non-Goals

**Goals:**

1. 全面采用 `spring-boot-starter-opentelemetry`，实现 Traces/Metrics/Logs 三信号的标准化采集
2. 替换自研 traceId 机制为 OTel W3C Trace Context 标准
3. userId 传播从 ScopedValue 迁移到 OTel Baggage API
4. 开发/测试环境通过 Testcontainers 自动启动 Jaeger v2 可视化后端
5. 日志中的 traceId 通过 OTel 自动 MDC 注入实现关联（修复当前断裂问题）
6. 移除 LogAspect 中的手动 Micrometer 指标，由 OTel 自动 instrumentation 覆盖
7. 保持所有现有业务功能不变（@BusinessLog、@RateLimit、@Idempotent 等）
8. 保持 API 响应体中 `traceId` 字段和 `X-Trace-Id` 响应头的行为不变（值格式会变）

**Non-Goals:**

1. **不**引入 OTel Collector 中间件（SDK 直连 Jaeger 后端）
2. **不**引入 Prometheus + Grafana 独立监控栈（Jaeger v2 已提供基本可视化）
3. **不**改动 component 模块（auth/cache/oss/email/sms/search）
4. **不**改动 Sa-Token 认证逻辑
5. **不**实现 `OperationLogWriter` 的数据库持久化（当前也无实现，属于独立需求）
6. **不**迁移到 OTel Java Agent 模式（使用 Spring Boot Starter 方式）
7. **不**引入分布式追踪传播（本项目是单体应用，但保留未来微服务化时的 W3C 标准兼容性）
8. **不**删除 `@BusinessLog` 注解和操作日志功能

## Decisions

### Decision 1: 使用 `spring-boot-starter-opentelemetry` 而非 OTel Java Agent

| 选项 | 优点 | 缺点 |
|------|------|------|
| **✅ spring-boot-starter-opentelemetry** | Spring Boot BOM 统一版本管理；声明式 YAML 配置；无启动开销；兼容 GraalVM Native Image；Testcontainers 自动配置 | 仅自动 instrument Spring 生态组件，非 Spring 库需要手动埋点 |
| ❌ OTel Java Agent (`-javaagent`) | 自动 instrument 数百个库（含非 Spring 的） | 启动开销；版本管理独立于 Spring Boot；不兼容 Native Image；配置通过环境变量而非 YAML |
| ❌ 混合（Starter + Agent） | 兼顾两者优势 | 复杂度翻倍；可能双重 instrument |

**选择理由**：项目是纯 Spring Boot 原生应用，Starter 覆盖的 instrumentation 范围（Spring MVC、JDBC、RestTemplate、WebClient）已满足需求。声明式配置符合项目规范（`@ConfigurationProperties`，禁止 `@Value`）。

### Decision 2: userId 传播使用 OTel Baggage 而非保留 ScopedValue

| 选项 | 优点 | 缺点 |
|------|------|------|
| **✅ OTel Baggage API** | OTel 标准；自动跨线程传播（无需 ContextRunnable/ContextCallable）；可导出到 Span 属性；未来微服务化时跨进程传播 | 访问 API 略冗长（通过工具类封装解决） |
| ❌ 保留 ScopedValue（仅 userId） | 改动最小 | 两套上下文机制并存（OTel Context + ScopedValue）；异步传播仍需 ContextRunnable/ContextCallable |
| ❌ Spring RequestContextHolder | Spring Web 标准 | 依赖 Spring Web 环境，非 Web 场景（定时任务）不可用 |

**选择理由**：全面拥抱 OTel 意味着统一上下文传播机制。Baggage 是 OTel 为应用自定义数据设计的标准传播通道，自动跨线程、跨进程传播，彻底消除手动上下文包装的需求。

### Decision 3: 可视化后端选择 Jaeger v2（All-in-One 模式）

| 选项 | 优点 | 缺点 |
|------|------|------|
| **✅ Jaeger v2 All-in-One** | 极轻量（单容器）；内置 Web UI（:16686）；原生 OTLP 支持；内存存储模式适合开发；v2 架构与 OTel Collector 对齐 | 功能相对简单（不如 Grafana LGTM 栈全面） |
| ❌ Grafana LGTM 栈 | 功能最全（Loki + Grafana + Tempo + Mimir） | 4 个容器；资源消耗大；配置复杂 |
| ❌ Zipkin | 极轻量经典方案 | 社区活跃度下降；OTLP 支持通过协议转换 |
| ❌ SigNoz | 开源 APM，功能强大 | 需要 ClickHouse；资源消耗大 |

**选择理由**：单体应用脚手架项目追求轻量。Jaeger v2 一个容器覆盖 Traces 查看需求。生产环境可根据需要切换后端（OTLP 协议标准保证后端可替换）。

### Decision 4: LogAspect 移除 Micrometer 指标，保留业务日志功能

**当前 LogAspect 职责**：
1. ~~Micrometer Timer/Counter 指标~~ → OTel 自动覆盖
2. @BusinessLog 业务日志记录 → 保留
3. OperationLogRecord 创建 → 保留（traceId 改从 OTel 获取）

**移除理由**：OTel 的 Spring MVC instrumentation 自动为每个 HTTP 请求创建 Span（含计时、状态码、异常），完全覆盖 LogAspect 的 Timer/Counter 功能。保留 @BusinessLog 是因为它承担操作审计的语义（记录谁在什么时间执行了什么操作），这是 OTel 不提供的业务层能力。

### Decision 5: BaseRequest.traceId 字段保留但语义变更

`BaseRequest` 和 `BasePageRequest` 中有 `traceId` 字段（前端传入）。迁移后：
- 该字段保留，但**仅作为前端调试参考**
- 后端 traceId 以 OTel 生成为准（通过 `X-Trace-Id` 请求头或 OTel W3C `traceparent` 传播）
- 不再使用 `BaseRequest.traceId` 来覆盖后端生成的 traceId

### Decision 6: 新增 UserContext 工具类封装 Baggage 访问

```java
// 位置: org.smm.archetype.shared.util.context.UserContext
// 提供 ScopedThreadContext.getUserId() 的等价 API
// 内部通过 OTel Baggage.current().getEntryValue("userId") 实现
// 调用者无需了解 OTel API 细节
public final class UserContext {
    public static String getUserId() { ... }
    public static void setUserId(String userId) { ... }  // 仅在 Filter 中调用
}
```

### Decision 7: ThreadPoolConfigure 上下文传播策略

Spring Boot 4 的 OTel 集成为 `@Async` 标注的方法自动传播上下文。但项目的 4 个自定义 `Executor` Bean（ioThreadPool、cpuThreadPool、daemonThreadPool、schedulerThreadPool）不使用 `@Async`，需要验证 OTel 是否自动包装。

**策略**：先验证 Spring Boot 4 是否自动包装。如果未自动包装，在 `ThreadPoolConfigure` 中为每个 `ThreadPoolTaskExecutor` 添加 `setTaskDecorator()` 使用 OTel 的 `Context.taskWrapping()` 机制。

## Risks / Trade-offs

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| OTel MDC key 与 logback `%X{traceId}` 不匹配 | 🟡 中 — 日志中 traceId 仍为空 | Spring Boot 4 的 Micrometer Tracing bridge 映射为 `traceId`（camelCase），与现有 logback 格式匹配。需编写集成测试验证 |
| 自定义 Executor Bean 未被 OTel 自动包装 | 🟡 中 — 异步任务中 userId/traceId 丢失 | 集成测试覆盖异步场景；备用方案为手动添加 `TaskDecorator` |
| `Span.current().getSpanContext().getTraceId()` 在非请求线程中返回无效值 | 🟢 低 — traceId 为全零 | 仅在 OTel Context 存在时返回有效值；无 Span 时返回空字符串（与当前 ScopedThreadContext 的 null 行为兼容） |
| Jaeger v2 容器在无 Docker 环境下不可用 | 🟢 低 — 仅影响开发体验 | 生产环境使用外部 Jaeger 部署；无 Docker 时 OTel 数据发送失败但应用正常运行 |
| `BaseResult.traceId` 值格式变更（UUID → OTel hex） | 🟢 低 — 前端如果强校验 traceId 格式会受影响 | traceId 字段是 String 类型，前端通常不做格式校验；API 文档中说明格式变更 |
| Sa-Token 拦截器顺序与 OTel Filter 冲突 | 🟢 低 — Sa-Token 拦截器在 DispatcherServlet 层，OTel Filter 在 Servlet Filter 层 | OTel Filter 优先级更高（更早执行），Sa-Token 在内层正常工作 |
| OTel SDK 与 Java 25 ScopedValue 兼容性 | 🟢 低 — OTel SDK 使用 ThreadLocal，与 ScopedValue 无冲突 | OTel 上下文传播不依赖 ScopedValue |

## Migration Plan

### 阶段划分

```
Phase 1: 基础设施（无破坏性）
  添加依赖、创建 Testcontainers 配置、添加 YAML 配置
  → 应用可正常启动，Jaeger UI 可看到自动链路
  → 此阶段新旧代码共存，旧代码仍使用 ScopedThreadContext

Phase 2: 上下文迁移（核心重构）
  创建 UserContext → 重写 ContextFillFilter → 修改所有消费点
  → 逐文件替换 ScopedThreadContext 调用为 OTel API 调用

Phase 3: 指标清理
  移除 LogAspect 的 Micrometer 代码 → 更新 LoggingConfigure
  → 移除显式 micrometer-core 依赖

Phase 4: 旧代码清理
  删除 ScopedThreadContext、ContextRunnable、ContextCallable
  删除/重写相关测试
  → 项目完全基于 OTel

Phase 5: 集成验证
  端到端测试：HTTP 请求 → Jaeger UI 看到完整链路
  日志验证：logback 输出包含有效 traceId
  异步验证：线程池任务中上下文正确传播
```

### 回滚策略

每个 Phase 独立提交 Git commit。如果某个 Phase 出现问题，可以 `git revert` 到上一个 Phase 的状态。Phase 1 完成后应用完全可用（新旧共存），Phase 2-4 是渐进式替换。

## Open Questions

1. **Spring Boot 4.0.2 的 `spring-boot-starter-opentelemetry` 具体版本号**：需要确认 Spring Boot 4.0.2 BOM 中包含的 OTel SDK 版本，以及该版本是否支持 Logs 信号导出（Logs 信号在 OTel Java SDK 中较新）
2. **自定义 Executor Bean 的 OTel 自动包装**：需要在 Phase 1 完成后编写集成测试验证。如果未自动包装，需要在 `ThreadPoolConfigure` 中添加 `TaskDecorator`
3. **OTel Logs 导出的 MDC key 名称**：需要确认 Spring Boot 4 Micrometer Tracing bridge 的确切 MDC key 映射（`traceId` vs `trace_id`）
