## ADDED Requirements

### Requirement: SystemConfig 实体移植
SHALL 从 speccoding 移植 SystemConfig 实体到 `app/.../entity/system/` 包，去除 DDD 聚合根依赖。实体 SHALL 包含 configKey、configValue、valueType、groupCode、displayName、description、inputType、inputConfig、sort 字段。

#### Scenario: SystemConfig 实体可实例化
- **WHEN** 创建 SystemConfig 实例
- **THEN** 包含 configKey/configValue/valueType/groupCode/displayName/description/inputType/inputConfig/sort 字段

### Requirement: 值对象改为 record
ConfigKey、ConfigValue、DisplayName SHALL 从 DDD 值对象（继承 ValueObject 抽象类）改为 Java record。每个 record SHALL 包含静态工厂方法 `of()` 和校验逻辑。

#### Scenario: ConfigKey 为 record
- **WHEN** 查看 ConfigKey 类定义
- **THEN** 它是 Java record，包含 `value` 字段和 `of()` 静态工厂方法

### Requirement: 枚举移植
ConfigGroup、ValueType、InputType 枚举 SHALL 从 speccoding 移植，保持枚举值不变。

#### Scenario: ConfigGroup 枚举正确
- **WHEN** 查看 ConfigGroup 枚举
- **THEN** 包含 BASIC、EMAIL、STORAGE、SECURITY 四个枚举值

### Requirement: 仓储层实现
SHALL 创建 SystemConfigRepository 接口和 SystemConfigRepositoryImpl 实现。Repository SHALL 支持 findByConfigKey、findByGroupCode、findAll、save 操作。

#### Scenario: 按 key 查询配置
- **WHEN** 调用 repository.findByConfigKey("site.name")
- **THEN** 返回对应的 SystemConfig 实体

#### Scenario: 按分组查询配置
- **WHEN** 调用 repository.findByGroupCode(ConfigGroup.BASIC)
- **THEN** 返回该分组下所有未删除的配置

### Requirement: DO 和 Mapper 由代码生成器生成
SystemConfigDO 和 SystemConfigMapper SHALL 由 MybatisPlusGenerator 生成（在 Change 2 中执行），位于 `app/.../generated/` 包下。

#### Scenario: DO 和 Mapper 已生成
- **WHEN** 查看 `app/.../generated/entity/` 和 `app/.../generated/mapper/`
- **THEN** 包含 SystemConfigDO.java 和 SystemConfigMapper.java

### Requirement: Converter 实现
SHALL 创建 SystemConfigConverter（`@Component`），实现 DO ↔ Entity 的双向转换。转换逻辑 SHALL 与 speccoding 的 SystemConfigConverter 一致。

#### Scenario: DO 转 Entity
- **WHEN** 调用 converter.toEntity(systemConfigDO)
- **THEN** 返回包含正确字段值的 SystemConfig 实体

### Requirement: Service 层合并 CQRS
SHALL 创建 SystemConfigService（合并 Command 和 Query），包含查询配置（按 key、按分组、全部）和更新配置值的方法。查询方法 SHALL 标注 `@Transactional(readOnly=true)`，更新方法 SHALL 标注 `@Transactional`。

#### Scenario: 查询所有配置
- **WHEN** 调用 service.findAllConfigs()
- **THEN** 返回所有未删除的配置列表，按 groupCode 分组

#### Scenario: 更新配置值
- **WHEN** 调用 service.updateConfig("site.name", "New Name")
- **THEN** 对应配置的 configValue 更新，updateTime 自动填充

### Requirement: VO 和 Command 使用 record
SystemConfigVO、ConfigGroupVO、UpdateConfigCommand、UpdateConfigRequest SHALL 创建为 Java record。

#### Scenario: VO 为 record
- **WHEN** 查看 SystemConfigVO 类定义
- **THEN** 它是 Java record

### Requirement: REST API 端点
SHALL 创建 SystemConfigController，提供以下端点：
- GET `/api/system/configs` — 查询所有配置（按分组）
- GET `/api/system/configs/groups` — 查询配置分组列表
- GET `/api/system/configs/{key}` — 按 key 查询配置
- PUT `/api/system/configs/{key}` — 更新配置值

#### Scenario: 查询所有配置
- **WHEN** GET /api/system/configs
- **THEN** 返回 HTTP 200，body 包含按分组组织的配置列表

#### Scenario: 更新配置值
- **WHEN** PUT /api/system/configs/site.name，body 为 {"value": "New Name"}
- **THEN** 返回 HTTP 200，配置值已更新

#### Scenario: 查询不存在的配置
- **WHEN** GET /api/system/configs/nonexistent
- **THEN** 返回 HTTP 404
