## ADDED Requirements

### Requirement: Idempotent 通用幂等注解
系统 SHALL 提供 `@Idempotent` 注解，支持在任意方法上标注幂等防护。注解参数包括 timeout（幂等窗口时间）、timeUnit（时间单位）、field（SpEL 表达式，从入参提取幂等 Key）、message（重复操作提示消息）。

#### Scenario: 首次调用 @Idempotent 方法正常执行
- **WHEN** 方法标注了 `@Idempotent(timeout = 3000)` 且是首次调用
- **THEN** 方法 SHALL 正常执行，结果被缓存

#### Scenario: 幂等窗口内重复调用被拒绝
- **WHEN** 方法标注了 `@Idempotent(timeout = 3000)` 且在 3 秒内使用相同 Key 重复调用
- **THEN** 系统 SHALL 抛出 BizException，消息为注解配置的 message

#### Scenario: 幂等窗口过期后可正常调用
- **WHEN** 方法标注了 `@Idempotent(timeout = 3000)` 且距离上次调用已超过 3 秒
- **THEN** 方法 SHALL 正常执行

### Requirement: Idempotent 支持字段级 Key 提取
系统 SHALL 支持通过 SpEL 表达式从方法入参中提取特定字段作为幂等 Key。

#### Scenario: 使用 field 属性提取入参字段作为 Key
- **WHEN** 方法标注了 `@Idempotent(field = "#request.orderId")` 且两次调用使用相同 orderId
- **THEN** 第二次调用 SHALL 被幂等防护拦截

#### Scenario: field 为空时使用方法签名+参数 hash 作为 Key
- **WHEN** 方法标注了 `@Idempotent`（field 为空）且参数完全相同
- **THEN** 系统 SHALL 使用 className.methodName(paramsHash) 作为幂等 Key

#### Scenario: 不同参数值的调用互不影响
- **WHEN** 方法标注了 `@Idempotent(field = "#id")` 且使用不同 id 值调用
- **THEN** 每个不同 id 的调用 SHALL 独立计数，互不拦截

### Requirement: Idempotent 基于 Caffeine 缓存
幂等防护底层 SHALL 使用内置 Caffeine 缓存存储 Key，Key 过期时间由 timeout 参数控制。模块 SHALL 作为 `client-idempotent` 独立模块实现。

#### Scenario: 幂等 Key 自动过期
- **WHEN** 幂等 Key 写入 Caffeine 后经过 timeout 时间
- **THEN** Key SHALL 自动过期并被清除

#### Scenario: 未启用幂等模块时注解不生效
- **WHEN** `middleware.idempotent.enabled=false` 或 classpath 中不存在 client-idempotent
- **THEN** `@Idempotent` 注解 SHALL 不产生任何效果
