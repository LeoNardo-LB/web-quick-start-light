## MODIFIED Requirements

### Requirement: 幂等存储依赖 CacheClient

IdempotentAspect SHALL 注入 CacheClient（而非自建 Caffeine Cache）实现幂等校验。使用 CacheClient 的 `hasKey()` 检查窗口内重复，
`put(key, value, duration)` 记录调用标记（duration 为注解的 timeout），`delete(key)` 在执行失败时清除标记。

#### Scenario: 通过 CacheClient 实现幂等检查

- **WHEN** 方法标注 @Idempotent(timeout=3000) 被调用
- **THEN** 切面 SHALL 调用 `cacheClient.hasKey(key)` 检查是否存在未过期的标记

#### Scenario: 幂等标记写入

- **WHEN** 首次调用或窗口已过期
- **THEN** 切面 SHALL 调用 `cacheClient.put(key, "1", Duration.ofMillis(timeoutMillis))` 写入标记

#### Scenario: 执行失败清除标记

- **WHEN** 业务方法执行抛出异常
- **THEN** 切面 SHALL 调用 `cacheClient.delete(key)` 清除标记以允许重试

### Requirement: 幂等模块包路径

幂等组件 SHALL 位于 `org.smm.archetype.shared.aspect.idempotent` 包下，不再是独立的 client 模块。

#### Scenario: 包路径变更

- **WHEN** 开发者使用 @Idempotent 注解
- **THEN** 导入路径 SHALL 为 `org.smm.archetype.shared.aspect.idempotent.Idempotent`

#### Scenario: Bean 注册方式

- **WHEN** 应用启动
- **THEN** IdempotentAspect SHALL 由 `org.smm.archetype.config.IdempotentConfigure` 通过 `@Bean` 方法注册，不再使用 `@AutoConfiguration`

## REMOVED Requirements

### Requirement: client-idempotent 独立模块

**Reason**: 幂等是应用层横切关注点，不是中间件接入，不应该是独立 client 模块
**Migration**: 代码已迁移到 `app/.../shared/aspect/idempotent/`，存储改为依赖 CacheClient

### Requirement: 自建 Caffeine Cache 存储

**Reason**: CacheClient 已有完整的 TTL 能力，无需重复造轮子
**Migration**: 使用 CacheClient.hasKey/put/delete 替代 Cache.getIfPresent/put/invalidate
