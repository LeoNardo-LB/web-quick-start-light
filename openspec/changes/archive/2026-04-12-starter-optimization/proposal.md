## Why

当前 client 模块已具备完整的 Spring Boot Starter 机制（`@AutoConfiguration` + `.imports` + 条件装配），但 app 模块中仍保留了 4 个冗余的 Configure 类（CacheConfigure、OssConfigure、NotificationConfigure、SearchConfigure）做重复的 Bean 注册。此外，客户端 Properties 缺少默认值（依赖 yaml 提供），日志系统与 app 耦合不便复用。

## What Changes

- **删除 app 模块中 4 个冗余的 Configure 类**：CacheConfigure、OssConfigure、NotificationConfigure、SearchConfigure。这些类与 client 模块自带的 `*AutoConfiguration` 完全重复。
- **为每个 client 模块的 Properties 类添加默认值**：通过 `@DefaultValue` 或字段初始化提供合理默认值，使"引入依赖即可用"无需额外 yaml 配置。
- **为每个 client 模块添加 `spring-boot-autoconfigure-processor` 依赖**：自动生成 `spring-configuration-metadata.json`，IDE 可自动补全 `middleware.*` 配置项。
- **将日志系统抽取为独立 starter 模块 `client-log`**：把 app 模块中的日志注解（BusinessLog）、切面（LogAspect）、配置（LoggingConfigure/LoggingConfiguration/SamplingTurboFilter/SlowQueryInterceptor 等）移至新的 client-log 模块，实现"引入即用"。

## Capabilities

### New Capabilities
- `client-log-starter`: 日志系统独立 starter 模块，包含 BusinessLog 注解、LogAspect 切面、日志配置类、SamplingTurboFilter、SlowQueryInterceptor、LogMarkers、SensitiveLogUtils 等，app 引入即可自动装配

### Modified Capabilities
- `client-modules-replacement`: 删除 app 冗余 Configure 类，Properties 增加默认值，添加 autoconfigure-processor 依赖
- `logging-system-replacement`: 日志核心代码从 app 迁移至 client-log starter，app 仅保留日志配置属性（yaml）

## Impact

- **代码删除**：app 模块 4 个 Configure 类（CacheConfigure、OssConfigure、NotificationConfigure、SearchConfigure）
- **代码迁移**：app 中日志相关 Java 类 → client-log 模块
- **依赖变更**：5 个 client 模块 pom.xml 新增 `spring-boot-autoconfigure-processor`；新增 client-log 子模块；app/pom.xml 新增 client-log 依赖
- **yaml 配置**：不变（已有 middleware.* 配置），但新增的默认值使配置变为可选
- **测试**：需验证删除冗余 Configure 后所有 Bean 正常注入，日志 starter 迁移后功能不变
