# AOP 切面 (AOP Aspects)

> **职责**: 描述 AOP 切面的 API、流程和配置
> **轨道**: Contract
> **维护者**: AI

---

## 目录

- [概述](#概述)
- [公共 API 参考](#公共-api-参考)
  - [自定义注解](#自定义注解)
  - [切面类](#切面类)
  - [核心类型定义](#核心类型定义)
- [服务流程](#服务流程)
  - [幂等防护流程 (IdempotentAspect)](#幂等防护流程-idempotentaspect)
  - [业务日志流程 (LogAspect)](#业务日志流程-logaspect)
  - [限流控制流程 (RateLimitAspect)](#限流控制流程-ratelimitaspect)
- [SpEL 表达式解析](#spel-表达式解析)
- [切面配置](#切面配置)
- [依赖关系](#依赖关系)
- [相关文档](#相关文档)
- [变更历史](#变更历史)

---

## 概述

项目通过 Spring AOP 提供三大横切关注点：

| 切面 | 注解 | 核心能力 | 依赖组件 |
|------|------|---------|---------|
| **幂等防护** | `@Idempotent` | 基于 CacheComponent 的重复请求拦截 | CacheComponent |
| **业务日志** | `@BusinessLog` | 方法执行日志 + 异步持久化操作日志 | OperationLogWriter |
| **限流控制** | `@RateLimit` | 基于 Bucket4j 令牌桶的接口限流 | Bucket4j |

三大切面均使用 `@Around` 环绕通知，支持 SpEL 表达式动态提取 Key。

---

## 公共 API 参考

### 自定义注解

#### @Idempotent — 幂等防护

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `timeout` | `long` | `3000` | 幂等窗口超时时间 |
| `timeUnit` | `TimeUnit` | `MILLISECONDS` | 超时时间单位 |
| `field` | `String` | `""` | SpEL 表达式提取幂等 Key 字段 |
| `message` | `String` | `"请勿重复操作"` | 重复调用时的异常消息 |

**使用示例**：
```java
@Idempotent(timeout = 5000, field = "#req.orderId")
public Result createOrder(CreateOrderRequest req) { ... }
```

#### @BusinessLog — 业务操作日志

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `value` | `String` | `""` | 操作描述 |
| `module` | `String` | `""` | 所属模块 |
| `operation` | `OperationType` | `QUERY` | 操作类型 |
| `samplingRate` | `double` | `1.0` | 采样率（0.0~1.0） |

**使用示例**：
```java
@BusinessLog(value = "创建订单", module = "ORDER", operation = OperationType.CREATE)
public Result createOrder(CreateOrderRequest req) { ... }

@BusinessLog(value = "批量查询", module = "SYSTEM", samplingRate = 0.1)
public Result batchQuery(QueryRequest req) { ... }
```

#### @RateLimit — 接口限流

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `capacity` | `double` | `10` | 桶容量（最大并发数） |
| `refillTokens` | `double` | `10` | 每次补充令牌数 |
| `refillDuration` | `long` | `1` | 补充时间窗口 |
| `refillUnit` | `TimeUnit` | `SECONDS` | 时间单位 |
| `key` | `String` | `""` | 限流 Key 的 SpEL 表达式 |
| `fallback` | `LimitFallback` | `REJECT` | 降级策略 |
| `fallbackMethod` | `String` | `""` | 降级方法名（fallback=FALLBACK 时生效） |

**使用示例**：
```java
@RateLimit(capacity = 100, refillTokens = 100, refillDuration = 1)
public Result queryList(QueryRequest req) { ... }

@RateLimit(capacity = 5, key = "#req.userId", fallback = LimitFallback.WAIT)
public Result submitOrder(SubmitRequest req) { ... }

@RateLimit(capacity = 10, fallback = LimitFallback.FALLBACK, fallbackMethod = "fallbackQuery")
public Result hotData(QueryRequest req) { ... }
```

### 切面类

| 切面 | 切点 | 核心方法 |
|------|------|---------|
| `IdempotentAspect` | `@Around("@annotation(idempotent)")` | `around(ProceedingJoinPoint, Idempotent)` |
| `LogAspect` | `@Around("logCut()")` | `doAround(ProceedingJoinPoint)` |
| `RateLimitAspect` | `@Around("rateLimitCut()")` | `doRateLimit(ProceedingJoinPoint)` |

### 核心类型定义

#### OperationType — 操作类型枚举

| 值 | Code | 说明 |
|----|------|------|
| `CREATE` | `CREATE` | 新增 |
| `UPDATE` | `UPDATE` | 修改 |
| `DELETE` | `DELETE` | 删除 |
| `QUERY` | `QUERY` | 查询 |
| `EXPORT` | `EXPORT` | 导出 |
| `IMPORT` | `IMPORT` | 导入 |

#### LimitFallback — 限流降级策略枚举

| 值 | 说明 |
|----|------|
| `REJECT` | 拒绝请求，抛 BizException（默认） |
| `WAIT` | 阻塞等待令牌补充 |
| `FALLBACK` | 执行降级方法 |

#### OperationLogRecord — 操作日志传输对象

```java
public record OperationLogRecord(
    String traceId,
    String userId,
    String module,
    String operationType,
    String description,
    String method,
    String params,
    String result,
    long executionTime,
    String ip,
    String status,
    String errorMessage
) {}
```

#### OperationLogWriter — 日志写入策略接口

```java
public interface OperationLogWriter {
    void write(OperationLogRecord record);
}
```

实现方位于 `app` 模块（如 MyBatis 持久化实现），LogAspect 通过此接口解耦日志写入逻辑。

---

## 服务流程

### 幂等防护流程 (IdempotentAspect)

```
方法调用 → @Around 切面
  │
  ├─ 1. resolveKey() 解析幂等 Key
  │     ├─ field 非空 → SpEL 解析（如 #req.orderId → "ORD123"）
  │     └─ field 为空 → paramsHashCode（方法签名 + 参数哈希）
  │
  ├─ 2. CacheComponent.get(idempotentKey)
  │     ├─ Key 已存在 → 抛 BizException(请勿重复操作)
  │     └─ Key 不存在 ↓
  │
  ├─ 3. CacheComponent.put(idempotentKey, "1", timeout)
  │
  ├─ 4. joinPoint.proceed() 执行目标方法
  │     ├─ 执行成功 → 返回结果
  │     └─ 执行失败 → CacheComponent.remove(idempotentKey) 允许重试
  │
  └─ 5. 返回结果
```

**关键行为**：
- 幂等窗口到期后 Key 自动过期（由 CacheComponent TTL 管理）
- 方法执行异常时自动移除 Key，允许客户端重试
- Key 格式：`类名:方法名:SpEL值` 或 `类名:方法名:paramsHashCode`

### 业务日志流程 (LogAspect)

```
方法调用 → @Around 切面
  │
  ├─ 1. 记录开始时间
  │
  ├─ 2. joinPoint.proceed() 执行目标方法
  │     ├─ 成功 → status = "SUCCESS"
  │     └─ 异常 → status = "ERROR", errorMessage = e.getMessage()
  │
  ├─ 3. SLF4J 记录方法执行日志
  │     ├─ 成功 → log.info("方法执行成功: {}", description)
  │     └─ 失败 → log.error("方法执行失败: {}", description, e)
  │
  ├─ 4. 采样率过滤
  │     ├─ random() < samplingRate → 继续
  │     └─ random() >= samplingRate → 跳过（不写入数据库）
  │
  ├─ 5. 构建 OperationLogRecord
  │     ├─ traceId → Span.current().getSpanContext().getTraceId()
  │     ├─ userId → BizContext.getUserId()
  │     ├─ params → JSON.toJSONString(args)，超过 2048 字符截断
  │     └─ result → JSON.toJSONString(result)，超过 2048 字符截断
  │
  └─ 6. OperationLogWriter.write(record) 异步写入
```

**关键行为**：
- traceId 从 OTel Span 获取（非 BizContext），保证链路追踪一致性
- JSON 序列化使用 FastJSON2，超大字符串截断为 2048 字符
- 采样率支持对高频查询接口降采样（如 `samplingRate = 0.1`）
- LogAspect 有无参构造和有参构造两种模式，无 OperationLogWriter 时仅记录 SLF4J 日志

### 限流控制流程 (RateLimitAspect)

```
方法调用 → @Around 切面
  │
  ├─ 1. buildBucketKey() 构建 Bucket Key
  │     └─ 方法签名 + SpEL 解析值（如 "submitOrder:user123"）
  │
  ├─ 2. ConcurrentHashMap.getOrDefault(key) 获取/创建 Bucket
  │     └─ BucketFactory.createBucket(capacity, refillTokens, refillDuration, refillUnit)
  │
  ├─ 3. bucket.tryConsume(1) 尝试消费令牌
  │     │
  │     ├─ 成功 → joinPoint.proceed() 执行目标方法
  │     │
  │     └─ 失败 → 根据 fallback 策略处理
  │           ├─ REJECT → 抛 BizException(请求过于频繁)
  │           ├─ WAIT → bucket.asScheduler().consume(1, timeout).await()
  │           │         ├─ 等待成功 → 执行目标方法
  │           │         └─ 等待超时 → 抛 BizException
  │           └─ FALLBACK → 反射调用 fallbackMethod
  │
  └─ 4. 返回结果
```

**三种降级策略对比**：

| 策略 | 行为 | 适用场景 |
|------|------|---------|
| `REJECT` | 直接拒绝，返回错误 | 默认策略，适用于大部分接口 |
| `WAIT` | 阻塞等待令牌补充 | 用户级限流，可接受短暂等待 |
| `FALLBACK` | 执行降级方法 | 热点数据降级为缓存数据 |

---

## SpEL 表达式解析

幂等和限流切面均支持 SpEL 表达式动态提取 Key：

### 支持的变量格式

| 格式 | 示例 | 说明 |
|------|------|------|
| `#参数名` | `#req.orderId` | 按方法参数名引用（需编译时保留参数名） |
| `#p0` / `#a0` | `#p0.id` | 按参数索引引用（0-based） |
| `#arg0` | `#arg0.userId` | 降级格式（某些编译环境下参数名丢失） |

### 解析器对比

| 解析器 | 切面 | 变量注册方式 |
|--------|------|-------------|
| `IdempotentKeyResolver` | IdempotentAspect | `#参数名` + `#p0/#a0` |
| `SpelKeyResolver` | RateLimitAspect | `#参数名` + `#arg0`（静态方法） |

### SpEL 解析异常处理

SpEL 表达式解析失败时采用降级策略：
- IdempotentAspect：降级为 `paramsHashCode`（方法签名 + 参数哈希），不会中断请求
- RateLimitAspect：使用方法签名作为 Key（全局限流），不会中断请求

---

## 切面配置

切面通过 Spring Boot AutoConfiguration 自动装配：

| Configure 类 | 装配的 Bean | 条件 |
|-------------|------------|------|
| `IdempotentConfigure` | `IdempotentAspect` | CacheComponent 可用 |
| `RatelimitConfigure` | `RateLimitAspect` | 类路径存在 Bucket4j |
| `LoggingConfigure` | `LogAspect` | 始终装配 |

`LogAspect` 有两个构造器：
- 无参构造：不写入数据库，仅记录 SLF4J 日志
- 有参构造（注入 `OperationLogWriter`）：同时写入数据库

---

## 依赖关系

### 模块内部依赖

```
shared.aspect.idempotent
  ├── IdempotentAspect → CacheComponent (components)
  └── IdempotentAspect → BizException / CommonErrorCode (common)

shared.aspect.operationlog
  ├── LogAspect → OperationLogWriter (接口，app 模块实现)
  ├── LogAspect → Span (OTel — 获取 traceId)
  └── LogAspect → OperationLogRecord (内部 DTO)

shared.aspect.ratelimit
  ├── RateLimitAspect → BucketFactory (内部)
  ├── RateLimitAspect → SpelKeyResolver (内部)
  └── RateLimitAspect → BizException / CommonErrorCode (common)
```

### 外部框架依赖

| 框架 | 版本 | 用途 |
|------|------|------|
| Spring AOP (AspectJ) | Boot 4.x 管理 | `@Around` 切面编程 |
| Spring Expression (SpEL) | Boot 4.x 管理 | 动态 Key 解析 |
| Bucket4j | 8.17.0 | 令牌桶限流算法 |
| OpenTelemetry | Boot 4.x 管理 | traceId 获取 |
| FastJSON2 | 2.0.61 | 参数/结果 JSON 序列化 |

### 反向依赖

| 引用方 | 引用的切面 |
|--------|-----------|
| `config/IdempotentConfigure` | IdempotentAspect |
| `config/RatelimitConfigure` | RateLimitAspect |
| `config/LoggingConfigure` | LogAspect, OperationLogWriter |
| `controller/test/TestController` | @BusinessLog 注解 |

---

## 相关文档

| 文档 | 关系 | 说明 |
|------|------|------|
| 上下文传播机制 | infrastructure/context-propagation.md | BizContext 提供 userId 给 LogAspect |
| 日志体系 | infrastructure/logging-system.md | LogAspect 的 SLF4J 日志输出配置 |
| 测试指南 | guides/testing-guide.md | 切面测试规范与覆盖要求 |
| 配置参考 | guides/configuration-reference.md | 切面相关配置项 |

---

## 变更历史

| 版本 | 日期 | 变更内容 |
|------|------|---------|
| 1.0.0 | 2026-04-25 | 初始版本：IdempotentAspect、LogAspect、RateLimitAspect 三大切面 |
