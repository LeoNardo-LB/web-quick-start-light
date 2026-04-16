## ADDED Requirements

### Requirement: BizContext API 契约

系统 SHALL 提供 `BizContext` 类（位于 `org.smm.archetype.shared.util.context` 包）作为唯一的业务上下文访问入口。

`BizContext` SHALL 使用 `Java 25 ScopedValue<Holder>` 作为底层载体，提供请求作用域内的不可变绑定。

`BizContext` SHALL 通过 `Key` 枚举定义上下文属性。当前 ONLY 包含：
- `USER_ID`（baggageKey="userId", propagated=true）— 跨服务传播

`BizContext` SHALL NOT 包含 `TRACE_ID` 键。traceId 的唯一来源是 `io.opentelemetry.api.trace.Span.current().getSpanContext().getTraceId()`。

#### Scenario: 请求线程中读写 userId
- **WHEN** 在 `BizContext.runWithContext(action, userId)` 的 action 内调用 `BizContext.getUserId()`
- **THEN** 返回传入的 userId 字符串

#### Scenario: 未绑定时读取返回 null
- **WHEN** 在未绑定 BizContext 的上下文中调用 `BizContext.getUserId()`
- **THEN** 返回 `null`

#### Scenario: Key.set 在请求线程中自动同步 Baggage
- **WHEN** 在请求线程（replica=false）中调用 `BizContext.Key.USER_ID.set("newUser")`
- **THEN** OTel Baggage 中的 `userId` 键更新为 `"newUser"`

#### Scenario: Key.set 在子线程中不同步 Baggage
- **WHEN** 在子线程（replica=true，通过 TaskDecorator 传播的 Holder）中调用 `BizContext.Key.USER_ID.set("childUser")`
- **THEN** 仅修改 EnumMap 副本，OTel Baggage 中的 `userId` 保持不变

#### Scenario: runWithContext 只接收 userId
- **WHEN** 调用 `BizContext.runWithContext(action, userId)`
- **THEN** action 内 `BizContext.getUserId()` 返回 userId，`Span.current().getSpanContext().getTraceId()` 返回有效的 OTel traceId

---

### Requirement: Holder 副本机制

`BizContext` SHALL 提供 `Holder` 内部类，包装 `EnumMap<Key, String>` 和 `boolean replica` 标记。

`BizContext` SHALL 提供 `copyAsReplica()` 方法，创建当前 Holder 的深拷贝（replica=true），供 TaskDecorator 内部使用。

`BizContext` SHALL 提供 `getScoped()` 方法，暴露底层 `ScopedValue<Holder>` 供 TaskDecorator 内部使用。

`copyAsReplica()` 和 `getScoped()` SHALL 视为内部 API，不鼓励业务代码直接调用。

#### Scenario: copyAsReplica 创建深拷贝
- **WHEN** 在绑定了 BizContext 的线程中调用 `BizContext.copyAsReplica()`
- **THEN** 返回新的 Holder，其 map 是原始 map 的深拷贝，replica=true

#### Scenario: 未绑定时 copyAsReplica 返回 null
- **WHEN** 在未绑定 BizContext 的线程中调用 `BizContext.copyAsReplica()`
- **THEN** 返回 `null`

---

### Requirement: 便捷方法

`BizContext` SHALL 提供静态便捷方法 `getUserId()`，等价于 `Key.USER_ID.get()`。

#### Scenario: getUserId 在有上下文时返回值
- **WHEN** 在 `runWithContext(action, "user123")` 内调用 `BizContext.getUserId()`
- **THEN** 返回 `"user123"`

#### Scenario: getUserId 在无上下文时返回 null
- **WHEN** 在未绑定 BizContext 的线程中调用 `BizContext.getUserId()`
- **THEN** 返回 `null`

---

### Requirement: 文件位置和命名

`BizContext.java` SHALL 位于 `app/src/main/java/org/smm/archetype/shared/util/context/BizContext.java`。

原文件 `ScopedThreadContext.java` SHALL 被删除（重命名，不是新建副本）。

#### Scenario: 文件存在性验证
- **WHEN** 检查文件系统
- **THEN** `BizContext.java` 存在且 `ScopedThreadContext.java` 不存在

---

### Requirement: ContextFillFilter 使用 BizContext

`ContextFillFilter` SHALL 使用 `BizContext.runWithContext(action, userId)` 绑定上下文，不再传递 traceId 参数。

#### Scenario: Filter 只传 userId 不传 traceId
- **WHEN** 请求进入 ContextFillFilter
- **THEN** 调用 `BizContext.runWithContext(() -> filterChain.doFilter(...), userId)`，其中 traceId 不作为参数传入

#### Scenario: traceId 仍然在响应头中
- **WHEN** 请求进入 ContextFillFilter
- **THEN** `X-Trace-Id` 响应头仍然从 `Span.current().getSpanContext().getTraceId()` 获取（不变）

---

### Requirement: MyMetaObjectHandler 使用 BizContext

`MyMetaObjectHandler` SHALL 使用 `BizContext.getUserId()` 获取当前用户 ID。

#### Scenario: 自动填充 createUser/updateUser
- **WHEN** MyBatis-Plus 触发 insertFill/updateFill
- **THEN** 从 `BizContext.getUserId()` 获取 userId，用于填充 `createUser`/`updateUser` 字段
