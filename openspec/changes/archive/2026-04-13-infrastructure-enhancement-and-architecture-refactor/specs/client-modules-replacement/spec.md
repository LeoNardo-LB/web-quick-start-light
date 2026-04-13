## ADDED Requirements

### Requirement: client-auth 认证客户端模块
系统 SHALL 新增 client-auth 模块，遵循 Template Method + 条件装配 + NoOp 默认模式。AuthClient 接口定义 login/logout/getCurrentUserId/isLogin/checkLogin 方法。

#### Scenario: client-auth 模块结构符合规范
- **WHEN** 检查 client-auth 模块
- **THEN** SHALL 包含 AuthClient 接口、AbstractAuthClient（Template Method）、NoOpAuthClient、SaTokenAuthClient、AuthAutoConfiguration

#### Scenario: client-auth 条件装配
- **WHEN** classpath 不存在 Sa-Token 或 middleware.auth.enabled=false
- **THEN** SHALL 使用 NoOpAuthClient

### Requirement: client-ratelimit 限流客户端模块
系统 SHALL 新增 client-ratelimit 模块，遵循 Template Method + 条件装配 + NoOp 默认模式。Bucket4j 为 optional 依赖。

#### Scenario: client-ratelimit 模块结构符合规范
- **WHEN** 检查 client-ratelimit 模块
- **THEN** SHALL 包含 RateLimit 注解、RateLimitAspect 切面、RateLimitAutoConfiguration

### Requirement: client-idempotent 幂等客户端模块
系统 SHALL 新增 client-idempotent 模块，遵循 Template Method + 条件装配 + NoOp 默认模式。依赖 client-cache（Caffeine）。

#### Scenario: client-idempotent 模块结构符合规范
- **WHEN** 检查 client-idempotent 模块
- **THEN** SHALL 包含 Idempotent 注解、IdempotentAspect 切面、IdempotentAutoConfiguration
