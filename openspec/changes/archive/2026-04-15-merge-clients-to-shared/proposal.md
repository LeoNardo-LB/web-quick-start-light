## Why

`client-ratelimit`、`client-idempotent`、`client-log` 三个模块不属于中间件接入层的职责——它们是应用层横切关注点（AOP 注解 + 切面），不应伪装成
client 模块。它们的存储依赖可以通过已有的 `client-cache`（CacheClient）解决，无需自建缓存。将它们合并到 app 模块的 `shared/` 包下，能使 clients
目录回归"纯中间件封装"的定位，同时简化依赖关系。

## What Changes

- **移除 `clients/client-idempotent` 模块**：注解、切面、Key 解析器移入 `app/.../shared/aspect/idempotent/`；存储层改为依赖 CacheClient（不再自建
  Caffeine Cache）
- **移除 `clients/client-ratelimit` 模块**：注解、切面、SpEL 解析器、Bucket 工厂、枚举移入 `app/.../shared/aspect/ratelimit/`；保留 Bucket4j 本地实现
- **移除 `clients/client-log` 模块**：
    - `@BusinessLog`、`LogAspect`、`OperationLogWriter`、`OperationLogRecord`、`OperationType` 移入 `app/.../shared/aspect/operationlog/`
    - `SlowQueryInterceptor`、`SamplingTurboFilter`、`SensitiveLogUtils`、`LogMarkers`、`LoggingConfiguration` 移入 `app/.../shared/logging/`
    - `LoggingProperties` 移入 `app/.../config/properties/`
- **移除 `app/.../util/` 顶级包**：内容迁移到 `app/.../shared/util/`（context/、dal/、IpUtils 等）
- **新增 `app/.../config/` 下的配置类**：`IdempotentConfigure`、`RatelimitConfigure`、`LoggingConfigure`（使用 `@Configuration` + `@Bean` 风格，与现有
  `WebConfigure` 一致）
- **更新文档**：AGENTS.md、module-structure.md、java-conventions.md、design-patterns.md（文档已在本次探索中先行更新）
- **BREAKING**：`@BusinessLog`、`@Idempotent`、`@RateLimit` 的包路径变更（`org.smm.archetype.client.*` → `org.smm.archetype.shared.aspect.*`）

## Capabilities

### New Capabilities

- `shared-package-structure`: app 模块 `shared/` 包的组织规范——aspect/logging/util 三级子包的职责、命名、依赖规则

### Modified Capabilities

- `rate-limiting`: 从独立 client 模块迁移到 `shared/aspect/ratelimit/`；移除 AutoConfiguration 改为 app 内 @Configuration
- `idempotent-protection`: 从独立 client 模块迁移到 `shared/aspect/idempotent/`；存储层从自建 Caffeine Cache 改为依赖 CacheClient
- `logging-enhancement`: 日志基础设施从 client-log 迁移到 `shared/logging/`；操作日志 AOP 迁移到 `shared/aspect/operationlog/`
- `logging-system-replacement`: 同上，LoggingProperties 移入 config/properties/

## Impact

- **Maven 结构**：clients/pom.xml 移除 3 个 submodule；app/pom.xml 移除 3 个 client 依赖、新增 bucket4j/micrometer/fastjson2 直接依赖
- **包路径**：所有 `org.smm.archetype.client.log.*`、`org.smm.archetype.client.ratelimit.*`、`org.smm.archetype.client.idempotent.*` 引用需更新
- **测试**：三个模块的测试类需迁移到 app 测试目录，包路径同步更新
- **ArchUnit**：现有守护规则需确认 `shared` 包不违反层间依赖约束
- **Spring Boot AutoConfiguration**：`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 中移除三个 client 的条目
