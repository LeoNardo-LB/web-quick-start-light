## Why

当前项目依赖版本滞后（Spring Boot 3.5.6），日志系统和客户端实现较为简陋，缺少代码生成器、系统配置管理等核心能力。参考项目 speccoding-fullstack 已在 Spring Boot 4.0.2 + DDD 四层架构上验证了完整的中级能力，需要将其核心逻辑移植到本项目（保持三层架构），同时升级所有依赖到 Maven Central 最新稳定版。

## What Changes

### **BREAKING** 依赖全面升级
- Spring Boot 3.5.6 → 4.0.2（Spring Framework 7，Jakarta EE 11）
- MyBatis-Plus 3.5.9 → 3.5.16
- SpringDoc OpenAPI 2.8.9 → 2.8.17
- Guava 33.3.1-jre → 33.5.0-jre、commons-lang3 3.18.0 → 3.20.0、commons-io 2.17.0 → 2.21.0、commons-collections4 4.4 → 4.5.0
- Kryo 5.6.1 → 5.6.2、commons-pool2 2.12.0 → 2.13.1、Fastjson2 2.0.51 → 2.0.61
- Hutool-core 5.8.40 → 5.8.44、SQLite JDBC 3.47.2.0 → 3.51.3.0
- 新增：mybatis-plus-generator 3.5.16、Freemarker 2.3.34、logstash-logback-encoder 9.0
- 移除：kryo-shaded（与 kryo 5.x 重复）、hutool-bom（改为直接声明 hutool-core 版本）
- BOM 管理的依赖跟随 Spring Boot 4.0.2 自动升级（Caffeine、Lombok、Jackson、Mockito、AssertJ、HikariCP 等）

### **BREAKING** 时间类型统一为 Instant
- `AuditEvent.timestamp`: LocalDateTime → Instant
- `BaseResult.time`: Long → Instant
- 所有新增实体/DTO 的时间字段统一使用 Instant

### record 迁移
- 新增的 DTO/VO/Result（如 SystemConfigVO、UpdateConfigRequest 等）直接创建为 Java record
- 现有 record 保持不变，有继承链的类（BaseResult、BaseRequest、BaseDO）保持 class

### 日志系统完全替换
- 删除现有日志代码（LoggingConfigure、BizLog、BizLogAspect、AuditLogService、ContextFillFilter、logback-spring.xml 等 13 个文件）
- 移植 speccoding 的完整日志系统（BusinessLog 注解、LogAspect 切面、SamplingTurboFilter、SlowQueryInterceptor、SensitiveLogUtils、LogMarkers、MemoryLogAppender、8 个 Appender 的 logback-spring.xml）

### **BREAKING** 客户端全面替换
- 5 个独立 Maven 子模块保留，内部代码替换为 speccoding 实现
- CacheClient：4 方法 → 10 方法，CaffeineCacheClientImpl 含 Expiry 策略
- OssClient：增强版 LocalOssClientImpl + AbstractOssClient（407 行）
- EmailClient：JakartaEmailClient → AliyunEmailClientImpl（阿里云 SDK）
- SmsClient：无实现 → AliyunSmsClientImpl（阿里云 SDK）
- SearchClient：SimpleSearchClient（ConcurrentHashMap）→ ElasticsearchClientImpl
- 新增依赖：阿里云 SMS/DM SDK、Elasticsearch Java Client

### 新增 DAL 层 + 代码生成器
- 移植 MybatisPlusGenerator（适配 SQLite），放在 app 模块 generated 包下
- 代码生成器生成的 DO 和 Mapper 禁止手动修改
- 新增 schema.sql（DDL，SQLite 语法）和 init.sql（DML）

### 新增系统配置功能
- 从 speccoding 移植 SystemConfig，适配三层架构（去除 DDD 聚合根/值对象继承体系）
- 值对象改为 record，CQRS Command/Query 合并为单一 Service
- 提供 REST API：查询配置（分组/单个）、更新配置值
- 15 条初始配置数据（BASIC/EMAIL/STORAGE/SECURITY 四组）

## Capabilities

### New Capabilities
- `dependency-upgrade`: 全量依赖升级到 Maven Central 最新稳定版，包含 Spring Boot 4.0.2 大版本跳跃
- `instant-unification`: 所有时间存储与传输统一使用 java.time.Instant
- `logging-system-replacement`: 日志系统从 speccoding 完整移植替换
- `client-modules-replacement`: 5 个客户端模块用 speccoding 实现全面替换
- `dal-code-generator`: MyBatis-Plus 代码生成器移植 + schema.sql/init.sql
- `system-config`: 系统配置管理功能移植（实体/仓储/服务/控制器）

### Modified Capabilities
- `agents-md-and-docs`: AGENTS.md 技术栈版本号、代码生成器规范、Instant 规范、日志系统描述、客户端描述、系统配置描述全面更新
- `logging-enhancement`: 现有日志增强 spec 的实现方式将完全替换（不再渐进增强，而是整体替换为 speccoding 版本）

## Impact

- **代码**: 约 60 个文件新增/修改/删除，涵盖 POM 配置、日志系统、客户端模块、DAL 层、系统配置
- **API**: 新增 4 个系统配置 REST 端点；客户端接口方法签名变化（CacheClient 4→10 方法）影响使用方
- **依赖**: Spring Boot 4.0.2 大版本升级影响全局编译；新增阿里云 SDK、Elasticsearch、Freemarker、logstash-encoder
- **配置**: application.yaml 需新增 spring.sql.init、spring.application.name 等配置项
- **测试**: 现有测试需要适配 API 变化；新增 MemoryLogAppender 测试日志收集
- **文档**: AGENTS.md、overview.md、coding-standards.md、getting-started.md、README.md 全部需更新
