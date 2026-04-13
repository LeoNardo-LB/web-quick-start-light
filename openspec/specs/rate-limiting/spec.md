## ADDED Requirements

### Requirement: RateLimit 注解式限流
系统 SHALL 提供 `@RateLimit` 注解，支持在任意方法上标注限流策略。注解参数包括 capacity（桶容量）、refillTokens（补充令牌数）、refillDuration（补充时间）、refillUnit（时间单位）、key（SpEL 表达式提取限流 Key）、fallback（降级策略）。

#### Scenario: 标注 @RateLimit 的方法在限流窗口内被正常调用
- **WHEN** 方法标注了 `@RateLimit(capacity = 10, refillTokens = 10, refillDuration = 1, refillUnit = SECONDS)` 且桶内有令牌
- **THEN** 方法 SHALL 正常执行并返回结果

#### Scenario: 标注 @RateLimit 的方法超过限流阈值被拒绝
- **WHEN** 方法标注了 `@RateLimit(capacity = 2)` 且已在窗口内调用了 2 次
- **THEN** 第 3 次调用 SHALL 抛出 `BizException`，错误码为限流相关的 CommonErrorCode

#### Scenario: @RateLimit 使用 SpEL 表达式提取限流 Key
- **WHEN** 方法标注了 `@RateLimit(key = "#request.ip")` 且不同 IP 发起请求
- **THEN** 系统 SHALL 按 IP 维度分别限流，互不影响

#### Scenario: @RateLimit 降级策略为 WAIT 时阻塞等待
- **WHEN** 方法标注了 `@RateLimit(capacity = 1, fallback = LimitFallback.WAIT)` 且桶内无令牌
- **THEN** 调用 SHALL 阻塞直到获取到令牌后执行

### Requirement: RateLimit 基于 Bucket4j + Caffeine 实现
限流底层 SHALL 使用 Bucket4j 令牌桶算法，Bucket 实例存储在 Caffeine 缓存中。限流模块 SHALL 作为 `client-ratelimit` 模块实现，遵循 Template Method + 条件装配模式。

#### Scenario: 未引入 Bucket4j 依赖时限流不生效
- **WHEN** classpath 中不存在 Bucket4j 类
- **THEN** `@RateLimit` 注解 SHALL 不产生任何效果（NoOp 降级）

#### Scenario: 引入 Bucket4j 依赖后限流自动生效
- **WHEN** classpath 中存在 Bucket4j 类且 `middleware.ratelimit.enabled=true`
- **THEN** `@RateLimit` 注解 SHALL 通过 AOP 切面拦截方法并执行限流逻辑

### Requirement: RateLimit 降级与熔断机制
系统 SHALL 支持三种降级策略：REJECT（直接拒绝，默认）、WAIT（阻塞等待）、FALLBACK（执行降级方法）。

#### Scenario: 降级策略为 REJECT 时抛出业务异常
- **WHEN** 限流触发且 fallback = REJECT
- **THEN** 系统 SHALL 抛出 BizException 并返回友好的限流提示消息

#### Scenario: 降级策略为 FALLBACK 时执行降级方法
- **WHEN** 限流触发且 fallback = FALLBACK 且配置了 fallbackMethod
- **THEN** 系统 SHALL 执行降级方法并返回其结果
