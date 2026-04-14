## MODIFIED Requirements

### Requirement: 限流模块包路径

限流组件 SHALL 位于 `org.smm.archetype.shared.aspect.ratelimit` 包下，不再是独立的 client 模块。

#### Scenario: 包路径变更

- **WHEN** 开发者使用 @RateLimit 注解
- **THEN** 导入路径 SHALL 为 `org.smm.archetype.shared.aspect.ratelimit.RateLimit`

#### Scenario: Bean 注册方式

- **WHEN** 应用启动
- **THEN** RateLimitAspect SHALL 由 `org.smm.archetype.config.RatelimitConfigure` 通过 `@Bean` 方法注册，不再使用 `@AutoConfiguration`

## REMOVED Requirements

### Requirement: client-ratelimit 独立模块

**Reason**: 限流是应用层横切关注点，不是中间件接入，不应该是独立 client 模块
**Migration**: 代码已迁移到 `app/.../shared/aspect/ratelimit/`
