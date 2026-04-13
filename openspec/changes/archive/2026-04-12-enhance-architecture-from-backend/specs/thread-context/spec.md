## ADDED Requirements

### Requirement: ScopedThreadContext 上下文传递
系统 SHALL 提供 ScopedThreadContext 工具类，基于 Java ScopedValue 实现，用于在请求生命周期内传递线程安全的上下文信息。

ScopedThreadContext SHALL 支持以下上下文属性：
- userId（当前用户标识）
- traceId（请求追踪标识）
- 自定义属性扩展

ScopedThreadContext SHALL 提供 `runWithContext(Runnable, String userId, String traceId)` 方法启动上下文作用域。
ScopedThreadContext SHALL 提供静态方法获取当前上下文属性：`getUserId()`、`getTraceId()`。

#### Scenario: Filter 设置请求上下文
- **WHEN** HTTP 请求进入 ContextFillFilter
- **THEN** Filter 从请求头提取 traceId（或生成新的），通过 ScopedThreadContext.runWithContext() 设置上下文后执行后续 FilterChain

#### Scenario: 获取当前用户ID
- **WHEN** Service 层需要获取当前操作用户
- **THEN** 调用 ScopedThreadContext.getUserId() 获取当前请求关联的 userId

#### Scenario: 上下文自动清理
- **WHEN** 请求处理完成（ScopedValue 作用域结束）
- **THEN** 上下文信息自动清理，无需手动 remove

### Requirement: ContextFillFilter
系统 SHALL 提供 ContextFillFilter（继承 OncePerRequestFilter），为每个 HTTP 请求初始化上下文。

ContextFillFilter SHALL：
- 从请求头 `X-Trace-Id` 获取 traceId，若不存在则生成新的 UUID
- 从请求头 `X-User-Id` 获取 userId（可选）
- 将 traceId 写入响应头 `X-Trace-Id`
- 使用 ScopedThreadContext.runWithContext() 包装后续处理

#### Scenario: 请求携带 traceId
- **WHEN** HTTP 请求头包含 `X-Trace-Id: abc-123`
- **THEN** ContextFillFilter 使用该 traceId，并在响应头中原样返回

#### Scenario: 请求不携带 traceId
- **WHEN** HTTP 请求头不包含 `X-Trace-Id`
- **THEN** ContextFillFilter 生成新的 UUID 作为 traceId，写入响应头

### Requirement: BaseResult 自动填充 traceId
BaseResult 的 success() 和 fail() 静态工厂方法 SHALL 自动从 ScopedThreadContext 获取 traceId 填充到响应中。

#### Scenario: 成功响应自动填充 traceId
- **WHEN** Controller 调用 BaseResult.success(data)
- **THEN** 返回的 BaseResult 中 traceId 字段自动从 ScopedThreadContext 获取

### Requirement: 跨线程上下文传递
ScopedThreadContext SHALL 提供 ContextRunnable 和 ContextCallable 包装器，支持异步场景下的上下文传递。

#### Scenario: 异步任务传递上下文
- **WHEN** 使用 @Async 或线程池执行异步任务
- **THEN** 通过 ContextRunnable 包装 Runnable，在异步线程中仍可获取 userId 和 traceId
