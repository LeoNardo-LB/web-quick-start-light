## Why

当前项目的 5 个中间件客户端（Cache/Email/SMS/OSS/Search）已模块化为独立子模块，其中 Cache（Caffeine）、OSS（本地文件）、Email（Jakarta Mail）、Search（ConcurrentHashMap）已有真实实现，SMS 仅有接口+抽象基类无真实实现。各 Client 通过 `@AutoConfiguration` 条件装配注册 Bean，无 Logging fallback 层。

## What Changes

- **BREAKING**：项目从单模块（`packaging=jar`）重构为多模块（`packaging=pom`），拆分为 `common`（异常体系 + 公共工具）、`clients`（中间件子模块）和 `app`（主应用）
- `common` 模块：独立提取异常体系（ErrorCode / CommonErrorCode / BaseException / BizException / ClientException / SysException），作为所有模块的公共依赖
- 将 5 个 Client 接口（Cache/Email/SMS/OSS/Search）和 8 个 DTO 移入 `clients` 模块各子模块
- 新增 Template Method 抽象基类（`AbstractCacheClient` / `AbstractOssClient` / `AbstractEmailClient` / `AbstractSmsClient`），统一参数校验、日志、异常包装
- 新增 CacheClient 真实实现：`CaffeineCacheClient`（基于 Caffeine，线程安全过期控制）
- 新增 OssClient 真实实现：`LocalOssClient`（本地文件系统，NIO 零拷贝 + 日期分层存储）
- 新增 EmailClient 真实实现：`JakartaEmailClient`（基于 Jakarta Mail / spring-boot-starter-mail）
- 新增 SearchClient 真实实现：`SimpleSearchClient`（基于 ConcurrentHashMap，开发环境用）
- SmsClient 无真实实现，仅提供接口和 AbstractSmsClient 抽象基类供后续扩展
- Properties 配置前缀统一为 `middleware.*`（从 `cache.*` / `email.*` 等迁移）
- 新增 `@AutoConfiguration` 条件装配（`@ConditionalOnClass` + `@ConditionalOnProperty`），通过 `AutoConfiguration.imports` 注册
- 在 `CommonErrorCode` 中新增 Client 相关错误码（`CACHE_OPERATION_FAILED` / `OSS_OPERATION_FAILED` / `OSS_UPLOAD_FAILED` / `EMAIL_SEND_FAILED` / `SMS_SEND_FAILED` / `SEARCH_OPERATION_FAILED`）
- 现有 134 个测试全部迁移到 `app` 模块，`clients` 新增对应的单元测试

## Capabilities

### New Capabilities

- `client-cache-module`: Cache 中间件模块 — CacheClient 接口 + AbstractCacheClient 模板方法 + CaffeineCacheClient 真实实现 + CacheAutoConfiguration 条件装配
- `client-oss-module`: OSS 中间件模块 — OssClient 接口 + AbstractOssClient 模板方法 + LocalOssClient 本地文件系统实现 + OssAutoConfiguration 条件装配
- `client-email-module`: Email 中间件模块 — EmailClient 接口 + AbstractEmailClient 模板方法 + JakartaEmailClient 真实实现 + EmailAutoConfiguration 条件装配
- `client-sms-module`: SMS 中间件模块 — SmsClient 接口 + AbstractSmsClient 模板方法 + Properties 配置（无真实实现，供后续扩展）
- `client-search-module`: Search 中间件模块 — SearchClient 接口 + SimpleSearchClient 简单实现 + SearchAutoConfiguration 条件装配
- `multi-module-structure`: 多模块项目结构 — 根 POM 聚合 + clients 子模块 + app 子模块 + 依赖管理

### Modified Capabilities

## Impact

- **项目结构**：从单模块变为多模块（根 POM + common + clients + app），所有 `mvn` 命令需要在根目录执行
- **依赖关系**：common 作为所有模块的公共依赖（异常体系），clients 依赖 common，app 依赖 common + clients。第三方 SDK（caffeine、jakarta-mail）在 clients 模块中标记 `optional`
- **配置变更**：Properties 前缀从分散的 `cache.*` / `email.*` 等统一为 `middleware.cache.*` / `middleware.email.*` 等
- **Bean 装配**：从 `TechClientConfigure` 手动注册改为 `@AutoConfiguration` 单层条件装配，无 fallback 层
- **异常体系**：`CommonErrorCode` 新增 6 个 Client 错误码
- **测试**：现有测试迁移到 app 模块（调整 import），clients 模块新增单元测试
- **文档**：AGENTS.md、architecture/overview.md、getting-started.md 更新
