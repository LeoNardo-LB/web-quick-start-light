# 上下文传播机制 (Context Propagation)

> **职责**: 描述上下文传播机制的设计和实现
> **轨道**: Intent
> **维护者**: Frozen

---

## 目录

- [概述](#概述)
- [设计背景](#设计背景)
- [技术方案](#技术方案)
  - [BizContext 核心架构](#bizcontext-核心架构)
  - [上下文键设计](#上下文键设计)
  - [传播链路全流程](#传播链路全流程)
  - [子线程传播机制](#子线程传播机制)
  - [跨服务传播机制](#跨服务传播机制)
- [关键设计决策](#关键设计决策)
  - [决策 1：ScopedValue 替代 ThreadLocal](#决策-1scopedvalue-替代-threadlocal)
  - [决策 2：三合一传播而非分散方案](#决策-2三合一传播而非分散方案)
  - [决策 3：副本隔离机制](#决策-3副本隔离机制)
  - [决策 4：traceId 不纳入业务上下文](#决策-4traceid-不纳入业务上下文)
- [API 参考](#api-参考)
- [依赖关系](#依赖关系)
- [相关文档](#相关文档)
- [变更历史](#变更历史)

---

## 概述

BizContext 是项目上下文传播的核心基础设施，基于 **Java 25 ScopedValue** + **OpenTelemetry Baggage** 实现三合一传播机制：

| 传播维度 | 技术 | 职责 |
|----------|------|------|
| 业务上下文 | `ScopedValue<Holder>` | 请求作用域内的 userId 等业务数据存取 |
| 链路追踪 | OTel Baggage | propagated=true 的键自动跨服务传播 |
| 日志关联 | MDC（由 TaskDecorator 设置） | traceId 自动写入日志，关联请求全链路 |

三者通过 `Filter → BizContext.runWithContext → TaskDecorator` 链路自动传递，业务代码零感知。

---

## 设计背景

### 问题域

在 Web 应用中，`userId`、`tenantId` 等业务上下文需要在请求处理的各层之间传递（Controller → Service → Repository），同时还需要在异步线程池中可用。传统方案存在以下痛点：

1. **ThreadLocal 泄漏风险** — 线程池复用时忘记清理导致上下文污染
2. **异步传播断裂** — `@Async` 或线程池提交任务时上下文丢失
3. **跨服务传播困难** — 微服务调用链路中 userId 无法自动传递
4. **多维度传播分散** — 业务上下文、traceId、MDC 各自独立管理，配置复杂

### 设计目标

- **零侵入** — 业务代码通过静态方法 `BizContext.getUserId()` 获取，无需传参
- **自动传播** — Filter 入口设置，线程池自动传播，全链路无需手动干预
- **跨服务** — propagated=true 的键通过 OTel Baggage 在 HTTP/gRPC 调用间自动传递
- **线程安全** — 子线程持有独立副本，修改不影响请求线程

---

## 技术方案

### BizContext 核心架构

```
┌─────────────────────────────────────────────────┐
│                   BizContext                      │
│                                                  │
│  ScopedValue<Holder> ──── 请求线程绑定            │
│       │                                          │
│       ├── Holder.map: EnumMap<Key, String>        │
│       └── Holder.replica: boolean                 │
│              (false=请求线程, true=子线程副本)      │
│                                                  │
│  Key 枚举                                        │
│       └── USER_ID("userId", propagated=true)      │
└─────────────────────────────────────────────────┘
```

**Holder** 是内部持有器，包含两个关键字段：
- `map`：`EnumMap<Key, String>` 存储所有上下文键值对
- `replica`：标记是否为子线程副本（影响 Baggage 写入行为）

### 上下文键设计

```java
public enum Key {
    USER_ID("userId", true);
    // 扩展方式：新增枚举值即可，例如 TENANT_ID("tenantId", true)

    private final String baggageKey;   // OTel Baggage 中的键名
    private final boolean propagated;  // 是否跨服务传播
}
```

| 属性 | 说明 |
|------|------|
| `baggageKey` | 在 OTel Baggage 中使用的键名，如 `"userId"` |
| `propagated` | `true` = 请求线程 set 时自动写入 OTel Baggage；`false` = 仅 JVM 内可见 |

扩展新键只需在枚举中新增一行，无需修改其他代码。

### 传播链路全流程

```
HTTP 请求
  │
  ▼
┌──────────────────────┐
│  ContextFillFilter   │  ① 从 AuthComponent 获取 userId
│  (OncePerRequest)    │  ② 调用 BizContext.runWithContext()
└──────────┬───────────┘
           │ ScopedValue.where(SCOPED, holder).run(action)
           │ Baggage.makeCurrent() (propagated 键)
           ▼
┌──────────────────────┐
│  Controller / Service│  ③ BizContext.getUserId() 读取
│  / Repository 层     │  ④ MyMetaObjectHandler 自动填充 createUser/updateUser
└──────────┬───────────┘
           │ 提交异步任务到线程池
           ▼
┌──────────────────────┐
│  TaskDecorator       │  ⑤ BizContext.copyAsReplica() 创建副本
│  (线程池装饰器)       │  ⑥ ScopedValue.where(SCOPED, replica).run(task)
│                      │  ⑦ MDC.put("traceId", ...) 设置日志上下文
└──────────────────────┘
```

**关键步骤说明**：

1. **ContextFillFilter** 解析 userId（优先从 AuthComponent 获取，无认证组件时返回 `"SYSTEM"`，未登录返回 `"ANONYMOUS"`）
2. `runWithContext()` 同时绑定 ScopedValue 和 OTel Baggage（双重绑定）
3. 业务代码通过 `BizContext.getUserId()` 或 `BizContext.Key.USER_ID.get()` 读取
4. `MyMetaObjectHandler` 自动从 `BizContext.getUserId()` 获取值填充数据库审计字段
5. 线程池 TaskDecorator 在任务提交时捕获当前上下文
6. 子线程使用副本执行任务，保证线程隔离

### 子线程传播机制

```java
// TaskDecorator 核心逻辑（简化）
public Runnable decorate(Runnable task) {
    // 1. 在请求线程中捕获副本
    BizContext.Holder replica = BizContext.copyAsReplica();
    String traceId = Span.current().getSpanContext().getTraceId();
    Map<String, String> mdc = MDC.getCopyOfContextMap();

    return () -> {
        // 2. 在子线程中绑定副本
        ScopedValue.where(BizContext.getScoped(), replica).run(() -> {
            MDC.put("traceId", traceId);
            try {
                task.run();
            } finally {
                MDC.clear();
            }
        });
    };
}
```

**副本行为**：
- 子线程读取 `BizContext.getUserId()` 正常返回请求线程的值
- 子线程调用 `BizContext.Key.USER_ID.set(newValue)` 仅修改副本 EnumMap，**不会**写入 OTel Baggage
- 副本修改不影响请求线程的上下文

### 跨服务传播机制

当 `propagated=true` 的键被设置时，BizContext 自动同步到 OTel Baggage：

```
请求线程: Key.USER_ID.set("user123")
  → syncToBaggage() → Baggage.current().toBuilder().put("userId", "user123")
  → OTel SDK 自动将 Baggage 注入 HTTP Header（如 W3C baggage header）
  → 下游服务通过 OTel SDK 自动解析 Baggage
  → 下游服务通过 Baggage.current().getEntryValue("userId") 获取
```

---

## 关键设计决策

### 决策 1：ScopedValue 替代 ThreadLocal

| 维度 | ThreadLocal | ScopedValue (Java 25) |
|------|-------------|----------------------|
| 生命周期 | 需手动 `remove()`，线程池复用有泄漏风险 | `ScopedValue.where().run()` 自动绑定/解绑 |
| 不可变性 | 可任意修改，并发不安全 | 绑定后不可变，子线程通过副本隔离 |
| 性能 | 无额外开销 | 逃逸分析优化，零成本抽象 |
| 语义 | 全局可变状态 | 结构化并发，作用域清晰 |

**选择理由**：Java 25 ScopedValue 提供了编译期保证的作用域安全，消除了 ThreadLocal 的内存泄漏风险，且与虚拟线程（Virtual Threads）天然兼容。

### 决策 2：三合一传播而非分散方案

**备选方案**：
- 方案 A：Reactor Context（仅响应式栈）
- 方案 B：ThreadLocal + 手动 MDC + 手动 Header（分散管理）
- 方案 C：Micrometer Context Propagation（需额外依赖）
- 方案 D：**BizContext 三合一**（当前方案）

**选择 D 的理由**：
1. **统一入口** — `runWithContext()` 一次调用绑定三个维度
2. **最小依赖** — 仅依赖 JDK 25 + OTel SDK（项目已有）
3. **同步栈友好** — 项目使用 Spring MVC 同步栈，不需要 Reactor Context

### 决策 3：副本隔离机制

子线程通过 `copyAsReplica()` 获得独立副本，设计考量：

- **为什么需要副本**：线程池中的线程被多个请求复用，不能直接绑定请求线程的 Holder
- **为什么 set() 不写 Baggage**：子线程的修改是临时性的（如缓存中间结果），不应传播到外部服务
- **为什么打印 WARN**：显式提醒开发者子线程 set 不会跨服务传播，避免误用

### 决策 4：traceId 不纳入业务上下文

traceId 由 OTel Span 全权负责（`Span.current().getSpanContext().getTraceId()`），不放入 BizContext 的原因：

1. **单一职责** — BizContext 管理业务数据，OTel 管理链路追踪
2. **自动传播** — OTel SDK 已自动在 HTTP Header 中传播 traceId
3. **日志集成** — MDC 中的 traceId 由 OTel 自动注入，无需手动设置

---

## API 参考

### BizContext 公开方法

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `runWithContext(Runnable, EnumMap<Key, String>)` | void | 在指定上下文中执行操作（绑定 ScopedValue + OTel Baggage） |
| `runWithContext(Runnable, Key, String)` | void | 便捷方法，使用单个键值对 |
| `getContext()` | EnumMap\<Key, String\> | 获取当前上下文（可修改，影响请求线程 Baggage） |
| `copyContext()` | EnumMap\<Key, String\> | 复制当前上下文（独立副本，不影响原上下文） |
| `copyAsReplica()` | Holder | 创建副本 Holder（供 TaskDecorator 使用） |
| `getScoped()` | ScopedValue\<Holder\> | 获取 ScopedValue 引用（供 TaskDecorator 绑定） |
| `getUserId()` | String | 便捷方法，获取当前用户 ID |

### Key 枚举方法

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `get()` | String | 获取当前上下文中该键的值 |
| `set(String)` | void | 在当前上下文中更新值（请求线程自动同步 Baggage） |
| `getBaggageKey()` | String | 获取 OTel Baggage 中的键名 |
| `isPropagated()` | boolean | 是否标记为跨服务传播 |

---

## 依赖关系

### 内部依赖

```
BizContext
  ├── 被 ContextFillFilter 依赖（Filter 入口设置上下文）
  ├── 被 ThreadPoolConfigure 依赖（TaskDecorator 子线程传播）
  ├── 被 MyMetaObjectHandler 依赖（自动填充 createUser/updateUser）
  └── 被 TestController 依赖（测试验证）
```

### 外部依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| `java.lang.ScopedValue` | Java 25 | 请求作用域绑定 |
| `io.opentelemetry.api.baggage` | OTel SDK | 跨服务 Baggage 传播 |

---

## 相关文档

| 文档 | 关系 | 说明 |
|------|------|------|
| AOP 切面 | infrastructure/aop-aspects.md | LogAspect 使用 OTel Span 获取 traceId |
| 日志体系 | infrastructure/logging-system.md | MDC traceId 关联日志链路 |
| 配置参考 | guides/configuration-reference.md | 线程池配置参数 |
| 编码规范 | guides/coding-standards.md | 上下文使用规范 |
