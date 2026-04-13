## MODIFIED Requirements

### Requirement: 技术栈版本号更新
AGENTS.md 的技术栈表格 SHALL 更新为以下版本：
- Spring Boot: 4.0.2
- MyBatis-Plus: 3.5.16
- SpringDoc OpenAPI: 2.8.17
- Caffeine: 由 Spring Boot BOM 管理
- Hutool: 5.8.44
- SQLite: 3.51.3.0

#### Scenario: AGENTS.md 版本与 POM 一致
- **WHEN** 对比 AGENTS.md 技术栈表格中的版本号与 POM 实际版本
- **THEN** 完全一致

### Requirement: 新增代码生成器规范
AGENTS.md SHALL 新增"代码生成器规范"章节，包含以下规则：
- 代码生成器位于 `app/.../generated/` 包下
- 生成的 DO 和 Mapper 位于 `app/.../generated/entity/` 和 `app/.../generated/mapper/`
- 禁止手动修改生成代码
- 使用环境变量配置数据库连接

#### Scenario: AGENTS.md 包含代码生成器规范
- **WHEN** 阅读 AGENTS.md
- **THEN** 包含"代码生成器规范"章节，含上述所有规则

### Requirement: 新增 Instant 规范
AGENTS.md 编码规则 SHALL 新增：所有时间存储与传输统一使用 `java.time.Instant`，展示用途的时间格式化可使用 LocalDateTime/LocalDate。

#### Scenario: AGENTS.md 包含 Instant 规范
- **WHEN** 阅读 AGENTS.md 的编码规则
- **THEN** 包含 Instant 时间类型规范

### Requirement: 日志系统描述更新
AGENTS.md 的日志规范 SHALL 更新为 speccoding 版本的描述：@BusinessLog 注解（替代 @BizLog）、LogAspect 切面（替代 BizLogAspect）、8 个 Appender、SamplingTurboFilter、SlowQueryInterceptor、SensitiveLogUtils、LogMarkers。

#### Scenario: AGENTS.md 日志描述与实际代码一致
- **WHEN** 阅读 AGENTS.md 的日志规范章节
- **THEN** 引用 @BusinessLog（而非 @BizLog），描述 8 个 Appender

### Requirement: 客户端描述更新
AGENTS.md 的客户端描述 SHALL 更新：Template Method 模式保留，新增 Aliyun SDK（Email/Sms）和 Elasticsearch（Search）实现说明。

#### Scenario: AGENTS.md 客户端描述与实际代码一致
- **WHEN** 阅读 AGENTS.md 的客户端模块描述
- **THEN** 包含 AliyunEmailClientImpl、AliyunSmsClientImpl、ElasticsearchClientImpl 的说明

### Requirement: 新增系统配置描述
AGENTS.md SHALL 新增系统配置功能描述，包含实体层（SystemConfig + 值对象）、仓储层、服务层、控制器层的说明。

#### Scenario: AGENTS.md 包含系统配置描述
- **WHEN** 阅读 AGENTS.md 的项目结构索引
- **THEN** 包含 system config 相关条目
