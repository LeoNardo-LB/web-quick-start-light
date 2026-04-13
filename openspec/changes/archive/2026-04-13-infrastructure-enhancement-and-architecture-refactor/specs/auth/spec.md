## ADDED Requirements

### Requirement: Sa-Token 登录认证
系统 SHALL 使用 Sa-Token（v1.45.0+）提供登录认证能力。仅做登录/注销和 Token 校验，不做角色权限管理。认证模块 SHALL 作为 `client-auth` 模块实现。

#### Scenario: 用户通过用户名密码登录
- **WHEN** 客户端发送 `POST /api/auth/login` 携带正确的用户名和密码
- **THEN** 系统 SHALL 调用 StpUtil.login(userId) 并返回 Token 信息（tokenName、tokenValue）

#### Scenario: 用户名或密码错误时登录失败
- **WHEN** 客户端发送 `POST /api/auth/login` 携带错误的用户名或密码
- **THEN** 系统 SHALL 返回登录失败响应，不创建会话

#### Scenario: 已登录用户注销
- **WHEN** 客户端发送 `POST /api/auth/logout` 携带有效 Token
- **THEN** 系统 SHALL 调用 StpUtil.logout() 清除会话

### Requirement: 路由拦截校验登录状态
系统 SHALL 通过 SaInterceptor 配置路由拦截规则。排除登录、注册、API 文档、静态资源等路径，其余路径 MUST 校验登录状态。

#### Scenario: 未登录访问受保护端点被拦截
- **WHEN** 未登录用户访问 `GET /api/system/configs`（不带 Token）
- **THEN** 系统 SHALL 返回未认证错误响应

#### Scenario: 已登录访问受保护端点正常通过
- **WHEN** 已登录用户携带有效 Token 访问 `GET /api/configs`
- **THEN** 系统 SHALL 正常返回数据

#### Scenario: 登录接口无需认证
- **WHEN** 访问 `POST /api/auth/login`
- **THEN** 系统 SHALL 放行，不校验登录状态

### Requirement: ContextFillFilter 集成 Sa-Token
ContextFillFilter SHALL 从 Sa-Token 获取当前登录用户 ID 填充到 ScopedThreadContext，替代硬编码的 "SYSTEM"。未登录时 userId 为 "ANONYMOUS"。

#### Scenario: 已登录请求填充真实 userId
- **WHEN** 已登录用户发送请求
- **THEN** ScopedThreadContext.getUserId() SHALL 返回该用户的真实 ID

#### Scenario: 未登录请求填充 ANONYMOUS
- **WHEN** 未登录用户发送请求（访问无需认证的端点）
- **THEN** ScopedThreadContext.getUserId() SHALL 返回 "ANONYMOUS"

### Requirement: 用户表与密码安全
系统 SHALL 提供 `user` 数据库表（id、username、password_hash、nickname、status、create_time、update_time）。密码 SHALL 使用 BCrypt 哈希存储。

#### Scenario: 用户密码使用 BCrypt 哈希存储
- **WHEN** 创建新用户时传入明文密码
- **THEN** 系统 SHALL 使用 BCrypt 哈希后存储到 password_hash 字段

### Requirement: NoOp 认证降级
当 classpath 中不存在 Sa-Token 依赖时，client-auth 模块 SHALL 使用 NoOp 默认实现，不执行任何认证逻辑。ContextFillFilter 中 userId 保持为 "SYSTEM"。

#### Scenario: 未引入 Sa-Token 依赖时不做认证
- **WHEN** classpath 中不存在 Sa-Token 类
- **THEN** 系统 SHALL 使用 NoOpAuthClient，所有请求视为已认证

#### Scenario: 引入 Sa-Token 但配置关闭时不做认证
- **WHEN** `middleware.auth.enabled=false`
- **THEN** 系统 SHALL 不加载认证拦截器，所有请求视为已认证
