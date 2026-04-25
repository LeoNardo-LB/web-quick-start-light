# 系统配置领域

> **职责**: 描述系统配置领域的 API、值对象和服务流程
> **轨道**: Contract
> **维护者**: AI

## 目录

- [概述](#概述)

## 概述

系统配置领域是应用中最丰富的业务领域，展示了完整的 DDD-lite 分层架构实践。`SystemConfigController` 提供 6 个 REST 端点（列表/分组/分页/按 Key/按分组/更新），通过 `SystemConfigFacade` 封装 Entity→VO 转换。领域层采用 DDD 值对象模式：`ConfigKey`、`ConfigValue`、`DisplayName` 三个不可变 record 作为值对象，`ConfigGroup`、`ValueType`、`InputType` 枚举携带 UI 元数据，`SystemConfig` 聚合根提供 `updateValue()` 领域行为。Repository 层通过 `SystemConfigConverter` 实现 DO↔Entity 双向转换。

## 公共 API 参考

### SystemConfigController — 系统配置控制器

```java
@Slf4j
@RestController
@RequestMapping("/api/system/configs")
@RequiredArgsConstructor
@Tag(name = "系统配置")
@Validated
public class SystemConfigController {

    private final SystemConfigFacade systemConfigFacade;

    @GetMapping
    public BaseResult<List<SystemConfigVO>> getAllConfigs();

    @GetMapping("/groups")
    public BaseResult<List<ConfigGroupVO>> getAllGroups();

    @GetMapping("/page")
    public BasePageResult<SystemConfigVO> findByPage(@Valid @ModelAttribute SystemConfigPageQuery query);

    @GetMapping("/{key}")
    public BaseResult<SystemConfigVO> getConfigByKey(@PathVariable String key);

    @GetMapping("/group/{code}")
    public BaseResult<List<SystemConfigVO>> getConfigsByGroup(@PathVariable String code);

    @PutMapping("/{key}")
    public BaseResult<SystemConfigVO> updateConfig(
            @PathVariable String key,
            @Valid @RequestBody UpdateConfigRequest request);
}
```

#### REST 端点

| HTTP 方法 | 路径 | 参数 | 返回类型 | 说明 |
|:---------:|------|------|---------|------|
| GET | `/api/system/configs` | 无 | `BaseResult<List<SystemConfigVO>>` | 获取所有配置 |
| GET | `/api/system/configs/groups` | 无 | `BaseResult<List<ConfigGroupVO>>` | 获取配置分组（枚举，不查数据库） |
| GET | `/api/system/configs/page` | `SystemConfigPageQuery` (query) | `BasePageResult<SystemConfigVO>` | 分页查询配置 |
| GET | `/api/system/configs/{key}` | `key` (path) | `BaseResult<SystemConfigVO>` | 按 Key 获取配置 |
| GET | `/api/system/configs/group/{code}` | `code` (path) | `BaseResult<List<SystemConfigVO>>` | 按分组获取配置 |
| PUT | `/api/system/configs/{key}` | `key` (path) + `UpdateConfigRequest` (body) | `BaseResult<SystemConfigVO>` | 更新配置值 |

### SystemConfigFacade — 系统配置 Facade 接口

```java
package org.smm.archetype.facade.system;

public interface SystemConfigFacade {
    List<ConfigGroupVO> getAllGroups();
    List<SystemConfigVO> getAllConfigs();
    SystemConfigVO getConfigByKey(String key);           // throws BizException if not found
    List<SystemConfigVO> getConfigsByGroup(String groupCode);
    void updateConfig(UpdateConfigCommand command);
    BasePageResult<SystemConfigVO> findByPage(SystemConfigPageQuery query);
}
```

### SystemConfigRepository — 系统配置仓储接口

```java
package org.smm.archetype.repository.system;

public interface SystemConfigRepository {
    Optional<SystemConfig> findByConfigKey(ConfigKey key);
    List<SystemConfig> findByGroupCode(ConfigGroup groupCode);
    List<SystemConfig> findAll();
    SystemConfig save(SystemConfig config);
    IPage<SystemConfig> findByPage(SystemConfigPageQuery query);
}
```

### SystemConfigService — 系统配置服务

```java
package org.smm.archetype.service.system;

public class SystemConfigService {
    List<ConfigGroupVO> getAllGroups();                              // 从枚举构造 VO，不查数据库
    List<SystemConfig> getConfigsByGroup(String groupCode);
    List<SystemConfig> getAllConfigs();
    SystemConfig getConfigByKey(String key);
    void updateConfig(UpdateConfigCommand command);                 // 通过 Entity.updateValue() 更新
    IPage<SystemConfig> findByPage(SystemConfigPageQuery query);
}
```

## 核心类型定义

### SystemConfig — 聚合根

```java
package org.smm.archetype.entity.system;

@Getter
@Setter
public class SystemConfig {
    private Long id;
    private ConfigKey configKey;          // 值对象：配置键
    private ConfigValue configValue;      // 值对象：配置值
    private ValueType valueType;          // 枚举：值数据类型
    private ConfigGroup groupCode;        // 枚举：所属分组
    private DisplayName displayName;      // 值对象：显示名称
    private String description;           // 描述
    private InputType inputType;          // 枚举：输入控件类型
    private String inputConfig;           // 输入控件配置（JSON）
    private Integer sort;                 // 排序号

    public void updateValue(ConfigValue newValue);  // 领域行为：更新配置值
}
```

### DDD 值对象

#### ConfigKey — 配置键

```java
public record ConfigKey(String value) {
    public ConfigKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("配置键不能为空");
        }
    }
    public static ConfigKey of(String value) {
        return new ConfigKey(value.trim());
    }
}
```

**约束**：不可变，禁止 null/空值。`of()` 工厂方法自动 trim。

#### ConfigValue — 配置值

```java
public record ConfigValue(String value) {
    public ConfigValue {
        // value 允许为空字符串，不允许为 null
    }
    public static ConfigValue of(String value) {
        return new ConfigValue(value != null ? value : "");
    }
}
```

**约束**：不可变，允许空字符串但不允许 null。`of()` 工厂方法将 null 转为空字符串。

#### DisplayName — 显示名称

```java
public record DisplayName(String value) {
    public DisplayName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("显示名称不能为空");
        }
    }
    public static DisplayName of(String value) {
        return new DisplayName(value.trim());
    }
}
```

**约束**：不可变，禁止 null/空值。`of()` 工厂方法自动 trim。

### 枚举类型

#### ConfigGroup — 配置分组

```java
public enum ConfigGroup {
    BASIC("BASIC", "基础配置", "SettingOutlined", "#1890ff"),
    EMAIL("EMAIL", "邮件配置", "MailOutlined", "#52c41a"),
    STORAGE("STORAGE", "存储配置", "CloudOutlined", "#faad14"),
    SECURITY("SECURITY", "安全配置", "LockOutlined", "#722ed1");

    String code;
    String displayName;
    String icon;        // Ant Design 图标名称
    String color;       // 前端渲染颜色
}
```

**设计要点**：`icon` / `color` 字段用于前端渲染，将 UI 元数据下沉到后端枚举，确保前后端一致性。

#### ValueType — 值数据类型

```java
public enum ValueType {
    STRING("STRING"),
    INTEGER("INTEGER"),
    DECIMAL("DECIMAL"),
    BOOLEAN("BOOLEAN"),
    ENUM("ENUM"),
    ARRAY("ARRAY"),
    JSON("JSON");
}
```

#### InputType — 输入控件类型

```java
public enum InputType {
    TEXT("TEXT"),
    TEXTAREA("TEXTAREA"),
    NUMBER("NUMBER"),
    SWITCH("SWITCH"),
    SELECT("SELECT"),
    MULTI_SELECT("MULTI_SELECT"),
    JSON_EDITOR("JSON_EDITOR");
}
```

### VO 类型（Facade 层输出）

#### SystemConfigVO

```java
public record SystemConfigVO(
    Long id,
    String configKey,
    String configValue,
    String valueType,
    String groupCode,
    String displayName,
    String description,
    String inputType,
    String inputConfig,
    Integer sort
) {}
```

#### ConfigGroupVO

```java
public record ConfigGroupVO(
    String code,
    String displayName,
    String icon,
    String color
) {}
```

### Command 类型（Facade 层输入）

#### UpdateConfigCommand

```java
public record UpdateConfigCommand(
    String configKey,
    String configValue
) {}
```

### Request 类型（Controller 层输入）

#### UpdateConfigRequest

```java
public record UpdateConfigRequest(
    @NotBlank(message = "配置值不能为空")
    String configValue
) {}
```

#### SystemConfigPageQuery

```java
public record SystemConfigPageQuery(
    @Min(1) Integer pageNo,
    @Min(1) @Max(100) Integer pageSize,
    String groupCode
) {
    // 紧凑构造器：pageNo 默认 1, pageSize 默认 20
}
```

## 服务流程

### 配置查询流程

```
GET /api/system/configs
    │
    ▼
SystemConfigController.getAllConfigs()
    │
    ▼
SystemConfigFacadeImpl.getAllConfigs()
    │
    ▼
SystemConfigService.getAllConfigs()
    │
    ▼
SystemConfigRepository.findAll()
    │
    ▼
SystemConfigRepositoryImpl.findAll()
    │  MyBatis-Plus 查询 (默认按 groupCode + sort 排序)
    ▼
List<SystemConfigDO>
    │  SystemConfigConverter.toEntity()
    ▼
List<SystemConfig> (Entity)
    │
    ▼
SystemConfigFacadeImpl (Entity → VO 转换)
    │  configKey.value(), groupCode.getCode(), ...
    ▼
List<SystemConfigVO>
    │
    ▼
BaseResult.success(voList)
```

### 配置更新流程

```
PUT /api/system/configs/{key}
  Body: { "configValue": "new value" }
    │
    ▼
SystemConfigController.updateConfig(key, request)
    │
    ├── 1. SystemConfigFacade.updateConfig(new UpdateConfigCommand(key, request.configValue()))
    │       │
    │       ▼
    │   SystemConfigService.updateConfig(command)
    │       │
    │       ├── SystemConfigRepository.findByConfigKey(ConfigKey.of(key))
    │       │       ├── 找到 → SystemConfig entity
    │       │       └── 未找到 → throw BizException(FAIL, "配置不存在")
    │       │
    │       ├── entity.updateValue(ConfigValue.of(command.configValue()))
    │       │       └── 领域行为：校验 newValue != null
    │       │
    │       └── SystemConfigRepository.save(entity)
    │               └── 根据 id != null 判断 update
    │
    └── 2. SystemConfigFacade.getConfigByKey(key)
            │
            ▼
        BaseResult.success(updatedVO)
```

### 分组查询流程（特殊：不查数据库）

```
GET /api/system/configs/groups
    │
    ▼
SystemConfigFacadeImpl.getAllGroups()
    │
    ▼
SystemConfigService.getAllGroups()
    │
    ▼
ConfigGroup.values() → stream → map → ConfigGroupVO
    │  直接从枚举构造，不查数据库
    ▼
List<ConfigGroupVO>
```

### DO↔Entity 双向转换（SystemConfigConverter）

```
Entity → DO (toDataObject):
  configKey.value()         → String configKey
  configValue.value()       → String configValue
  groupCode.getCode()       → String groupCode
  displayName.value()       → String displayName
  valueType.getCode()       → String valueType
  inputType.getCode()       → String inputType

DO → Entity (toEntity):
  String configKey          → ConfigKey.of(configKey)
  String configValue        → ConfigValue.of(configValue)
  String groupCode          → ConfigGroup.fromCode(groupCode)
  String displayName        → DisplayName.of(displayName)
  String valueType          → ValueType.fromCode(valueType)
  String inputType          → InputType.fromCode(inputType)
```

## 依赖关系

### 内部依赖

| 依赖组件 | 类型 | 用途 |
|---------|------|------|
| `SystemConfigRepository` | 接口 | 数据访问抽象 |
| `SystemConfigConverter` | 组件 | DO↔Entity 双向转换 |
| `SystemConfigService` | 服务 | 业务逻辑编排 |
| `BizException` + `CommonErrorCode` | 异常体系 | 配置不存在时抛出 |
| `BaseResult` / `BasePageResult` | 基础类型 | 统一响应包装 |

### 外部框架依赖

| 框架 | 用途 |
|------|------|
| Spring Boot Web | `@RestController`, `@GetMapping`, `@PutMapping`, `@PathVariable` |
| MyBatis-Plus | `IPage`, `LambdaQueryWrapper`, 分页查询 |
| Jakarta Validation | `@NotBlank`, `@Min`, `@Max` |
| Swagger/OpenAPI | `@Tag`, `@Operation` API 文档注解 |
| Lombok | `@RequiredArgsConstructor`, `@Getter`, `@Setter`, `@Slf4j` |

### 模块间依赖

```
SystemConfigController (controller/system)
    └── SystemConfigFacade (facade/system 接口)
            └── SystemConfigFacadeImpl (facade/system 实现)
                    ├── SystemConfigService (service/system)
                    │       └── SystemConfigRepository (repository/system 接口)
                    │               └── SystemConfigRepositoryImpl (repository/system 实现)
                    │                       ├── SystemConfigMapper (generated)
                    │                       └── SystemConfigConverter (DO↔Entity)
                    └── Entity → VO 转换
```

## 配置参考

### 分页参数约束

| 参数 | 类型 | 默认值 | 约束 |
|------|------|:------:|------|
| `pageNo` | Integer | 1 | `@Min(1)` |
| `pageSize` | Integer | 20 | `@Min(1)`, `@Max(100)` |
| `groupCode` | String | null | 可选过滤条件 |

### 数据库默认排序

- `findAll()` / `findByGroupCode()`：按 `groupCode ASC, sort ASC` 排序
- `findByPage()`：支持 `groupCode` 过滤，按 `groupCode ASC, sort ASC` 排序

## 相关文档

| 文档 | 关系 | 说明 |
|------|------|------|
| 基础类型 | business/base-types.md | BaseResult/BasePageResult 响应格式 |
| 异常体系 | business/exception-system.md | BizException 错误处理 |
| REST API 参考 | business/api-reference.md | 完整端点列表 |
| 操作日志领域 | business/domain-operation-log.md | 配置更新可被 @BusinessLog 记录 |

## 变更历史

| 日期 | 版本 | 变更内容 |
|------|------|---------|
| 2026-04-25 | v1.0 | 初始版本：系统配置领域文档生成 |
