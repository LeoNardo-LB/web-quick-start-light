## 0. Pre-flight

- [x] 0.1 确认 test-driven-development、dispatching-parallel-agents 技能已加载（未加载则加载，已加载则跳过）

## 1. Change 1: 基础设施层 — 依赖升级

- [x] 1.1 RED: 编写依赖版本验证测试（确认 Spring Boot、MyBatis-Plus、Guava 等核心依赖版本与目标一致，通过加载类并检查包版本来验证）
- [x] 1.2 GREEN: 修改根 pom.xml — parent 升级到 spring-boot-starter-parent:4.0.2，更新所有 properties 版本（mybatis-plus.version=3.5.16、springdoc.version=2.8.17、guava.version=33.5.0-jre、commons-lang3.version=3.20.0、commons-io.version=2.21.0、commons-collections4.version=4.5.0、kryo.version=5.6.2、commons-pool2.version=2.13.1、fastjson2.version=2.0.61、hutool.version=5.8.44、sqlite.version=3.51.3.0）
- [x] 1.3 GREEN: 修改 app/pom.xml — 新增 mybatis-plus-generator:3.5.16、freemarker:2.3.34、logstash-logback-encoder:9.0 依赖；移除 kryo-shaded；caffeine 版本跟随 BOM
- [x] 1.4 GREEN: 修改 clients/client-search/pom.xml — fastjson2 版本升级到 2.0.61
- [x] 1.5 GREEN: 修改根 pom.xml — 移除 hutool-bom import，改为直接在 properties 中管理 hutool.version=5.8.44
- [x] 1.6 REFACTOR: 修复 Spring Boot 4.0.2 升级导致的编译错误（Jakarta EE 11 命名空间变更、Spring Framework 7 API 变更等）
- [x] 1.7 GREEN: 修改 AuditEvent.java — timestamp 字段从 LocalDateTime 改为 Instant，更新所有静态工厂方法
- [x] 1.8 GREEN: 修改 BaseResult.java — time 字段从 Long 改为 Instant，success()/fail() 工厂方法使用 Instant.now()
- [x] 1.9 REFACTOR: 修复 Instant 变更导致的测试编译错误，更新所有引用 AuditEvent.timestamp 和 BaseResult.time 的测试代码
- [x] 1.10 REFACTOR: 修复 application.yaml 配置以适配 Spring Boot 4.0.2（检查 deprecated 配置项、新增 spring.application.name）
- [x] 1.11 TEST: 执行 mvn clean compile 确认编译通过，执行 mvn test 确认所有测试通过

## 2. Change 1: 基础设施层 — 文档更新

- [x] 2.1 更新 AGENTS.md — 技术栈表格版本号更新（Spring Boot 4.0.2、MyBatis-Plus 3.5.16 等），新增 Instant 规范（所有时间存储与传输统一使用 java.time.Instant）

## 3. Change 2: DAL 层 — 代码生成器

- [x] 3.1 RED: 编写 MybatisPlusGenerator 存在性测试（验证 MybatisPlusGenerator 类存在于 classpath，可被实例化）
- [x] 3.2 GREEN: 移植 MybatisPlusGenerator.java 到 app/.../generated/ 包 — 从 speccoding 移植，适配包路径为 org.smm.archetype.generated，默认 DB_URL 改为 jdbc:sqlite:./data/app.db，TARGET_TABLES 改为 {"system_config"}，SOURCE_DIR 指向 app/src/main/java
- [x] 3.3 REFACTOR: 确认 BaseDO.java 和 MyMetaObjectHandler.java 与 SQLite 兼容（@TableId(type=ASSIGN_ID) 在 SQLite 下的行为）
- [x] 3.4 TEST: 验证 MybatisPlusGenerator 类可编译、可实例化（不要求连接数据库运行）

## 4. Change 2: DAL 层 — SQL 文件

- [x] 4.1 RED: 编写 SQL 初始化测试（验证应用启动后 system_config 表存在且有 15 条初始数据）
- [x] 4.2 GREEN: 创建 app/src/main/resources/schema.sql — system_config 表 DDL（SQLite 语法：INTEGER PRIMARY KEY AUTOINCREMENT、TEXT 替代 JSON/TIMESTAMP、无 ENGINE/CHARSET），包含表模板注释
- [x] 4.3 GREEN: 创建 app/src/main/resources/init.sql — 15 条系统配置初始数据（BASIC 4条、EMAIL 4条、STORAGE 3条、SECURITY 4条），使用 datetime('now') 替代 NOW()
- [x] 4.4 GREEN: 配置 application.yaml — 添加 spring.sql.init 配置确保 SQL 文件自动执行（schema-locations、data-locations、mode、continue-on-error）
- [x] 4.5 TEST: 启动应用，验证 system_config 表自动创建且包含 15 条初始数据

## 5. Change 2: DAL 层 — 文档更新

- [x] 5.1 更新 AGENTS.md — 新增"代码生成器规范"章节（代码生成器位置、生成代码禁止手动修改、使用环境变量配置数据库连接）

## 6. Change 3: 日志系统 — 删除现有

- [x] 6.1 删除 app/.../config/logging/ 目录下所有文件（LoggingConfigure、LoggingConfiguration、SamplingTurboFilter、SlowQueryInterceptor、LogMarkers、SensitiveLogUtils）
- [x] 6.2 删除 app/.../config/properties/LoggingProperties.java
- [x] 6.3 删除 app/.../util/log/ 目录下所有文件（BizLog.java、BizLogAspect.java）
- [x] 6.4 删除 app/.../service/log/ 目录下所有文件（AuditLogService.java、AuditEvent.java）
- [x] 6.5 删除 app/.../controller/global/ContextFillFilter.java
- [x] 6.6 删除 app/src/main/resources/logback-spring.xml（将用 speccoding 版本替换）

## 7. Change 3: 日志系统 — 移植 speccoding

- [x] 7.1 RED: 编写日志系统组件存在性测试（验证 BusinessLog 注解、LogAspect、LogMarkers、SamplingTurboFilter、SlowQueryInterceptor、SensitiveLogUtils、LoggingConfiguration、LoggingProperties、MemoryLogAppender 存在于 classpath）
- [x] 7.2 GREEN: 移植 BusinessLog.java 到 app/.../log/ — 从 speccoding infrastructure/shared/log/ 移植，包路径改为 org.smm.archetype.log
- [x] 7.3 GREEN: 移植 MethodExecutionLog.java 到 app/.../log/ — 适配为独立类（@Getter @Builder），不继承 Entity/BaseDO，包路径改为 org.smm.archetype.log
- [x] 7.4 GREEN: 移植 LogAspect.java 到 app/.../log/ — 适配包路径，修改 MethodExecutionLog 引用，修改 Micrometer 依赖引用
- [x] 7.5 GREEN: 移植 LogConfigure.java 到 app/.../config/ — 创建 LogAspect Bean，包路径改为 org.smm.archetype.config
- [x] 7.6 GREEN: 移植 LoggingProperties.java 到 app/.../config/properties/ — 包路径改为 org.smm.archetype.config.properties
- [x] 7.7 GREEN: 移植 LoggingConfigure.java 到 app/.../config/logging/ — 注册 LoggingConfiguration、SlowQueryInterceptor、SamplingTurboFilter Bean，适配包路径
- [x] 7.8 GREEN: 移植 LoggingConfiguration.java 到 app/.../config/logging/ — 启动验证（日志目录/磁盘空间），适配包路径
- [x] 7.9 GREEN: 移植 SamplingTurboFilter.java 到 app/.../config/logging/ — 高并发日志采样，ERROR 始终记录
- [x] 7.10 GREEN: 移植 SlowQueryInterceptor.java 到 app/.../config/logging/ — MyBatis 慢 SQL 拦截，记录到 SLOW_QUERY logger
- [x] 7.11 GREEN: 移植 LogMarkers.java 到 app/.../config/logging/ — 5 个业务日志标记（ORDER/PAYMENT/USER/SECURITY/AUDIT）
- [x] 7.12 GREEN: 移植 SensitiveLogUtils.java 到 app/.../config/logging/ — 日志脱敏工具，确认 hutool-core 可用（StrUtil）
- [x] 7.13 GREEN: 移植 ContextFillFilter.java 到 app/.../controller/global/ — 从 speccoding adapter/web/config/ 移植，包路径适配
- [x] 7.14 GREEN: 移植 logback-spring.xml 到 app/src/main/resources/ — 8 个 Appender（CONSOLE、ASYNC_FILE、ASYNC_JSON_FILE、ASYNC_CURRENT、ERROR_FILE、WARN_FILE、SLOW_QUERY_FILE、AUDIT_FILE），适配 ${spring.application.name}
- [x] 7.15 GREEN: 移植 MemoryLogAppender.java 到 app/src/test/.../support/ — 测试日志收集
- [x] 7.16 REFACTOR: 更新所有引用 @BizLog 的代码改为 @BusinessLog（TestController 等）
- [x] 7.17 REFACTOR: 更新 logback-test.xml（app/src/test/resources/）适配新日志系统
- [x] 7.18 TEST: 执行 mvn clean compile 确认编译通过，执行 mvn test 确认所有测试通过

## 8. Change 3: 客户端 — CacheClient 替换

- [x] 8.1 RED: 编写 CacheClient 接口方法数测试（验证 CacheClient 接口包含 10 个方法，AbstractCacheClient 的 public 方法为 final）
- [x] 8.2 GREEN: 替换 client-cache 模块 — CacheClient 接口（10 方法）、AbstractCacheClient（final 公开方法 + do* 扩展点）、CaffeineCacheClient（含 CacheValueWrapper + CaffeineExpiry），替换现有实现
- [x] 8.3 REFACTOR: 更新 CacheAutoConfiguration 适配新 CaffeineCacheClient 构造函数（接受 initialCapacity、maximumSize、expireAfterWrite 参数）
- [x] 8.4 REFACTOR: 更新 CacheProperties 适配配置项（initialCapacity=1000、maximumSize=10000、expireAfterWrite=30d、expireAfterAccess=30d）
- [x] 8.5 TEST: 执行 mvn test -pl clients/client-cache 确认 client-cache 模块测试通过

## 9. Change 3: 客户端 — OssClient 替换

- [x] 9.1 RED: 编写 OssClient 接口方法数测试（验证 OssClient 接口包含 7 个方法）
- [x] 9.2 GREEN: 替换 client-oss 模块 — OssClient 接口（7 方法）、AbstractOssClient（final + do* 扩展点）、LocalOssClient（NIO 零拷贝 + 日期分层 + searchFiles/exists/getFileSize），替换现有实现
- [x] 9.3 REFACTOR: 更新 OssAutoConfiguration 适配新实现，新增 FileMetadata DTO
- [x] 9.4 TEST: 执行 mvn test -pl clients/client-oss 确认 client-oss 模块测试通过

## 10. Change 3: 客户端 — EmailClient 替换

- [x] 10.1 GREEN: 替换 client-email 模块 — EmailClient 接口（3 方法 + ServiceProvider）、AbstractEmailClient、NoOpEmailClientImpl（替换 JakartaEmailClient，无真实 SDK 调用）
- [x] 10.2 GREEN: 更新 client-email/pom.xml — 保留 spring-boot-starter-mail（optional），不添加阿里云 SDK
- [x] 10.3 GREEN: 新增 ServiceProvider 枚举到 client-email 模块的 dto 包
- [x] 10.4 REFACTOR: 更新 EmailAutoConfiguration 条件装配需 middleware.email.enabled=true，默认 NoOp 实现
- [x] 10.5 TEST: 执行 mvn compile -pl clients/client-email 确认 client-email 模块编译通过

## 11. Change 3: 客户端 — SmsClient 替换

- [x] 11.1 GREEN: 替换 client-sms 模块 — SmsClient 接口（3 方法 + ServiceProvider）、AbstractSmsClient、NoOpSmsClientImpl（无真实 SDK 调用）
- [x] 11.2 GREEN: 更新 client-sms/pom.xml — 不添加阿里云 SDK
- [x] 11.3 REFACTOR: 更新 SmsAutoConfiguration 注册 NoOpSmsClientImpl Bean，条件装配需 middleware.sms.enabled=true
- [x] 11.4 TEST: 执行 mvn compile -pl clients/client-sms 确认 client-sms 模块编译通过

## 12. Change 3: 客户端 — SearchClient 替换

- [x] 12.1 GREEN: 替换 client-search 模块 — SearchClient 接口（15 方法）、AbstractSearchClient（final + 14 do* 扩展点）、SimpleSearchClient（ConcurrentHashMap 内存实现），替换现有 SimpleSearchClient
- [x] 12.2 GREEN: 更新 client-search/pom.xml — 不添加 Elasticsearch 依赖
- [x] 12.3 REFACTOR: 更新 SearchAutoConfiguration 适配 SimpleSearchClient，条件装配
- [x] 12.4 TEST: 执行 mvn compile -pl clients/client-search 确认 client-search 模块编译通过

## 13. Change 3: 客户端 — 配置类移植

- [x] 13.1 GREEN: 移植 CacheConfigure.java 到 app/.../config/ — 创建 CaffeineCacheClient Bean，注入 CacheProperties
- [x] 13.2 GREEN: 移植 OssConfigure.java 到 app/.../config/ — 创建 LocalOssClient Bean
- [x] 13.3 GREEN: 移植 NotificationConfigure.java 到 app/.../config/ — 条件装配 NoOpEmailClient 和 NoOpSmsClient
- [x] 13.4 GREEN: 移植 SearchConfigure.java 到 app/.../config/ — 条件装配 SimpleSearchClient
- [x] 13.5 TEST: 执行 mvn clean compile + mvn test 确认所有客户端模块和 app 模块编译测试通过（ITest 因 Change 4 未完成的 SystemConfigConverter 编译错误而失败，非本次变更范围）

## 14. Change 4: 系统配置 — 实体层

- [x] 14.1 RED: 编写 SystemConfig 实体测试（验证实体包含 configKey/configValue/valueType/groupCode/displayName/description/inputType/inputConfig/sort 字段）
- [x] 14.2 GREEN: 创建 app/.../entity/system/SystemConfig.java — 从 speccoding 移植，去除 AggregateRoot 依赖，保留所有业务字段
- [x] 14.3 GREEN: 创建 ConfigKey.java（record）、ConfigValue.java（record）、DisplayName.java（record）— 值对象简化为 record，含静态工厂 of() 和校验
- [x] 14.4 GREEN: 创建 ConfigGroup.java（枚举：BASIC/EMAIL/STORAGE/SECURITY）、ValueType.java（枚举：STRING/INTEGER/DECIMAL/BOOLEAN/ENUM/ARRAY/JSON）、InputType.java（枚举：TEXT/TEXTAREA/NUMBER/SWITCH/SELECT/MULTI_SELECT/JSON_EDITOR）

## 15. Change 4: 系统配置 — DAL 层（代码生成器）

- [x] 15.1 运行 MybatisPlusGenerator 生成 SystemConfigDO.java 和 SystemConfigMapper.java（确认 schema.sql 中 system_config 表已存在）
- [x] 15.2 REFACTOR: 手动检查生成代码，确认字段映射正确（特别是 SQLite TEXT 类型与 Java String 的映射）

## 16. Change 4: 系统配置 — 仓储层

- [x] 16.1 RED: 编写 SystemConfigRepository 测试（验证 findByConfigKey、findByGroupCode、findAll、save 操作，使用真实 SQLite 数据库）
- [x] 16.2 GREEN: 创建 app/.../repository/system/SystemConfigRepository.java 接口（findByConfigKey、findByGroupCode、findAll、save）
- [x] 16.3 GREEN: 创建 app/.../repository/system/SystemConfigRepositoryImpl.java — 实现 Repository 接口，注入 SystemConfigMapper
- [x] 16.4 GREEN: 创建 app/.../repository/system/SystemConfigConverter.java（@Component）— DO ↔ Entity 双向转换
- [x] 16.5 TEST: 执行 SystemConfigRepository 测试，验证 CRUD 操作通过

## 17. Change 4: 系统配置 — 服务层

- [x] 17.1 RED: 编写 SystemConfigService 测试（验证 findAllConfigs 按分组返回、findByConfigKey 返回正确配置、updateConfig 更新 configValue 和 updateTime）
- [x] 17.2 GREEN: 创建 SystemConfigVO.java（record）、ConfigGroupVO.java（record）、UpdateConfigCommand.java（record）
- [x] 17.3 GREEN: 创建 SystemConfigService.java — 合并 CQRS（findAllConfigs、findByConfigKey、findByGroupCode、updateConfig），查询方法 @Transactional(readOnly=true)，更新方法 @Transactional
- [x] 17.4 TEST: 执行 SystemConfigService 测试，验证业务逻辑正确

## 18. Change 4: 系统配置 — 控制器层

- [x] 18.1 RED: 编写 SystemConfigController 集成测试（验证 GET /api/system/configs 返回 200、GET /api/system/configs/groups 返回 200、GET /api/system/configs/{key} 返回 200/404、PUT /api/system/configs/{key} 返回 200）
- [x] 18.2 GREEN: 创建 UpdateConfigRequest.java（record）
- [x] 18.3 GREEN: 创建 SystemConfigController.java — 4 个端点（GET configs、GET groups、GET config by key、PUT config），注入 SystemConfigService
- [x] 18.4 TEST: 执行 SystemConfigController 集成测试，验证所有 API 端点通过

## 19. 文档更新

- [x] 19.1 更新 AGENTS.md — 日志系统描述更新（@BusinessLog 替代 @BizLog、8 个 Appender）、客户端描述更新（NoOp 默认实现 + 条件装配）、新增系统配置描述（实体/仓储/服务/控制器）
- [x] 19.2 更新 docs/architecture/overview.md — 架构描述更新（新增 DAL 层、代码生成器、系统配置、日志系统详情）
- [x] 19.3 更新 docs/backend/coding-standards.md — 新增 record 规范（DTO/VO/Result 使用 record）、Instant 规范、代码生成器规范
- [x] 19.4 更新 docs/guides/getting-started.md — 版本号更新、新增系统配置 API 说明、SQL 初始化说明
- [x] 19.5 更新 README.md — 特性列表更新（新增系统配置、代码生成器、增强版客户端、完整日志系统）

## 20. 最终验证

- [x] 20.1 执行 mvn clean compile — 确认全量编译通过
- [x] 20.2 执行 mvn test — 确认所有测试通过
- [x] 20.3 执行 mvn spring-boot:run -pl app — 确认应用启动成功（无 ES/阿里云 配置时客户端 Bean 不注册但不报错）
- [x] 20.4 通过 curl 验证系统配置 API 端点（GET /api/system/configs 返回 15 条配置）

## 21. 一致性检查

- [x] 21.1 进行 artifact 文档、讨论结果的一致性检查
