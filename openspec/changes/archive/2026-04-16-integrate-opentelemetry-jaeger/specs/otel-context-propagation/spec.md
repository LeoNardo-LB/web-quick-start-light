## ADDED Requirements

### Requirement: OTel W3C Trace Context 替代自研 UUID traceId

系统 SHALL 使用 OpenTelemetry 的 W3C Trace Context 标准生成和传播 traceId，替代自研的 UUID + X-Trace-Id Header + ScopedValue 机制。traceId SHALL 由 OTel SDK 自动生成（32 字符 hex 格式），通过 W3C `traceparent` Header 自动传播。

#### Scenario: HTTP 请求自动获得 OTel traceId

- **WHEN** 应用接收到一个 HTTP 请求
- **THEN** OTel SDK SHALL 自动为该请求创建 Root Span 并生成 W3C 标准 traceId
- **AND** 该 traceId SHALL 通过 `Span.current().getSpanContext().getTraceId()` 可获取
- **AND** 该 traceId SHALL 自动写入 MDC 的 `traceId` key（由 Micrometer Tracing bridge 映射）

#### Scenario: X-Trace-Id 响应头回传 OTel traceId

- **WHEN** 应用处理一个 HTTP 请求
- **THEN** 响应头 `X-Trace-Id` SHALL 包含 OTel 生成的 traceId 值（从当前 Span 读取）
- **AND** 该值 SHALL 为 32 字符小写 hex 字符串（OTel 标准 traceId 格式）

#### Scenario: 非请求线程中获取 traceId

- **WHEN** 在没有 OTel Span 上下文的线程中调用 `Span.current().getSpanContext().getTraceId()`
- **THEN** SHALL 返回空字符串 `""`（OTel 的 InvalidSpanContext）
- **AND** 不 SHALL 抛出异常

### Requirement: userId 通过 OTel Baggage 传播替代 ScopedValue

系统 SHALL 使用 OTel Baggage API 传播 userId，替代 `ScopedThreadContext` 基于 Java 25 ScopedValue 的传播机制。

#### Scenario: Filter 中注入 userId 到 Baggage

- **WHEN** HTTP 请求经过上下文填充 Filter
- **THEN** Filter SHALL 从 `AuthComponent.getCurrentUserId()` 获取 userId
- **AND** 将 userId 注入到 OTel Baggage 中（key 为 `"userId"`）
- **AND** userId 为 null 时 SHALL 使用 `"ANONYMOUS"` 作为默认值
- **AND** 无 AuthComponent 时 SHALL 使用 `"SYSTEM"` 作为默认值

#### Scenario: 业务代码获取 userId

- **WHEN** 业务代码调用 `UserContext.getUserId()`
- **THEN** SHALL 从 OTel Baggage 中读取 `"userId"` 值
- **AND** Baggage 中无 userId 时 SHALL 返回 null

#### Scenario: userId 在异步线程中自动传播

- **WHEN** 使用项目自定义的 Executor Bean（ioThreadPool / cpuThreadPool / daemonThreadPool / schedulerThreadPool）提交任务
- **THEN** OTel Baggage 中的 userId SHALL 自动传播到异步线程中
- **AND** 异步线程中 `UserContext.getUserId()` SHALL 返回与父线程相同的值

### Requirement: UserContext 工具类

系统 SHALL 提供 `UserContext` 工具类（位于 `org.smm.archetype.shared.util.context` 包），封装 OTel Baggage 的 userId 读写操作。

#### Scenario: UserContext API 签名

- **WHEN** 开发者需要获取或设置 userId
- **THEN** SHALL 通过 `UserContext.getUserId()` 静态方法获取（返回 `String`，可能为 null）
- **AND** SHALL 通过 `UserContext.setUserId(String)` 静态方法设置（仅在 Filter 中调用）

### Requirement: 删除 ScopedThreadContext 及其异步传播组件

系统 SHALL 删除以下文件及其所有引用：
- `ScopedThreadContext.java`
- `ContextRunnable.java`
- `ContextCallable.java`

#### Scenario: 编译时无 ScopedThreadContext 引用

- **WHEN** 项目编译
- **THEN** SHALL 不存在对 `ScopedThreadContext`、`ContextRunnable`、`ContextCallable` 的任何 import 或使用
- **AND** 上述三个文件 SHALL 不存在于源码目录中

### Requirement: BaseResult traceId 来源迁移到 OTel Span

`BaseResult` 和 `BasePageResult` 的 `traceId` 字段 SHALL 从 OTel 当前 Span 读取，替代 `ScopedThreadContext.getTraceId()`。

#### Scenario: BaseResult.success() 填充 OTel traceId

- **WHEN** `BaseResult.success(data)` 被调用
- **THEN** `traceId` 字段 SHALL 设置为 `Span.current().getSpanContext().getTraceId()` 的值

#### Scenario: BaseResult.fail() 填充 OTel traceId

- **WHEN** `BaseResult.fail(errorCode, message)` 被调用
- **THEN** `traceId` 字段 SHALL 设置为 `Span.current().getSpanContext().getTraceId()` 的值

#### Scenario: BasePageResult.fromPage() 填充 OTel traceId

- **WHEN** `BasePageResult.fromPage(page)` 被调用
- **THEN** `traceId` 字段 SHALL 设置为 `Span.current().getSpanContext().getTraceId()` 的值

### Requirement: ContextFillFilter 重写为 OTel 上下文桥接

`ContextFillFilter` SHALL 重写为轻量级的 OTel 上下文桥接器，职责限定为：将 userId 注入 Baggage + 将 OTel traceId 写入响应头。

#### Scenario: Filter 不再生成 traceId

- **WHEN** HTTP 请求到达 Filter
- **THEN** Filter SHALL NOT 自行生成 UUID traceId
- **AND** Filter SHALL NOT 读取 `X-Trace-Id` 请求头来决定 traceId
- **AND** traceId SHALL 由 OTel SDK 自动生成

#### Scenario: Filter 不再使用 ScopedValue

- **WHEN** Filter 处理请求
- **THEN** Filter SHALL NOT 调用 `ScopedThreadContext.runWithContext()`
- **AND** Filter SHALL NOT 使用 `ScopedValue` API

#### Scenario: Filter 执行顺序

- **WHEN** 应用启动
- **THEN** 重写后的 Filter（可能重命名为 `OtelContextFilter` 或保持 `ContextFillFilter`） SHALL 通过 `FilterRegistrationBean` 注册
- **AND** order SHALL 保证在 OTel Servlet Filter 之后执行（以便 OTel Span 已创建）

### Requirement: MyMetaObjectHandler userId 来源迁移

`MyMetaObjectHandler` 的 `createUser` / `updateUser` 自动填充 SHALL 从 `UserContext.getUserId()` 获取 userId，替代 `ScopedThreadContext.getUserId()`。

#### Scenario: 自动填充 createUser/updateUser

- **WHEN** MyBatis-Plus 执行 INSERT 操作
- **THEN** `createUser` 字段 SHALL 设置为 `UserContext.getUserId()` 的值
- **AND** userId 为 null 时 SHALL 使用 `"system"` 作为默认值（保持当前行为）

### Requirement: SlowQueryInterceptor 通过 OTel MDC 获取 traceId

`SlowQueryInterceptor` 中 `MDC.get("traceId")` SHALL 通过 OTel 自动 MDC 注入获得有效 traceId，无需代码改动（或仅做微调）。

#### Scenario: 慢 SQL 日志包含有效 traceId

- **WHEN** SQL 执行时间超过阈值且触发慢 SQL 日志
- **THEN** 日志中的 `traceId=` SHALL 包含当前请求的 OTel traceId 值（非 null、非空）
- **AND** 该 traceId SHALL 与同一请求的 HTTP Span traceId 一致

### Requirement: 自定义 Executor Bean 支持 OTel 上下文传播

项目的自定义 `Executor` Bean（ioThreadPool、cpuThreadPool、daemonThreadPool、schedulerThreadPool）SHALL 支持 OTel Context（含 traceId 和 Baggage）的自动传播。

#### Scenario: Executor 任务中 OTel Context 可用

- **WHEN** 通过 `ioThreadPool` 或 `cpuThreadPool` 或 `daemonThreadPool` 提交 Runnable/Callable
- **THEN** 任务执行时 `Span.current()` SHALL 可访问（非 InvalidSpan）
- **AND** `UserContext.getUserId()` SHALL 返回与提交线程相同的值
