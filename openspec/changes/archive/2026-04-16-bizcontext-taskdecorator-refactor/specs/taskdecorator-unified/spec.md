## ADDED Requirements

### Requirement: 三合一 TaskDecorator

`ThreadPoolConfigure` SHALL 使用一个三合一 `TaskDecorator`（名为 `CONTEXT_PROPAGATING_DECORATOR`）替代当前的 `OTEL_CONTEXT_DECORATOR`。

该 TaskDecorator SHALL 在 `decorate(Runnable)` 方法中同时捕获调用线程的三种上下文：
1. **BizContext**（ScopedValue）：通过 `BizContext.copyAsReplica()` 获取 Holder 副本
2. **OTel Context**：通过 `io.opentelemetry.context.Context.current()` 捕获
3. **MDC**：通过 `MDC.getCopyOfContextMap()` 捕获

在工作线程中执行时 SHALL 按以下顺序恢复上下文：
1. 恢复 MDC（`MDC.setContextMap`）
2. 恢复 ScopedValue（`ScopedValue.where(scoped, replica).run(task)`）
3. 恢复 OTel Context（`otelContext.wrap(task).run()`）

执行完毕后 SHALL 恢复工作线程原有的 MDC 状态。

#### Scenario: TaskDecorator 传播 BizContext
- **WHEN** 在绑定了 `BizContext.runWithContext(action, "user123")` 的线程中通过 ThreadPoolConfigure 的 Executor 提交普通 Runnable
- **THEN** 在 Runnable 执行时 `BizContext.getUserId()` 返回 `"user123"`

#### Scenario: TaskDecorator 传播 OTel Context
- **WHEN** 在有 OTel Span 的请求线程中通过 Executor 提交 Runnable
- **THEN** 在 Runnable 执行时 `Span.current().getSpanContext().getTraceId()` 返回与请求线程相同的 traceId

#### Scenario: TaskDecorator 传播 MDC
- **WHEN** 在 MDC 包含 `traceId` 的请求线程中通过 Executor 提交 Runnable
- **THEN** 在 Runnable 执行时 `MDC.get("traceId")` 返回与请求线程相同的 traceId

#### Scenario: TaskDecorator 在子线程中不污染 Baggage
- **WHEN** 在子线程中调用 `BizContext.Key.USER_ID.set("childUser")`
- **THEN** 子线程的 EnumMap 副本被修改，但请求线程的 OTel Baggage 不受影响

#### Scenario: MDC 执行后恢复
- **WHEN** Runnable 执行完毕
- **THEN** 工作线程的 MDC 恢复为执行前的状态（或清空）

---

### Requirement: 删除 ContextRunnable 和 ContextCallable

`ContextRunnable.java` 和 `ContextCallable.java` SHALL 从代码库中删除。

对应的测试文件 `ContextRunnableUTest.java` 和 `ContextCallableUTest.java` SHALL 也被删除。

三合一传播逻辑完全由 TaskDecorator 承担，不再需要手动包装。

#### Scenario: 文件不存在验证
- **WHEN** 检查文件系统
- **THEN** `ContextRunnable.java`、`ContextCallable.java`、`ContextRunnableUTest.java`、`ContextCallableUTest.java` 均不存在

#### Scenario: 使用者无需手动包装
- **WHEN** 业务代码通过 `@Qualifier("ioThreadPool") Executor` 提交普通 Runnable
- **THEN** 三种上下文（BizContext + OTel + MDC）自动传播，无需 `new ContextRunnable(r)`

---

### Requirement: ScheduledExecutorService 上下文传播

`schedulerThreadPool` Bean SHALL 返回一个装饰过的 `ScheduledExecutorService`，在每次任务提交时传播三种上下文。

装饰器 SHALL 覆盖以下 5 个方法：`execute`、`submit(Callable)`、`submit(Runnable)`、`schedule`、`scheduleAtFixedRate`、`scheduleWithFixedDelay`。

装饰器 SHALL NOT 覆盖生命周期方法（`shutdown`、`shutdownNow`、`isShutdown`、`isTerminated`、`awaitTermination`）。

#### Scenario: schedule 传播上下文
- **WHEN** 在绑定了 BizContext 的线程中调用 `schedulerThreadPool.schedule(runnable, 100, MILLISECONDS)`
- **THEN** 在 Runnable 执行时 `BizContext.getUserId()` 返回与提交线程相同的值

#### Scenario: scheduleAtFixedRate 传播上下文
- **WHEN** 在绑定了 BizContext 的线程中调用 `schedulerThreadPool.scheduleAtFixedRate(runnable, 0, 1, SECONDS)`
- **THEN** 在每次执行时 `BizContext.getUserId()` 返回与提交线程相同的值

---

### Requirement: ThreadPoolConfigure 统一使用三合一 TaskDecorator

`ThreadPoolConfigure` 中的 `ioThreadPool`、`cpuThreadPool`、`daemonThreadPool` Bean SHALL 全部使用 `CONTEXT_PROPAGATING_DECORATOR`。

`schedulerThreadPool` Bean SHALL 使用 `ContextPropagatingScheduledExecutorService` 包装。

`OTEL_CONTEXT_DECORATOR` 常量 SHALL 被删除，替换为 `CONTEXT_PROPAGATING_DECORATOR`。

`schedulerThreadPool` 的 ThreadFactory 中 SHALL NOT 再包含 `Context.current().wrap(r)` 逻辑。

#### Scenario: 所有 Executor Bean 使用统一传播
- **WHEN** 通过 `ioThreadPool`、`cpuThreadPool`、`daemonThreadPool` 中任意一个提交任务
- **THEN** 三种上下文（BizContext + OTel + MDC）均被传播

---

### Requirement: TaskDecorator 集成测试

SHALL 创建 `TaskDecoratorContextPropagationITest`（位于 `app/src/test/java/.../cases/integrationtest/`）验证三合一传播。

#### Scenario: 端到端验证异步上下文
- **WHEN** HTTP 请求触发一个异步任务（通过 `ioThreadPool` 提交）
- **THEN** 异步任务中 `BizContext.getUserId()` 非空，`Span.current().getSpanContext().getTraceId()` 为 32 字符 hex，`MDC.get("traceId")` 非空
