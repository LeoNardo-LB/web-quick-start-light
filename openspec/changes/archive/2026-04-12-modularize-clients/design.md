## Context

`web-quick-start-light` 是一个基于 Spring Boot 3.5.6 + Java 25 的三层架构 Maven Archetype 脚手架项目。当前状态：

- 单模块项目（`packaging=jar`），134 个测试全部通过
- 5 个 Client 接口（Cache/Email/SMS/OSS/Search）在 `client/` 包下，其中 Cache/OSS/Search 已有真实实现（Caffeine/LocalFS/ConcurrentHashMap），Email 基于 Jakarta Mail，SMS 仅有接口+抽象基类无真实实现
- 8 个 DTO（EmailRequest/EmailResult/SmsRequest/SmsResult/OssUploadRequest/OssUploadResult/SearchQuery/SearchResult）
- 5 个 Properties 配置类在 `config/properties/` 下，前缀分散（`cache.*`、`email.*` 等）
- 业务层（Controller/Service/Repository）尚未消费任何 Client 接口

参考项目 `speccoding-fullstack`（DDD 架构）已有成熟的中间件实现：Template Method 抽象基类、CaffeineCacheClient、LocalOssClient、JakartaEmailClient。本次设计将参考其模式适配到三层架构。

## Goals / Non-Goals

**Goals:**

- 将项目从单模块重构为多模块（`clients` + `app`），实现中间件按需接入
- 为 Cache/OSS/Email/Search 提供真实可用的实现，使脚手架开箱即用
- 引入 Template Method 模式统一 Client 层的质量标准（参数校验/日志/异常包装）
- 通过 `@AutoConfiguration` + `@ConditionalOnClass` + `@ConditionalOnProperty` 实现条件装配
- 保持所有现有测试通过（134 个），新增 clients 模块单元测试

**Non-Goals:**

- 不引入 Redis / RustFS / Elasticsearch / 阿里云 SMS 等外部中间件真实接入（留作后续扩展）
- 不改变三层架构（Controller → Service → Repository）的核心分层
- 不做 Maven Archetype 生成验证（`mvn archetype:create-from-project`）
- 不引入 DDD / 六边形 / Clean Architecture
- 不为 clients 模块做独立的 Spring Boot Starter 发布
- 不修改现有的日志系统（logback 7 Appender / Micrometer / SamplingTurboFilter）

## Decisions

### D1: 多模块结构 — 3 子模块（common + clients + app）

**选择**：根 POM（`packaging=pom`）聚合 3 个子模块

**替代方案**：
- A) 7 子模块（每个 Client 独立 + 共享 autoconfigure）：更标准化但管理复杂，对 archetype 内部使用过度设计
- C) 保持单模块内部按包分包：不符合"独立出去按需接入"的需求

**理由**：用户确认仅在 archetype 内部使用，不需要独立版本管理。`common` 模块独立承载异常体系（ErrorCode / CommonErrorCode / BaseException / BizException / ClientException / SysException），作为所有模块的公共依赖。`clients` 和 `app` 均依赖 `common`。3 子模块平衡了模块化和简洁性，与 speccoding 项目的 infrastructure 模块角色类似。

### D2: Template Method 模式 — 参考 speccoding

**选择**：每个 Client 采用 接口 → AbstractXxxClient（final 模板方法）→ 具体实现 三层结构

**理由**：
- speccoding 项目已验证此模式的价值：抽象基类统一处理参数校验、日志、异常包装
- 具体实现类仅覆写 `do*` 扩展点方法（如 `doGet`/`doPut`/`doUpload`），代码更聚焦
- 异常统一包装为 `ClientException`，错误码来自 `CommonErrorCode`

### D3: Bean 装配策略 — @AutoConfiguration 单层装配

**选择**：各 clients 子模块通过 `@AutoConfiguration` + `@ConditionalOnClass` + `@ConditionalOnProperty` 注册真实实现，无 fallback 层。

**替代方案**：speccoding 的纯手动 `@Configuration` + `@Bean` 模式；双层装配（AutoConfiguration + Logging fallback）

**理由**：虽然参考项目使用手动装配，但 `@AutoConfiguration` 通过 `AutoConfiguration.imports` 注册是 Spring Boot 3.x 的标准做法，提供了更好的条件装配能力（classpath 检测 + 配置开关），且未来扩展为独立 Starter 时无需重构。Logging fallback 层已移除——项目作为脚手架，clients 模块引入即代表需要对应能力，不需要空实现占位。

### D4: Properties 前缀统一为 middleware.*

**选择**：所有中间件配置使用 `middleware.*` 前缀

| 前缀 | 说明 |
|------|------|
| `middleware.cache` | 缓存配置 |
| `middleware.object-storage` | 对象存储配置 |
| `middleware.email` | 邮件配置 |
| `middleware.sms` | 短信配置 |
| `middleware.search` | 搜索配置 |

**理由**：与 speccoding 项目保持一致，统一命名空间避免与 Spring 官方 `spring.*` 冲突。

### D5: Cache 实现 — CaffeineCacheClient

**选择**：基于 Caffeine `Cache<String, CacheValueWrapper>`，自定义 `CaffeineExpiry` 实现每个 entry 独立过期时间

**关键设计**（参考 speccoding）：
- `CacheValueWrapper`：持有 `volatile Object value` + `volatile long expiryTimestamp`，线程安全
- `CaffeineExpiry`：实现 Caffeine 的 `Expiry<K, V>` 接口，基于 `CacheValueWrapper.expiryTimestamp` 计算剩余 TTL
- `keys(pattern)` 方法：通过 `cache.asMap().keySet()` + glob 匹配

### D6: OSS 实现 — LocalOssClient

**选择**：本地文件系统实现，NIO 零拷贝 + 日期分层存储

**关键设计**（参考 speccoding）：
- 存储路径：`{localPath}/yyyy/MM/{timestamp}-{filename}`
- 上传/下载：`FileChannel.transferTo()` 零拷贝
- 文件元数据：内存 Map 索引（不支持跨重启持久化，开发环境足够）
- `searchFiles(pattern)`：遍历文件系统，glob 匹配文件名

### D7: Email 实现 — JakartaEmailClient

**选择**：基于 `spring-boot-starter-mail` 的 `JavaMailSender`

**关键设计**：
- 单发：`JavaMailSender.send(MimeMessage)`
- 批量：stream + 逐个发送，收集部分失败结果
- 开发环境搭配 MailHog/Mailpit（SMTP 端口 1025）

### D8: Search 实现 — SimpleSearchClient

**选择**：基于 `ConcurrentHashMap<String, Map<String, Object>>` 的内存实现

**关键设计**：
- 每个 indexName 对应一个 Map
- `search()` 做简单的关键词字符串匹配（遍历所有 document 的 value）
- 仅用于开发/测试环境

### D9: SMS — 仅接口+抽象基类，无真实实现

**选择**：不提供真实实现，仅提供 `SmsClient` 接口和 `AbstractSmsClient` 抽象基类供后续扩展

**理由**：短信服务需要云服务商账号（阿里云/腾讯云），开发环境不便于测试。创建 `AbstractSmsClient` 为后续扩展预留。LoggingSmsClient 空实现已移除——不使用 SMS 功能时不注入 SmsClient Bean 即可。

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|---------|
| [BREAKING] 单模块→多模块，现有 CI/构建脚本需调整 | 构建命令从 `mvn test` 不变（根 POM 聚合自动构建子模块），但 IDE 需重新导入 |
| Properties 前缀变更（`cache.*` → `middleware.cache.*`）导致现有配置失效 | 由于 Properties 目前未被任何实现引用（仅预留），无实际影响 |
| clients 作为必选依赖，引入后所有 client 接口都在 classpath | 各 Client Bean 通过 `@AutoConfiguration` + `@ConditionalOnProperty` 按需注册，未启用的 Client 不会有 Bean 注入 |
| SimpleSearchClient 的内存实现不适合生产 | 通过 `@ConditionalOnProperty` + `@ConditionalOnClass` 控制，生产环境不启用 |
| 多模块后 ArchUnit 架构测试需要调整扫描范围 | 使用 `Architectures.layeredArchitecture()` 明确指定两个模块的包路径 |
