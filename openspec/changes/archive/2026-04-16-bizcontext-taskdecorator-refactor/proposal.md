## Why

当前项目中三套上下文体系（OTel Context / ScopedValue 业务上下文 / MDC）的传播机制存在架构混乱：
1. **`ScopedThreadContext` 包含死字段**：`Key.TRACE_ID` 被写入但从未被读取——所有 traceId 消费者直接使用 `Span.current()` 或 `MDC.get("traceId")`，导致职责边界模糊。
2. **两条异步传播路径不一致**：`ThreadPoolConfigure` 的 `TaskDecorator` 只传播 OTel Context，而 `ContextRunnable`/`ContextCallable` 做三合一传播（ScopedValue + OTel + MDC）。开发者必须知道何时用哪个 Executor，否则 `MyMetaObjectHandler` 等依赖 `ScopedValue` 的代码在异步线程中会拿到 `null`。
3. **命名暴露实现细节**：`ScopedThreadContext` 这个名字暴露了 Java 25 ScopedValue 实现细节，不如 `BizContext` 直观表达"业务上下文"的职责。
4. **`ContextRunnable`/`ContextCallable` 应该被消灭**：三合一传播逻辑应该内聚到 `TaskDecorator` 中，使用者只需提交普通 Runnable/Callable 即可自动获得全部上下文传播。

## What Changes

- **BREAKING**：将 `ScopedThreadContext` 重命名为 `BizContext`，包路径不变（`shared.util.context`）
- **BREAKING**：移除 `Key.TRACE_ID` 死字段及相关参数（`runWithContext` 不再接受 traceId 参数）
- **BREAKING**：删除 `ContextRunnable.java` 和 `ContextCallable.java`，三合一传播逻辑融合到 `TaskDecorator`
- 修改 `ThreadPoolConfigure`：将 `OTEL_CONTEXT_DECORATOR` 替换为三合一 `CONTEXT_PROPAGATING_DECORATOR`
- 修改 `ContextFillFilter`：不再传 traceId 给 `BizContext.runWithContext()`，只传 userId
- 修改 `MyMetaObjectHandler`：`ScopedThreadContext.getUserId()` → `BizContext.getUserId()`
- 修改 `schedulerThreadPool`：修复 ThreadFactory 上下文传播 bug（改为包装 `ScheduledExecutorService` 的 `execute/submit/schedule` 方法）
- 更新所有引用 `ScopedThreadContext` 的测试文件

## Capabilities

### New Capabilities

- `bizcontext-propagation`：业务上下文传播——定义 `BizContext` 的 API 契约、`Key` 枚举扩展规则、`Holder` 副本机制、Baggage 自动同步规则
- `taskdecorator-unified`：统一 TaskDecorator——定义三合一传播机制（ScopedValue + OTel Context + MDC），消灭 `ContextRunnable`/`ContextCallable`，统一 `ThreadPoolConfigure` 和 `ScheduledExecutorService` 的上下文传播

### Modified Capabilities

（无——现有 specs 不涉及此层次的上下文传播架构）

## Impact

### 源码文件（修改）

| 文件 | 变更类型 |
|------|----------|
| `app/.../shared/util/context/ScopedThreadContext.java` | 重命名 → `BizContext.java`，移除 `TRACE_ID`，简化 API |
| `app/.../shared/util/context/ContextRunnable.java` | 删除 |
| `app/.../shared/util/context/ContextCallable.java` | 删除 |
| `app/.../controller/global/ContextFillFilter.java` | 改用 `BizContext.runWithContext(action, userId)` |
| `app/.../shared/util/dal/MyMetaObjectHandler.java` | 改用 `BizContext.getUserId()` |
| `app/.../config/ThreadPoolConfigure.java` | 三合一 TaskDecorator + 修复 schedulerThreadPool |

### 测试文件（修改）

| 文件 | 变更类型 |
|------|----------|
| `ScopedThreadContextUTest.java` | 重命名 + 重写 |
| `ContextRunnableUTest.java` | 删除（逻辑迁移到 TaskDecorator 测试） |
| `ContextCallableUTest.java` | 删除（逻辑迁移到 TaskDecorator 测试） |
| `ContextFillFilterUTest.java` | 适配 BizContext 新 API |
| `MyMetaObjectHandlerUTest.java` | 适配 BizContext 新 API |
| 其他引用 `ScopedThreadContext` 的测试 | 批量替换 import |

### API 影响

- 无 HTTP API 变更
- 无数据库 Schema 变更
- 内部 Java API 变更（`ScopedThreadContext` → `BizContext`，签名变化）
