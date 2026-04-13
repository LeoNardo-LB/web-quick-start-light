## ADDED Requirements

### Requirement: 模块文档统一模板
- **WHEN** 维护者为任意模块创建文档
- **THEN** 文档 MUST 使用统一模板结构，包含以下固定章节：`📋 目录`（锚点链接到所有二级标题）→ 概述（模块定位和职责）→ 业务场景（核心用例列表）→ 技术设计（类图/时序图 + 关键类说明）→ API 参考（公开方法/接口列表，含参数和返回值）→ 配置参考（配置项表格，含默认值和说明）→ 使用指南（集成步骤 + 代码示例），且每个文档 MUST 在顶部包含所属轨道标签（Contract 轨）

#### Scenario: 模板章节完整性验证
- **WHEN** 审查 modules/ 下的任意模块文档
- **THEN** 文档 MUST 在开头包含 `📋 目录` 章节（列出所有二级标题的锚点链接），MUST 包含 `## 概述`、`## 业务场景`、`## 技术设计`、`## API 参考`、`## 配置参考`、`## 使用指南` 六个章节，每个章节 MUST 包含实质性内容（不允许空章节）

### Requirement: auth 模块文档
- **WHEN** 开发者需要了解认证模块
- **THEN** 系统 MUST 在 docs/modules/ 下创建 auth.md，包含 LoginController（POST /api/auth/login, POST /api/auth/logout）、LoginFacadeImpl（登录/登出业务逻辑）、AuthClient 接口（SaTokenAuthClient/NoOpAuthClient 条件装配）、Sa-Token 会话管理说明和配置参考（middleware.auth.*）

#### Scenario: auth 模块 API 参考
- **WHEN** 开发者查阅 auth.md 的 API 参考章节
- **THEN** 文档 MUST 列出 LoginController 的全部端点（POST /api/auth/login、POST /api/auth/logout），每个端点 MUST 包含请求参数、响应格式和认证要求说明

### Requirement: system-config 模块文档
- **WHEN** 开发者需要了解系统配置模块
- **THEN** 系统 MUST 在 docs/modules/ 下创建 system-config.md，包含 SystemConfigController（GET /api/system/configs, GET /api/system/configs/page）、SystemConfigFacade（Entity→VO 转换）、SystemConfigService（CRUD 业务逻辑）、SystemConfig 实体（ConfigKey/ConfigValue/DisplayName record）和配置参考

#### Scenario: system-config 模块业务场景
- **WHEN** 开发者查阅 system-config.md 的业务场景章节
- **THEN** 文档 MUST 列出核心用例：按 Key 查询配置值、分页查询配置列表、更新配置值，每个用例 MUST 包含简短的流程描述

### Requirement: operation-log 模块文档
- **WHEN** 开发者需要了解操作日志模块
- **THEN** 系统 MUST 在 docs/modules/ 下创建 operation-log.md，包含 OperationLogController（GET /api/system/operation-logs 分页查询）、OperationLogFacade（OperationLogVO 转换）、@BusinessLog 注解使用说明、OperationType 枚举（CREATE/UPDATE/DELETE/QUERY/EXPORT/IMPORT）和 OperationLogWriter 持久化接口

#### Scenario: operation-log 模块技术设计
- **WHEN** 开发者查阅 operation-log.md 的技术设计章节
- **THEN** 文档 MUST 包含 LogAspect 切面的处理流程（Mermaid 时序图），展示从 @BusinessLog 注解触发到日志持久化的完整链路

### Requirement: client-cache 模块文档
- **WHEN** 开发者需要了解缓存客户端模块
- **THEN** 系统 MUST 在 docs/modules/ 下创建 client-cache.md，包含 CaffeineCacheClient 实现说明、AbstractCacheClient Template Method 模式（10 个 final 方法）、CacheClient 接口定义、配置参考（middleware.cache.*）和条件装配说明

#### Scenario: client-cache 模块 API 参考
- **WHEN** 开发者查阅 client-cache.md 的 API 参考章节
- **THEN** 文档 MUST 列出 CacheClient 接口的全部 10 个方法，每个方法 MUST 包含方法签名、参数说明和返回值说明

### Requirement: client-oss 模块文档
- **WHEN** 开发者需要了解对象存储客户端模块
- **THEN** 系统 MUST 在 docs/modules/ 下创建 client-oss.md，包含 LocalOssClient 实现说明（NIO + 日期分层存储）、AbstractOssClient Template Method 模式（7 个 final 方法）、OssClient 接口定义和配置参考（middleware.object-storage.*）

#### Scenario: client-oss 模块技术设计
- **WHEN** 开发者查阅 client-oss.md 的技术设计章节
- **THEN** 文档 MUST 包含日期分层存储的目录结构说明和 Mermaid 类图（AbstractOssClient → LocalOssClient 继承关系）

### Requirement: client-email 模块文档
- **WHEN** 开发者需要了解邮件客户端模块
- **THEN** 系统 MUST 在 docs/modules/ 下创建 client-email.md，包含 NoOpEmailClient 默认实现、AbstractEmailClient Template Method 模式（3 个 final 方法）、EmailClient 接口定义、条件装配说明（@ConditionalOnClass Jakarta Mail）和配置参考（middleware.email.*）

### Requirement: client-sms 模块文档
- **WHEN** 开发者需要了解短信客户端模块
- **THEN** 系统 MUST 在 docs/modules/ 下创建 client-sms.md，包含 NoOpSmsClient 默认实现、AbstractSmsClient Template Method 模式（3 个 final 方法）、SmsClient 接口定义、条件装配说明和配置参考（middleware.sms.*）

### Requirement: client-search 模块文档
- **WHEN** 开发者需要了解搜索客户端模块
- **THEN** 系统 MUST 在 docs/modules/ 下创建 client-search.md，包含 SimpleSearchClient 实现说明（ConcurrentHashMap 内存搜索）、AbstractSearchClient Template Method 模式（15 个 final 方法）、SearchClient 接口定义、条件装配说明和配置参考（middleware.search.*）

#### Scenario: client-search 模块 API 参考
- **WHEN** 开发者查阅 client-search.md 的 API 参考章节
- **THEN** 文档 MUST 列出 SearchClient 接口的全部 15 个方法，按功能分组（索引操作/查询操作/管理操作）

### Requirement: client-log 模块文档
- **WHEN** 开发者需要了解日志客户端模块
- **THEN** 系统 MUST 在 docs/modules/ 下创建 client-log.md，包含 @BusinessLog 注解说明（module/operation/samplingRate 扩展属性）、LogAspect 切面处理流程、OperationType 枚举、OperationLogWriter 持久化接口、SamplingTurboFilter 采样说明、SlowQueryInterceptor 慢 SQL 拦截和 8 个 Appender 列表

#### Scenario: client-log 模块业务场景
- **WHEN** 开发者查阅 client-log.md 的业务场景章节
- **THEN** 文档 MUST 列出核心用例：业务方法日志记录、操作日志持久化、高并发采样、慢 SQL 监控，每个用例 MUST 包含对应的注解/配置示例

### Requirement: client-ratelimit 模块文档
- **WHEN** 开发者需要了解限流客户端模块
- **THEN** 系统 MUST 在 docs/modules/ 下创建 client-ratelimit.md，包含 @RateLimit 注解说明、RateLimitAspect 切面、Bucket4j + Caffeine 实现说明、SpelKeyResolver 表达式解析、LimitFallback 策略（REJECT/WAIT/FALLBACK）、配置参考（middleware.ratelimit.*）和条件装配说明

### Requirement: client-idempotent 模块文档
- **WHEN** 开发者需要了解幂等客户端模块
- **THEN** 系统 MUST 在 docs/modules/ 下创建 client-idempotent.md，包含 @Idempotent 注解说明、IdempotentAspect 切面、IdempotentKeyResolver 自定义 Key 解析、Caffeine 幂等 Key 存储说明、配置参考（middleware.idempotent.*）和条件装配说明

### Requirement: client-auth 模块文档
- **WHEN** 开发者需要了解认证客户端模块
- **THEN** 系统 MUST 在 docs/modules/ 下创建 client-auth.md，包含 AuthClient 接口定义、SaTokenAuthClient 实现说明、NoOpAuthClient 默认实现、AbstractAuthClient Template Method 模式、AuthProperties 配置属性、AuthInterceptorConfigurer 拦截器配置和配置参考（middleware.auth.*）

### Requirement: 模块文档内容与代码一致性
- **WHEN** 模块代码发生变更
- **THEN** 对应的 modules/ 文档 MUST 同步更新，文档中的类名、方法签名、配置项、API 路径 MUST 与当前代码 100% 一致（Contract 文档属性），过时的内容 MUST 被标记为废弃或删除

#### Scenario: 代码变更后文档同步
- **WHEN** SystemConfigController 新增了一个端点
- **THEN** system-config.md 的 API 参考章节 MUST 在下一次文档审查时更新，包含新端点的路径、参数和响应格式
