## Context

本项目（`web-quick-start-light`）是一个 Spring Boot 4.x + Java 25 的多模块四层架构脚手架。在前一阶段的 OTel 集成工作中（变更 `integrate-opentelemetry-jaeger`），引入了 OTel Context 作为可观测性上下文载体，同时保留了基于 `ScopedValue` 的业务上下文机制 `ScopedThreadContext`。

当前存在以下架构问题：

### 三套上下文体系

| 体系 | 载体 | 内容 | 跨线程传播 |
|------|------|------|-----------|
| OTel Context | `io.opentelemetry.context.Context` | Span（traceId/spanId）+ Baggage（userId） | `Context.current().wrap(r)` |
| 业务上下文 | `ScopedValue<Holder>`（Java 25） | `EnumMap<Key, String>`（userId + traceId） | `ContextRunnable`/`ContextCallable` |
| MDC | `ThreadLocal<Map>` | traceId、spanId（OTel 自动注入） | 手动快照恢复 |

### 已发现的问题

1. **`Key.TRACE_ID` 是死字段**：没有任何代码通过 `ScopedThreadContext.getTraceId()` 读取。所有 traceId 消费者（`BaseResult`/`LogAspect`/`OperationLogFacadeImpl`）直接用 `Span.current()`，`SlowQueryInterceptor` 用 `MDC.get("traceId")`。
2. **两条异步传播路径不一致**：
   - 路径 A：`ThreadPoolConfigure` 的 `TaskDecorator` = `Context.current().wrap(r)` → 只传播 OTel Context
   - 路径 B：`new ContextRunnable(r)` → 三合一传播（ScopedValue + OTel + MDC）
   - 如果 `MyMetaObjectHandler` 在路径 A 的线程中运行，`ScopedThreadContext.getUserId()` 返回 `null`
3. **`schedulerThreadPool` 的 ThreadFactory 有 bug**：`ThreadFactory.newThread()` 只在线程创建时调用一次，后续复用的线程不会再经过 ThreadFactory，导致上下文传播失效
4. **命名暴露实现细节**：`ScopedThreadContext` 暴露了 ScopedValue 实现细节

### 约束

- Java 25，使用 `ScopedValue`（非 `ThreadLocal`）作为业务上下文载体
- Micrometer Context Propagation 库不支持 `ScopedValue`（issue #108 open），不能使用 Spring 官方的 `ContextPropagatingTaskDecorator`
- Spring Boot 4.0.2，OTel 通过 `spring-boot-starter-opentelemetry` 集成
- 测试规范：单元测试 `*UTest`，集成测试 `*ITest`，禁止 mock 替代

## Goals / Non-Goals

**Goals:**

1. 将 `ScopedThreadContext` 重命名为 `BizContext`，清晰表达"业务上下文"的职责
2. 移除 `Key.TRACE_ID` 死字段，traceId 由 OTel Span 全权负责
3. 将三合一上下文传播逻辑（ScopedValue + OTel Context + MDC）融合到 `TaskDecorator`
4. 删除 `ContextRunnable.java` 和 `ContextCallable.java`
5. 统一 `ThreadPoolConfigure` 中所有 Executor 的上下文传播机制
6. 修复 `schedulerThreadPool` 的上下文传播 bug
7. 保持 488+ 测试全部通过（零回归）

**Non-Goals:**

- 不改变 OTel 的配置方式（OTLP endpoint、采样率等）
- 不改变 MDC 的 logback 配置（`%X{traceId:--}` 模式保持不变）
- 不改变 OTel Baggage 的传播协议（W3C Baggage Header）
- 不迁移到 `ThreadLocal` + Micrometer `ThreadLocalAccessor` 体系（保持 `ScopedValue`，等 Micrometer 原生支持后再评估）
- 不改变 HTTP API 契约（`X-Trace-Id` 响应头、`BaseResult.traceId` 字段不变）
- 不处理 OTel + Jaeger 的集成验证（属于变更 `integrate-opentelemetry-jaeger` Chunk 5 的范围）

## Decisions

### Decision 1：保持 ScopedValue，手写 TaskDecorator

**选择**：方案 B — 保持 `ScopedValue` + 手写三合一 `TaskDecorator`

**理由**：
- Micrometer Context Propagation 不支持 `ScopedValue`（issue #108 open）
- `ScopedValue` 的不可变绑定、请求结束自动清理、无泄漏风险是设计优势
- 手写 TaskDecorator 的逻辑与当前 `ContextRunnable.run()` 完全一致，无额外复杂度

**备选**：方案 A — 迁移到 `ThreadLocal` + Micrometer `ContextPropagatingTaskDecorator`
- 放弃 `ScopedValue` 的安全性优势
- 需要为每个业务上下文键实现 `ThreadLocalAccessor` 并 SPI 注册
- 等 Micrometer 支持 ScopedValue 后还要迁移回来
- **不选择**

### Decision 2：BizContext 只保留纯业务键

**选择**：移除 `Key.TRACE_ID`，`BizContext` 只包含纯业务数据（userId、未来可能加的 tenantId 等）

**理由**：
- traceId 的唯一真实来源是 OTel Span（`Span.current().getSpanContext().getTraceId()`）
- 在 BizContext 中存一份 traceId 是冗余副本，且从未被读取
- 三种上下文的职责边界更清晰：OTel 管可观测性、BizContext 管业务、MDC 管日志输出

**影响**：
- `ContextFillFilter` 的 `runWithContext()` 调用从 `(action, userId, traceId)` 简化为 `(action, userId)`
- `buildBaggage()` 中不再需要跳过 `propagated=false` 的 TRACE_ID

### Decision 3：TaskDecorator 融合三合一传播

**选择**：将 `ContextRunnable.run()` 的三合一逻辑内聚到 `ThreadPoolConfigure` 的 `TaskDecorator`

**实现**：
```java
// TaskDecorator.decorate() 在调用线程执行 → 捕获上下文
// 返回的 Runnable 在工作线程执行 → 恢复上下文
private static final TaskDecorator CONTEXT_PROPAGATING_DECORATOR = runnable -> {
    BizContext.Holder replica = BizContext.copyAsReplica();
    ScopedValue<BizContext.Holder> scoped = BizContext.getScoped();
    io.opentelemetry.context.Context otelContext = io.opentelemetry.context.Context.current();
    Map<String, String> mdcContext = MDC.getCopyOfContextMap();

    return () -> {
        Map<String, String> previousMdc = MDC.getCopyOfContextMap();
        if (mdcContext != null) MDC.setContextMap(mdcContext);
        try {
            Runnable task = replica != null
                ? () -> ScopedValue.where(scoped, replica).run(runnable)
                : runnable;
            otelContext.wrap(task).run();
        } finally {
            if (previousMdc != null) MDC.setContextMap(previousMdc);
            else MDC.clear();
        }
    };
};
```

**理由**：
- 使用者只需 `executor.execute(plainRunnable)` 即可自动获得三合一传播
- 消灭 `ContextRunnable`/`ContextCallable` 两个类
- 与当前 `ContextRunnable.run()` 的逻辑完全一致，无行为变更

### Decision 4：schedulerThreadPool 包装方案

**选择**：使用装饰器模式包装 `ScheduledExecutorService`，覆盖 `execute`/`submit`/`schedule`/`scheduleAtFixedRate`/`scheduleWithFixedDelay` 方法

**理由**：
- `ScheduledExecutorService` 没有 `TaskDecorator` 机制
- `ThreadFactory` 只在线程创建时调用一次，无法传播上下文（当前代码有 bug）
- 装饰器模式在每次任务提交时捕获调用线程上下文，在工作线程中恢复
- 实现为一个内部静态类 `ContextPropagatingScheduledExecutorService`

**不选择**：
- `ThreadPoolTaskScheduler`（Spring 提供）：虽然支持 `TaskDecorator`，但需要引入额外依赖和配置变更

### Decision 5：BizContext 类结构

**选择**：保留 `Holder` 内部类 + `Key` 枚举 + `replica` 标记的设计

**理由**：
- Holder 封装 EnumMap + replica 标记，是 TaskDecorator 传播的核心数据结构
- Key 枚举提供类型安全的上下文访问
- replica=true 时 `Key.set()` 仅修改 EnumMap 不写 Baggage，防止子线程污染请求线程的 Baggage

## Risks / Trade-offs

### [Risk] ScopedValue 与 Micrometer 生态不兼容
→ **Mitigation**：手写 TaskDecorator 是自包含的，不依赖 Micrometer Context Propagation。未来 Micrometer 支持 ScopedValue 后，可以迁移到 `ContextPropagatingTaskDecorator`，迁移路径清晰（只需实现 `ThreadLocalAccessor`）。

### [Risk] 重命名导致大范围 import 变更
→ **Mitigation**：这是纯机械替换，IDE 批量重构可完成。影响范围限于 `app` 模块内的 11 个文件 + 对应测试。

### [Risk] ScheduledExecutorService 装饰器可能遗漏方法
→ **Mitigation**：只包装 5 个核心方法（`execute`/`submit`/`schedule`/`scheduleAtFixedRate`/`scheduleWithFixedDelay`），覆盖所有任务提交入口。其他方法如 `shutdown()`/`invokeAll()` 不涉及上下文传播。

### [Risk] TaskDecorator 性能开销
→ **Mitigation**：每次任务提交多一次上下文快照（EnumMap 复制 + MDC 复制 + OTel Context 捕获），开销极小（< 1μs）。Spring 官方文档对 `ContextPropagatingTaskDecorator` 有同样的性能警告，但认为对于非高频小任务场景可以接受。

## Migration Plan

### 执行顺序（Chunk 化）

1. **Chunk 1**：核心重命名 + API 简化
   - `ScopedThreadContext.java` → `BizContext.java`（移除 TRACE_ID，简化 runWithContext）
   - 更新 `ContextFillFilter`、`MyMetaObjectHandler`
   - 更新单元测试

2. **Chunk 2**：TaskDecorator 融合
   - 重写 `ThreadPoolConfigure` 的 TaskDecorator 为三合一版本
   - 添加 `ContextPropagatingScheduledExecutorService` 内部类
   - 删除 `ContextRunnable.java`、`ContextCallable.java`
   - 删除对应的单元测试，创建新的 TaskDecorator 测试

3. **Chunk 3**：全量验证
   - `mvn test -pl app` 全量通过
   - 验证异步场景中 BizContext / OTel Span / MDC 三种上下文均可用

### 回滚策略

- 变更仅涉及 `app` 模块内部类重命名和删除，无数据变更
- Git revert 即可回滚
- 不影响 HTTP API 契约

## Open Questions

无。所有设计决策已在讨论中确认。
