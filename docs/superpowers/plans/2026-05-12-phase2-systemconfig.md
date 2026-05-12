# 阶段 2 systemconfig 模块化迁移 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 systemconfig 模块从传统四层包结构迁移到 Spring Modulith 风格的 `systemconfig/` + `systemconfig/internal/` 包结构，消除 IPage 泄漏，修复层级违规，增强领域模型。

**Architecture:** 增量迁移 — 新文件创建在目标包（无 Spring 注解），旧文件保持不变，Task 7 统一切换。

**Tech Stack:** Java 25, Maven, MyBatis-Plus, Spring Boot 4.x, Lombok, Jakarta Validation

**关联 Spec:** `docs/superpowers/specs/2026-05-12-phase2-systemconfig-spec.md`

**Prerequisites:**
```bash
# 确保阶段 1 已合并到 main
git checkout main && git pull
git checkout -b refactor/phase2-systemconfig-module

# 环境变量
export JAVA_HOME="/home/leonardo123/.jdks/openjdk-25.0.2"
export PATH="$JAVA_HOME/bin:$PATH"
```

---

## File Structure

```
新建（Task 1-6，无 Spring 注解）:
  app/src/main/java/org/smm/archetype/systemconfig/
    SystemConfigFacade.java                           ← 公开 API 接口
    internal/
      ConfigKey.java                                  ← entity.system
      ConfigValue.java                                ← entity.system
      DisplayName.java                                ← entity.system
      ConfigGroup.java                                ← entity.system
      ValueType.java                                  ← entity.system
      InputType.java                                  ← entity.system
      SystemConfig.java                               ← entity.system + isEditable()
      SystemConfigPageQuery.java                      ← entity.system + PageQuery 适配
      SystemConfigDO.java                             ← (引用旧 generated 包)
      SystemConfigMapper.java                         ← (引用旧 generated 包)
      SystemConfigConverter.java                      ← repository.system + toModel
      SystemConfigRepository.java                     ← repository.system + PageResult
      SystemConfigRepositoryImpl.java                 ← repository.system + PageResult
      SystemConfigService.java                        ← service.system + ConfigGroup 修复
      SystemConfigFacadeImpl.java                     ← facade.system + PageResult
      SystemConfigVO.java                             ← facade.system
      ConfigGroupVO.java                              ← facade.system
      UpdateConfigCommand.java                        ← facade.system
      SystemConfigController.java                     ← controller.system
      UpdateConfigRequest.java                        ← controller.system

新建（Task 6，测试文件）:
  app/src/test/java/org/smm/archetype/systemconfig/internal/
    ConfigKeyDisplayNameUTest.java
    SystemConfigConverterUTest.java
    SystemConfigRepositoryITest.java
    SystemConfigPaginationITest.java
    SystemConfigFacadeImplUTest.java
    SystemConfigFacadeITest.java
    SystemConfigFacadePaginationITest.java
    SystemConfigControllerITest.java
    SystemConfigControllerPaginationITest.java
    SystemConfigPaginationBoundaryITest.java

删除（Task 7）:
  app/src/main/java/org/smm/archetype/entity/system/           (8 files)
  app/src/main/java/org/smm/archetype/repository/system/       (3 files)
  app/src/main/java/org/smm/archetype/service/system/          (1 file)
  app/src/main/java/org/smm/archetype/facade/system/           (5 files)
  app/src/main/java/org/smm/archetype/controller/system/       (2 files)
  app/src/main/java/org/smm/archetype/generated/entity/SystemConfigDO.java
  app/src/main/java/org/smm/archetype/generated/mapper/SystemConfigMapper.java
  app/src/main/java/org/smm/archetype/generated/MybatisPlusGenerator.java
  app/src/test/java/org/smm/archetype/entity/system/           (1 file)
  app/src/test/java/org/smm/archetype/repository/system/       (3 files)
  app/src/test/java/org/smm/archetype/facade/system/           (3 files)
  app/src/test/java/org/smm/archetype/controller/system/       (3 files)
```

---

## Task 1: 创建目标包结构 + 迁移纯值对象

**Goal:** 创建 `systemconfig/` 和 `systemconfig/internal/` 包，迁移 3 个纯值对象 record。无 Spring 注解，无逻辑变更。

**Verification:** `mvn compile -pl app` passes + `mvn test -Dtest="*UTest" -pl app` passes

### Step 1.1: 创建目录结构

```bash
mkdir -p app/src/main/java/org/smm/archetype/systemconfig/internal
mkdir -p app/src/test/java/org/smm/archetype/systemconfig/internal
```

### Step 1.2: 迁移 ConfigKey.java

**新文件:** `app/src/main/java/org/smm/archetype/systemconfig/internal/ConfigKey.java`

```java
package org.smm.archetype.systemconfig.internal;

/**
 * 配置键值对象
 */
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

### Step 1.3: 迁移 ConfigValue.java

**新文件:** `app/src/main/java/org/smm/archetype/systemconfig/internal/ConfigValue.java`

```java
package org.smm.archetype.systemconfig.internal;

/**
 * 配置值值对象
 */
public record ConfigValue(String value) {

    public ConfigValue {
        // value 允许为空字符串，不允许为 null
    }

    public static ConfigValue of(String value) {
        return new ConfigValue(value != null ? value : "");
    }
}
```

### Step 1.4: 迁移 DisplayName.java

**新文件:** `app/src/main/java/org/smm/archetype/systemconfig/internal/DisplayName.java`

```java
package org.smm.archetype.systemconfig.internal;

/**
 * 显示名称值对象
 */
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

### Step 1.5: 验证

```bash
export JAVA_HOME="/home/leonardo123/.jdks/openjdk-25.0.2" && export PATH="$JAVA_HOME/bin:$PATH"
mvn compile -pl app -q
mvn test -Dtest="*UTest" -pl app -q
```

- [ ] 编译通过
- [ ] 所有 UTest 通过（旧测试不受影响）

---

## Task 2: 迁移枚举类

**Goal:** 迁移 3 个枚举类到新包。无 Spring 注解，无逻辑变更。

**Verification:** `mvn compile -pl app` passes + `mvn test -pl app` passes

### Step 2.1: 迁移 ConfigGroup.java

**新文件:** `app/src/main/java/org/smm/archetype/systemconfig/internal/ConfigGroup.java`

```java
package org.smm.archetype.systemconfig.internal;

import lombok.Getter;

/**
 * 配置分组枚举
 */
@Getter
public enum ConfigGroup {
    BASIC("BASIC", "基础配置", "SettingOutlined", "#1890ff"),
    EMAIL("EMAIL", "邮件配置", "MailOutlined", "#52c41a"),
    STORAGE("STORAGE", "存储配置", "CloudOutlined", "#faad14"),
    SECURITY("SECURITY", "安全配置", "LockOutlined", "#722ed1");

    private final String code;
    private final String displayName;
    private final String icon;
    private final String color;

    ConfigGroup(String code, String displayName, String icon, String color) {
        this.code = code;
        this.displayName = displayName;
        this.icon = icon;
        this.color = color;
    }

    public static ConfigGroup fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ConfigGroup group : values()) {
            if (group.getCode().equals(code)) {
                return group;
            }
        }
        return null;
    }
}
```

### Step 2.2: 迁移 ValueType.java

**新文件:** `app/src/main/java/org/smm/archetype/systemconfig/internal/ValueType.java`

```java
package org.smm.archetype.systemconfig.internal;

import lombok.Getter;

/**
 * 配置值类型枚举
 */
@Getter
public enum ValueType {
    STRING("STRING"),
    INTEGER("INTEGER"),
    DECIMAL("DECIMAL"),
    BOOLEAN("BOOLEAN"),
    ENUM("ENUM"),
    ARRAY("ARRAY"),
    JSON("JSON");

    private final String code;

    ValueType(String code) {
        this.code = code;
    }

    public static ValueType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ValueType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
```

### Step 2.3: 迁移 InputType.java

**新文件:** `app/src/main/java/org/smm/archetype/systemconfig/internal/InputType.java`

```java
package org.smm.archetype.systemconfig.internal;

import lombok.Getter;

/**
 * 输入类型枚举
 */
@Getter
public enum InputType {
    TEXT("TEXT"),
    TEXTAREA("TEXTAREA"),
    NUMBER("NUMBER"),
    SWITCH("SWITCH"),
    SELECT("SELECT"),
    MULTI_SELECT("MULTI_SELECT"),
    JSON_EDITOR("JSON_EDITOR");

    private final String code;

    InputType(String code) {
        this.code = code;
    }

    public static InputType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (InputType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
```

### Step 2.4: 验证

```bash
export JAVA_HOME="/home/leonardo123/.jdks/openjdk-25.0.2" && export PATH="$JAVA_HOME/bin:$PATH"
mvn compile -pl app -q
mvn test -pl app -q
```

- [ ] 编译通过
- [ ] 所有测试通过（旧测试不受影响）

---

## Task 3: 迁移 SystemConfig 实体 + SystemConfigPageQuery

**Goal:** 迁移实体（新增 `isEditable()`）和查询对象（适配 `PageQuery` 校验规则）。**KEY 变更。**

**Verification:** `mvn compile -pl app` passes + `mvn test -pl app` passes

### Step 3.1: 迁移 SystemConfig.java（新增 isEditable）

**新文件:** `app/src/main/java/org/smm/archetype/systemconfig/internal/SystemConfig.java`

```java
package org.smm.archetype.systemconfig.internal;

import lombok.Getter;
import lombok.Setter;

/**
 * 系统配置实体
 */
@Getter
@Setter
public class SystemConfig {

    private Long id;
    private ConfigKey configKey;
    private ConfigValue configValue;
    private ValueType valueType;
    private ConfigGroup groupCode;
    private DisplayName displayName;
    private String description;
    private InputType inputType;
    private String inputConfig;
    private Integer sort;

    /**
     * 更新配置值
     *
     * @param newValue 新的配置值
     */
    public void updateValue(ConfigValue newValue) {
        if (newValue == null) {
            throw new IllegalArgumentException("配置值不能为空");
        }
        this.configValue = newValue;
    }

    /**
     * 判断配置是否可编辑。
     * <p>
     * 当配置值类型为 BOOLEAN 时不可编辑配置值本身（只能切换开关）。
     *
     * @return true 表示可编辑配置值
     */
    public boolean isEditable() {
        return this.valueType != ValueType.BOOLEAN;
    }
}
```

### Step 3.2: 迁移 SystemConfigPageQuery.java（适配 PageQuery）

**新文件:** `app/src/main/java/org/smm/archetype/systemconfig/internal/SystemConfigPageQuery.java`

```java
package org.smm.archetype.systemconfig.internal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.smm.archetype.shared.pagination.PageQuery;

/**
 * 系统配置分页查询参数
 */
public record SystemConfigPageQuery(
        @Min(1)
        Integer pageNo,
        @Min(1)
        @Max(100)
        Integer pageSize,
        String groupCode
) {

    /**
     * 兼容无参绑定场景（Spring MVC @ModelAttribute）
     */
    public SystemConfigPageQuery() {
        this(null, null, null);
    }

    /**
     * 紧凑构造器，委托 PageQuery 做基础分页校验，为缺失的分页参数设置默认值
     */
    public SystemConfigPageQuery {
        PageQuery base = new PageQuery(
                pageNo == null ? 1 : pageNo,
                pageSize == null ? 20 : pageSize
        );
        pageNo = base.pageNo();
        pageSize = base.pageSize();
    }
}
```

### Step 3.3: 验证

```bash
export JAVA_HOME="/home/leonardo123/.jdks/openjdk-25.0.2" && export PATH="$JAVA_HOME/bin:$PATH"
mvn compile -pl app -q
mvn test -pl app -q
```

- [ ] 编译通过
- [ ] 所有测试通过

---

## Task 4: 迁移数据层（DO/Mapper/Converter/Repository）

**Goal:** 迁移数据层文件，**KEY 变更**：
1. Repository 接口 `findByPage` 返回 `PageResult`（替代 `IPage`）
2. Converter `toEntity()` 重命名为 `toModel()`
3. RepositoryImpl 使用 `PageResult.of()` 包装 IPage 结果
4. DO/Mapper **仍引用旧 generated 包**（避免 Mapper 重复注册）

**注意：** 新文件不添加 @Repository/@Component 等 Spring 注解。

**Verification:** `mvn compile -pl app` passes + `mvn test -pl app` passes

### Step 4.1: 迁移 SystemConfigDO.java（引用旧包）

**新文件:** `app/src/main/java/org/smm/archetype/systemconfig/internal/SystemConfigDO.java`

> **过渡期策略：** Task 1-6 期间，新 RepositoryImpl 引用旧的 `generated.entity.SystemConfigDO`。本文件在 Task 7 才实际被使用（届时 DO 会迁入此包）。
>
> **本 Task 跳过创建此文件**，RepositoryImpl 直接 import 旧的 `org.smm.archetype.generated.entity.SystemConfigDO`。

### Step 4.2: 迁移 SystemConfigConverter.java（toEntity → toModel）

**新文件:** `app/src/main/java/org/smm/archetype/systemconfig/internal/SystemConfigConverter.java`

> **无 @Component** — 过渡期直接构造实例使用。

```java
package org.smm.archetype.systemconfig.internal;

import org.smm.archetype.generated.entity.SystemConfigDO;

/**
 * 系统配置 DO ↔ Model 转换器
 */
public class SystemConfigConverter {

    public SystemConfigDO toDataObject(SystemConfig config) {
        if (config == null) {
            return null;
        }
        SystemConfigDO configDO = new SystemConfigDO();
        configDO.setId(config.getId());
        configDO.setConfigKey(config.getConfigKey() != null ? config.getConfigKey().value() : null);
        configDO.setConfigValue(config.getConfigValue() != null ? config.getConfigValue().value() : null);
        configDO.setValueType(config.getValueType() != null ? config.getValueType().getCode() : null);
        configDO.setGroupCode(config.getGroupCode() != null ? config.getGroupCode().getCode() : null);
        configDO.setDisplayName(config.getDisplayName() != null ? config.getDisplayName().value() : null);
        configDO.setDescription(config.getDescription());
        configDO.setInputType(config.getInputType() != null ? config.getInputType().getCode() : null);
        configDO.setInputConfig(config.getInputConfig());
        configDO.setSort(config.getSort());
        return configDO;
    }

    /**
     * DO → 领域模型转换（语义重命名：toEntity → toModel）
     */
    public SystemConfig toModel(SystemConfigDO configDO) {
        if (configDO == null) {
            return null;
        }
        SystemConfig config = new SystemConfig();
        config.setId(configDO.getId());
        config.setConfigKey(configDO.getConfigKey() != null ? ConfigKey.of(configDO.getConfigKey()) : null);
        config.setConfigValue(ConfigValue.of(configDO.getConfigValue()));
        config.setValueType(ValueType.fromCode(configDO.getValueType()));
        config.setGroupCode(ConfigGroup.fromCode(configDO.getGroupCode()));
        config.setDisplayName(configDO.getDisplayName() != null ? DisplayName.of(configDO.getDisplayName()) : null);
        config.setDescription(configDO.getDescription());
        config.setInputType(InputType.fromCode(configDO.getInputType()));
        config.setInputConfig(configDO.getInputConfig());
        config.setSort(configDO.getSort());
        return config;
    }
}
```

### Step 4.3: 迁移 SystemConfigRepository.java（PageResult 返回）

**新文件:** `app/src/main/java/org/smm/archetype/systemconfig/internal/SystemConfigRepository.java`

```java
package org.smm.archetype.systemconfig.internal;

import org.smm.archetype.shared.pagination.PageResult;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置仓储接口
 */
public interface SystemConfigRepository {

    Optional<SystemConfig> findByConfigKey(ConfigKey configKey);

    List<SystemConfig> findByGroupCode(ConfigGroup groupCode);

    List<SystemConfig> findAll();

    SystemConfig save(SystemConfig config);

    /**
     * 分页查询系统配置
     *
     * @param query 分页查询参数
     * @return 分页结果（框架无关）
     */
    PageResult<SystemConfig> findByPage(SystemConfigPageQuery query);
}
```

### Step 4.4: 迁移 SystemConfigRepositoryImpl.java（IPage → PageResult）

**新文件:** `app/src/main/java/org/smm/archetype/systemconfig/internal/SystemConfigRepositoryImpl.java`

> **无 @Repository** — 过渡期直接构造实例使用。

```java
package org.smm.archetype.systemconfig.internal;

import org.smm.archetype.generated.entity.SystemConfigDO;
import org.smm.archetype.generated.mapper.SystemConfigMapper;
import org.smm.archetype.shared.pagination.PageResult;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置仓储实现
 */
public class SystemConfigRepositoryImpl implements SystemConfigRepository {

    private final SystemConfigMapper systemConfigMapper;
    private final SystemConfigConverter converter;

    public SystemConfigRepositoryImpl(SystemConfigMapper systemConfigMapper, SystemConfigConverter converter) {
        this.systemConfigMapper = systemConfigMapper;
        this.converter = converter;
    }

    @Override
    public Optional<SystemConfig> findByConfigKey(ConfigKey configKey) {
        LambdaQueryWrapper<SystemConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfigDO::getConfigKey, configKey.value());
        SystemConfigDO configDO = systemConfigMapper.selectOne(wrapper);
        return Optional.ofNullable(converter.toModel(configDO));
    }

    @Override
    public List<SystemConfig> findByGroupCode(ConfigGroup groupCode) {
        LambdaQueryWrapper<SystemConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfigDO::getGroupCode, groupCode.getCode());
        wrapper.orderByAsc(SystemConfigDO::getSort);
        List<SystemConfigDO> configDOs = systemConfigMapper.selectList(wrapper);
        return configDOs.stream().map(converter::toModel).toList();
    }

    @Override
    public List<SystemConfig> findAll() {
        LambdaQueryWrapper<SystemConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SystemConfigDO::getGroupCode, SystemConfigDO::getSort);
        List<SystemConfigDO> configDOs = systemConfigMapper.selectList(wrapper);
        return configDOs.stream().map(converter::toModel).toList();
    }

    @Override
    public SystemConfig save(SystemConfig config) {
        SystemConfigDO configDO = converter.toDataObject(config);
        if (configDO.getId() == null) {
            systemConfigMapper.insert(configDO);
            config.setId(configDO.getId());
        } else {
            systemConfigMapper.updateById(configDO);
        }
        return config;
    }

    @Override
    public PageResult<SystemConfig> findByPage(SystemConfigPageQuery query) {
        Page<SystemConfigDO> page = new Page<>(query.pageNo(), query.pageSize());
        LambdaQueryWrapper<SystemConfigDO> wrapper = new LambdaQueryWrapper<>();
        if (query.groupCode() != null && !query.groupCode().isBlank()) {
            wrapper.eq(SystemConfigDO::getGroupCode, query.groupCode());
        }
        wrapper.orderByAsc(SystemConfigDO::getGroupCode, SystemConfigDO::getSort);
        IPage<SystemConfigDO> doPage = systemConfigMapper.selectPage(page, wrapper);

        // 转换 IPage<DO> → PageResult<Model>
        List<SystemConfig> models = doPage.getRecords().stream()
                .map(converter::toModel).toList();
        return PageResult.of(models, doPage.getTotal(), (int) doPage.getCurrent(), (int) doPage.getSize());
    }
}
```

### Step 4.5: 验证

```bash
export JAVA_HOME="/home/leonardo123/.jdks/openjdk-25.0.2" && export PATH="$JAVA_HOME/bin:$PATH"
mvn compile -pl app -q
mvn test -pl app -q
```

- [ ] 编译通过
- [ ] 所有测试通过（旧代码不受影响，新代码无 Spring 注解不被扫描）

---

## Task 5: 迁移业务层（Service/VO/Command/Facade/Controller）

**Goal:** 迁移业务层文件，**KEY 变更**：
1. `SystemConfigService.getAllGroups()` 返回 `List<ConfigGroup>`（替代 `List<ConfigGroupVO>`）
2. `SystemConfigService.findByPage()` 返回 `PageResult`（替代 `IPage`）
3. `SystemConfigFacadeImpl.findByPage()` 使用 `BasePageResult.from(PageResult)`
4. `SystemConfigFacadeImpl.getAllGroups()` 自行将 `ConfigGroup` 转换为 `ConfigGroupVO`
5. `SystemConfigFacade` 接口放在 `systemconfig/`（模块公开 API），其他在 `internal/`

**注意：** 新文件不添加 @Service/@RestController 等 Spring 注解。

**Verification:** `mvn compile -pl app` passes + `mvn test -pl app` passes

### Step 5.1: 迁移 VO/Command/Request

**新文件:** `app/src/main/java/org/smm/archetype/systemconfig/internal/SystemConfigVO.java`

```java
package org.smm.archetype.systemconfig.internal;

/**
 * 系统配置 VO
 */
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

**新文件:** `app/src/main/java/org/smm/archetype/systemconfig/internal/ConfigGroupVO.java`

```java
package org.smm.archetype.systemconfig.internal;

/**
 * 配置分组 VO
 */
public record ConfigGroupVO(
        String code,
        String displayName,
        String icon,
        String color
) {}
```

**新文件:** `app/src/main/java/org/smm/archetype/systemconfig/internal/UpdateConfigCommand.java`

```java
package org.smm.archetype.systemconfig.internal;

/**
 * 更新配置命令
 */
public record UpdateConfigCommand(
        String configKey,
        String configValue
) {}
```

**新文件:** `app/src/main/java/org/smm/archetype/systemconfig/internal/UpdateConfigRequest.java`

```java
package org.smm.archetype.systemconfig.internal;

import jakarta.validation.constraints.NotBlank;

/**
 * 更新配置请求
 */
public record UpdateConfigRequest(
        @NotBlank(message = "配置值不能为空")
        String configValue
) {}
```

### Step 5.2: 迁移 SystemConfigService.java（修复 ConfigGroupVO 依赖 + PageResult）

**新文件:** `app/src/main/java/org/smm/archetype/systemconfig/internal/SystemConfigService.java`

> **无 @Service** — 过渡期直接构造实例使用。

```java
package org.smm.archetype.systemconfig.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.shared.pagination.PageResult;

import java.util.Arrays;
import java.util.List;

/**
 * 系统配置服务
 */
@Slf4j
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    /**
     * 获取所有配置分组（返回枚举值，由 Facade 层转换为 VO）
     */
    public List<ConfigGroup> getAllGroups() {
        return Arrays.stream(ConfigGroup.values()).toList();
    }

    public List<SystemConfig> getConfigsByGroup(String groupCode) {
        ConfigGroup group = ConfigGroup.fromCode(groupCode);
        if (group == null) {
            throw new IllegalArgumentException("Invalid group: " + groupCode);
        }
        return systemConfigRepository.findByGroupCode(group);
    }

    public List<SystemConfig> getAllConfigs() {
        return systemConfigRepository.findAll();
    }

    public SystemConfig getConfigByKey(String key) {
        return systemConfigRepository.findByConfigKey(ConfigKey.of(key))
                .orElse(null);
    }

    public void updateConfig(UpdateConfigCommand command) {
        log.info("Updating config: {}", command.configKey());
        SystemConfig config = systemConfigRepository.findByConfigKey(ConfigKey.of(command.configKey()))
                .orElseThrow(() -> new IllegalArgumentException("Config not found: " + command.configKey()));
        config.updateValue(ConfigValue.of(command.configValue()));
        systemConfigRepository.save(config);
        log.info("Config updated: {}", command.configKey());
    }

    /**
     * 分页查询系统配置
     *
     * @param query 分页查询参数
     * @return 分页结果（框架无关）
     */
    public PageResult<SystemConfig> findByPage(SystemConfigPageQuery query) {
        return systemConfigRepository.findByPage(query);
    }
}
```

### Step 5.3: 迁移 SystemConfigFacade.java（公开 API，放在模块根包）

**新文件:** `app/src/main/java/org/smm/archetype/systemconfig/SystemConfigFacade.java`

```java
package org.smm.archetype.systemconfig;

import org.smm.archetype.shared.result.BasePageResult;
import org.smm.archetype.systemconfig.internal.SystemConfigPageQuery;
import org.smm.archetype.systemconfig.internal.SystemConfigVO;
import org.smm.archetype.systemconfig.internal.ConfigGroupVO;
import org.smm.archetype.systemconfig.internal.UpdateConfigCommand;

import java.util.List;

/**
 * 系统配置 Facade 接口（模块公开 API）
 * <p>
 * 提供给 Controller 层调用的统一门面，封装 Entity→VO 转换逻辑
 */
public interface SystemConfigFacade {

    /**
     * 获取所有配置分组
     *
     * @return 分组列表
     */
    List<ConfigGroupVO> getAllGroups();

    /**
     * 获取所有配置
     *
     * @return 配置 VO 列表
     */
    List<SystemConfigVO> getAllConfigs();

    /**
     * 按 Key 获取配置
     *
     * @param key 配置键
     * @return 配置 VO
     * @throws org.smm.archetype.exception.BizException 配置不存在时抛出
     */
    SystemConfigVO getConfigByKey(String key);

    /**
     * 按分组获取配置
     *
     * @param groupCode 分组编码
     * @return 配置 VO 列表
     */
    List<SystemConfigVO> getConfigsByGroup(String groupCode);

    /**
     * 更新配置
     *
     * @param command 更新命令
     */
    void updateConfig(UpdateConfigCommand command);

    /**
     * 分页查询系统配置
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    BasePageResult<SystemConfigVO> findByPage(SystemConfigPageQuery query);
}
```

### Step 5.4: 迁移 SystemConfigFacadeImpl.java（ConfigGroup 修复 + PageResult）

**新文件:** `app/src/main/java/org/smm/archetype/systemconfig/internal/SystemConfigFacadeImpl.java`

> **无 @Service** — 过渡期直接构造实例使用。

```java
package org.smm.archetype.systemconfig.internal;

import lombok.RequiredArgsConstructor;
import org.smm.archetype.exception.BizException;
import org.smm.archetype.exception.CommonErrorCode;
import org.smm.archetype.shared.pagination.PageResult;
import org.smm.archetype.shared.result.BasePageResult;

import java.util.List;

/**
 * 系统配置 Facade 实现
 * <p>
 * 封装 Service 调用，负责 Model→VO 转换
 */
@RequiredArgsConstructor
public class SystemConfigFacadeImpl implements org.smm.archetype.systemconfig.SystemConfigFacade {

    private final SystemConfigService systemConfigService;

    @Override
    public List<ConfigGroupVO> getAllGroups() {
        // 修复：Service 返回 List<ConfigGroup>（枚举），Facade 负责转换为 VO
        return systemConfigService.getAllGroups().stream()
                .map(g -> new ConfigGroupVO(g.getCode(), g.getDisplayName(), g.getIcon(), g.getColor()))
                .toList();
    }

    @Override
    public List<SystemConfigVO> getAllConfigs() {
        return systemConfigService.getAllConfigs().stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public SystemConfigVO getConfigByKey(String key) {
        SystemConfig config = systemConfigService.getConfigByKey(key);
        if (config == null) {
            throw new BizException(CommonErrorCode.FAIL);
        }
        return toVO(config);
    }

    @Override
    public List<SystemConfigVO> getConfigsByGroup(String groupCode) {
        return systemConfigService.getConfigsByGroup(groupCode).stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public void updateConfig(UpdateConfigCommand command) {
        systemConfigService.updateConfig(command);
    }

    @Override
    public BasePageResult<SystemConfigVO> findByPage(SystemConfigPageQuery query) {
        // 使用 PageResult 替代 IPage
        PageResult<SystemConfig> pageResult = systemConfigService.findByPage(query);

        List<SystemConfigVO> voList = pageResult.list().stream()
                .map(this::toVO)
                .toList();
        PageResult<SystemConfigVO> voPageResult = new PageResult<>(
                voList, pageResult.total(), pageResult.pageNo(), pageResult.pageSize(), pageResult.totalPages());

        return BasePageResult.from(voPageResult);
    }

    private SystemConfigVO toVO(SystemConfig c) {
        return new SystemConfigVO(
                c.getId(),
                c.getConfigKey() != null ? c.getConfigKey().value() : null,
                c.getConfigValue() != null ? c.getConfigValue().value() : null,
                c.getValueType() != null ? c.getValueType().getCode() : null,
                c.getGroupCode() != null ? c.getGroupCode().getCode() : null,
                c.getDisplayName() != null ? c.getDisplayName().value() : null,
                c.getDescription(),
                c.getInputType() != null ? c.getInputType().getCode() : null,
                c.getInputConfig(),
                c.getSort()
        );
    }
}
```

### Step 5.5: 迁移 SystemConfigController.java

**新文件:** `app/src/main/java/org/smm/archetype/systemconfig/internal/SystemConfigController.java`

> **无 @RestController** — 过渡期不注册为 Spring Bean。

```java
package org.smm.archetype.systemconfig.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.shared.result.BasePageResult;
import org.smm.archetype.shared.result.BaseResult;
import org.smm.archetype.systemconfig.SystemConfigFacade;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统配置控制器
 */
@Slf4j
@RequiredArgsConstructor
@Tag(name = "系统配置")
@Validated
public class SystemConfigController {

    private final SystemConfigFacade systemConfigFacade;

    @Operation(summary = "获取所有配置")
    @GetMapping("/api/system/configs")
    public BaseResult<List<SystemConfigVO>> getAllConfigs() {
        return BaseResult.success(systemConfigFacade.getAllConfigs());
    }

    @Operation(summary = "获取配置分组")
    @GetMapping("/api/system/configs/groups")
    public BaseResult<List<ConfigGroupVO>> getAllGroups() {
        return BaseResult.success(systemConfigFacade.getAllGroups());
    }

    @Operation(summary = "分页查询系统配置")
    @GetMapping("/api/system/configs/page")
    public BasePageResult<SystemConfigVO> findByPage(@Valid @ModelAttribute SystemConfigPageQuery query) {
        return systemConfigFacade.findByPage(query);
    }

    @Operation(summary = "按 Key 获取配置")
    @GetMapping("/api/system/configs/{key}")
    public BaseResult<SystemConfigVO> getConfigByKey(@PathVariable String key) {
        return BaseResult.success(systemConfigFacade.getConfigByKey(key));
    }

    @Operation(summary = "按分组获取配置")
    @GetMapping("/api/system/configs/group/{code}")
    public BaseResult<List<SystemConfigVO>> getConfigsByGroup(@PathVariable String code) {
        return BaseResult.success(systemConfigFacade.getConfigsByGroup(code));
    }

    @Operation(summary = "更新配置")
    @PutMapping("/api/system/configs/{key}")
    public BaseResult<SystemConfigVO> updateConfig(
            @PathVariable String key,
            @Valid @RequestBody UpdateConfigRequest request) {
        systemConfigFacade.updateConfig(new UpdateConfigCommand(key, request.configValue()));
        return BaseResult.success(systemConfigFacade.getConfigByKey(key));
    }
}
```

### Step 5.6: 验证

```bash
export JAVA_HOME="/home/leonardo123/.jdks/openjdk-25.0.2" && export PATH="$JAVA_HOME/bin:$PATH"
mvn compile -pl app -q
mvn test -pl app -q
```

- [ ] 编译通过
- [ ] 所有测试通过（旧代码正常工作，新代码无 Spring 注解不被扫描）

---

## Task 6: 迁移所有测试文件

**Goal:** 创建新包结构下的所有测试文件，更新 import 路径和断言。

**变更规则：**
1. 所有 `org.smm.archetype.entity.system.*` → `org.smm.archetype.systemconfig.internal.*`
2. 所有 `org.smm.archetype.facade.system.*` → `org.smm.archetype.systemconfig.internal.*` 或 `org.smm.archetype.systemconfig.*`（Facade 接口）
3. 所有 `org.smm.archetype.repository.system.*` → `org.smm.archetype.systemconfig.internal.*`
4. 所有 `org.smm.archetype.service.system.*` → `org.smm.archetype.systemconfig.internal.*`
5. 所有 `org.smm.archetype.controller.system.*` → `org.smm.archetype.systemconfig.internal.*`
6. `IPage` 断言 → `PageResult` 断言（`result.getRecords()` → `result.list()`，`result.getCurrent()` → `result.pageNo()`，`result.getTotal()` → `result.total()`，`result.getSize()` → `result.pageSize()`，`result.getPages()` → `result.totalPages()`）
7. `converter.toEntity()` → `converter.toModel()`
8. FacadeImplUTest 中 `systemConfigService.getAllGroups()` mock 返回类型从 `List<ConfigGroupVO>` → `List<ConfigGroup>`
9. FacadeImplUTest 中 `systemConfigService.findByPage()` mock 返回类型从 `IPage<SystemConfig>` → `PageResult<SystemConfig>`

**Verification:** `mvn test -pl app` passes（新旧测试共存）

### Step 6.1: 迁移 ConfigKeyDisplayNameUTest.java

**新文件:** `app/src/test/java/org/smm/archetype/systemconfig/internal/ConfigKeyDisplayNameUTest.java`

**变更说明：**
- 包声明：`org.smm.archetype.systemconfig.internal`
- 所有 `org.smm.archetype.entity.system.ConfigKey` → `org.smm.archetype.systemconfig.internal.ConfigKey`
- 所有 `org.smm.archetype.entity.system.DisplayName` → `org.smm.archetype.systemconfig.internal.DisplayName`
- 所有 `org.smm.archetype.entity.system.ConfigValue` → `org.smm.archetype.systemconfig.internal.ConfigValue`
- 所有 `org.smm.archetype.entity.system.SystemConfig` → `org.smm.archetype.systemconfig.internal.SystemConfig`
- 所有 `org.smm.archetype.entity.system.ValueType` → `org.smm.archetype.systemconfig.internal.ValueType`
- 所有 `org.smm.archetype.entity.system.InputType` → `org.smm.archetype.systemconfig.internal.InputType`
- 移除 `import org.smm.archetype.entity.system.*` 的 FQCN 引用（如 `org.smm.archetype.entity.system.DisplayName.of(...)` → `DisplayName.of(...)`）
- 新增 `isEditable()` 测试用例

**完整代码：**

```java
package org.smm.archetype.systemconfig.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.UnitTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ConfigKey / DisplayName / SystemConfig 值对象与实体")
class ConfigKeyDisplayNameUTest extends UnitTestBase {

    @Nested
    @DisplayName("ConfigKey 配置键值对象")
    class ConfigKeyTests {

        @Nested
        @DisplayName("构造器校验")
        class ConstructorValidation {

            @Test
            @DisplayName("value=null 应抛出 IllegalArgumentException")
            void should_throw_when_value_is_null() {
                assertThatThrownBy(() -> new ConfigKey(null))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("配置键不能为空");
            }

            @Test
            @DisplayName("value=空字符串 应抛出 IllegalArgumentException")
            void should_throw_when_value_is_blank() {
                assertThatThrownBy(() -> new ConfigKey(""))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("配置键不能为空");
            }

            @Test
            @DisplayName("value=纯空白字符串 应抛出 IllegalArgumentException")
            void should_throw_when_value_is_whitespace_only() {
                assertThatThrownBy(() -> new ConfigKey("   "))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("配置键不能为空");
            }

            @Test
            @DisplayName("合法 value 应正常构造")
            void should_construct_with_valid_value() {
                ConfigKey key = new ConfigKey("sys.enabled");

                assertThat(key.value()).isEqualTo("sys.enabled");
            }
        }

        @Nested
        @DisplayName("of 工厂方法")
        class OfFactory {

            @Test
            @DisplayName("of 应自动 trim 值")
            void should_trim_value() {
                ConfigKey key = ConfigKey.of("  sys.name  ");

                assertThat(key.value()).isEqualTo("sys.name");
            }

            @Test
            @DisplayName("of(null) 应抛出 NullPointerException（trim() 先于构造器校验）")
            void should_throw_when_of_null() {
                assertThatThrownBy(() -> ConfigKey.of(null))
                        .isInstanceOf(NullPointerException.class);
            }

            @Test
            @DisplayName("of(空字符串) 应抛出 IllegalArgumentException")
            void should_throw_when_of_blank() {
                assertThatThrownBy(() -> ConfigKey.of(""))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("配置键不能为空");
            }
        }

        @Nested
        @DisplayName("record equals/hashCode/toString")
        class RecordBehavior {

            @Test
            @DisplayName("相同 value 的两个 ConfigKey 应相等")
            void should_be_equal_with_same_value() {
                ConfigKey k1 = new ConfigKey("a.b");
                ConfigKey k2 = new ConfigKey("a.b");

                assertThat(k1).isEqualTo(k2);
                assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
            }

            @Test
            @DisplayName("不同 value 的两个 ConfigKey 应不相等")
            void should_not_be_equal_with_different_value() {
                ConfigKey k1 = new ConfigKey("a.b");
                ConfigKey k2 = new ConfigKey("c.d");

                assertThat(k1).isNotEqualTo(k2);
            }

            @Test
            @DisplayName("value() 应返回原始值")
            void should_return_value() {
                ConfigKey key = ConfigKey.of("test.key");
                assertThat(key.value()).isEqualTo("test.key");
            }
        }
    }

    @Nested
    @DisplayName("DisplayName 显示名称值对象")
    class DisplayNameTests {

        @Nested
        @DisplayName("构造器校验")
        class ConstructorValidation {

            @Test
            @DisplayName("value=null 应抛出 IllegalArgumentException")
            void should_throw_when_value_is_null() {
                assertThatThrownBy(() -> new DisplayName(null))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("显示名称不能为空");
            }

            @Test
            @DisplayName("value=空字符串 应抛出 IllegalArgumentException")
            void should_throw_when_value_is_blank() {
                assertThatThrownBy(() -> new DisplayName(""))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("显示名称不能为空");
            }

            @Test
            @DisplayName("value=纯空白字符串 应抛出 IllegalArgumentException")
            void should_throw_when_value_is_whitespace_only() {
                assertThatThrownBy(() -> new DisplayName("   "))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("显示名称不能为空");
            }

            @Test
            @DisplayName("合法 value 应正常构造")
            void should_construct_with_valid_value() {
                DisplayName dn = new DisplayName("系统开关");

                assertThat(dn.value()).isEqualTo("系统开关");
            }
        }

        @Nested
        @DisplayName("of 工厂方法")
        class OfFactory {

            @Test
            @DisplayName("of 应自动 trim 值")
            void should_trim_value() {
                DisplayName dn = DisplayName.of("  系统名称  ");

                assertThat(dn.value()).isEqualTo("系统名称");
            }

            @Test
            @DisplayName("of(null) 应抛出 NullPointerException（trim() 先于构造器校验）")
            void should_throw_when_of_null() {
                assertThatThrownBy(() -> DisplayName.of(null))
                        .isInstanceOf(NullPointerException.class);
            }

            @Test
            @DisplayName("of(空字符串) 应抛出 IllegalArgumentException")
            void should_throw_when_of_blank() {
                assertThatThrownBy(() -> DisplayName.of(""))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("显示名称不能为空");
            }
        }

        @Nested
        @DisplayName("record equals/hashCode")
        class RecordBehavior {

            @Test
            @DisplayName("相同 value 的两个 DisplayName 应相等")
            void should_be_equal_with_same_value() {
                DisplayName d1 = new DisplayName("名称");
                DisplayName d2 = new DisplayName("名称");

                assertThat(d1).isEqualTo(d2);
                assertThat(d1.hashCode()).isEqualTo(d2.hashCode());
            }

            @Test
            @DisplayName("不同 value 的两个 DisplayName 应不相等")
            void should_not_be_equal_with_different_value() {
                DisplayName d1 = new DisplayName("名称A");
                DisplayName d2 = new DisplayName("名称B");

                assertThat(d1).isNotEqualTo(d2);
            }
        }
    }

    @Nested
    @DisplayName("SystemConfig 实体")
    class SystemConfigTests {

        @Nested
        @DisplayName("updateValue 方法")
        class UpdateValue {

            @Test
            @DisplayName("updateValue(null) 应抛出 IllegalArgumentException")
            void should_throw_when_newValue_is_null() {
                SystemConfig config = new SystemConfig();

                assertThatThrownBy(() -> config.updateValue(null))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("配置值不能为空");
            }

            @Test
            @DisplayName("updateValue(合法值) 应正常设置 configValue")
            void should_set_configValue_on_valid_update() {
                SystemConfig config = new SystemConfig();
                ConfigValue newValue = ConfigValue.of("true");

                config.updateValue(newValue);

                assertThat(config.getConfigValue()).isEqualTo(newValue);
            }

            @Test
            @DisplayName("updateValue 应覆盖之前的值")
            void should_overwrite_previous_value() {
                SystemConfig config = new SystemConfig();
                config.updateValue(ConfigValue.of("old"));

                ConfigValue newValue = ConfigValue.of("new");

                config.updateValue(newValue);

                assertThat(config.getConfigValue()).isEqualTo(newValue);
                assertThat(config.getConfigValue().value()).isEqualTo("new");
            }
        }

        @Nested
        @DisplayName("isEditable 方法")
        class IsEditable {

            @Test
            @DisplayName("非 BOOLEAN 类型应可编辑")
            void should_be_editable_when_not_boolean() {
                SystemConfig config = new SystemConfig();
                config.setValueType(ValueType.STRING);

                assertThat(config.isEditable()).isTrue();
            }

            @Test
            @DisplayName("BOOLEAN 类型不可编辑配置值")
            void should_not_be_editable_when_boolean() {
                SystemConfig config = new SystemConfig();
                config.setValueType(ValueType.BOOLEAN);

                assertThat(config.isEditable()).isFalse();
            }

            @Test
            @DisplayName("valueType 为 null 时默认可编辑")
            void should_be_editable_when_value_type_is_null() {
                SystemConfig config = new SystemConfig();
                // valueType 未设置，默认 null

                assertThat(config.isEditable()).isTrue();
            }
        }

        @Nested
        @DisplayName("getter/setter")
        class GetterSetter {

            @Test
            @DisplayName("所有字段应正确读写")
            void should_read_write_all_fields() {
                SystemConfig config = new SystemConfig();
                ConfigKey key = ConfigKey.of("test.key");
                ConfigValue value = ConfigValue.of("test-value");
                DisplayName displayName = DisplayName.of("测试配置");

                config.setId(1L);
                config.setConfigKey(key);
                config.setConfigValue(value);
                config.setDisplayName(displayName);
                config.setDescription("测试描述");
                config.setSort(10);

                assertThat(config.getId()).isEqualTo(1L);
                assertThat(config.getConfigKey()).isEqualTo(key);
                assertThat(config.getConfigValue()).isEqualTo(value);
                assertThat(config.getDisplayName()).isEqualTo(displayName);
                assertThat(config.getDescription()).isEqualTo("测试描述");
                assertThat(config.getSort()).isEqualTo(10);
            }
        }
    }
}
```

### Step 6.2: 迁移 SystemConfigConverterUTest.java

**新文件:** `app/src/test/java/org/smm/archetype/systemconfig/internal/SystemConfigConverterUTest.java`

**变更说明：**
- 包声明：`org.smm.archetype.systemconfig.internal`
- 所有 import 更新为新包路径
- `converter.toEntity(...)` → `converter.toModel(...)`
- 测试类名中的 "toEntity" 注释更新为 "toModel"
- `SystemConfigDO` 仍引用 `org.smm.archetype.generated.entity.SystemConfigDO`

**完整代码：**

```java
package org.smm.archetype.systemconfig.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.generated.entity.SystemConfigDO;
import org.smm.archetype.support.UnitTestBase;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SystemConfigConverter")
class SystemConfigConverterUTest extends UnitTestBase {

    private final SystemConfigConverter converter = new SystemConfigConverter();

    // =========================================================================
    // toDataObject
    // =========================================================================

    @Nested
    @DisplayName("toDataObject")
    class ToDataObject {

        @Test
        @DisplayName("正常转换 — 所有字段非 null")
        void should_convert_all_fields_when_all_non_null() {
            // given
            SystemConfig config = new SystemConfig();
            config.setId(1L);
            config.setConfigKey(ConfigKey.of("site.name"));
            config.setConfigValue(ConfigValue.of("MyApp"));
            config.setValueType(ValueType.STRING);
            config.setGroupCode(ConfigGroup.BASIC);
            config.setDisplayName(DisplayName.of("站点名称"));
            config.setDescription("站点名称配置");
            config.setInputType(InputType.TEXT);
            config.setInputConfig("{\"maxLength\":100}");
            config.setSort(10);

            // when
            SystemConfigDO configDO = converter.toDataObject(config);

            // then
            assertThat(configDO).isNotNull();
            assertThat(configDO.getId()).isEqualTo(1L);
            assertThat(configDO.getConfigKey()).isEqualTo("site.name");
            assertThat(configDO.getConfigValue()).isEqualTo("MyApp");
            assertThat(configDO.getValueType()).isEqualTo("STRING");
            assertThat(configDO.getGroupCode()).isEqualTo("BASIC");
            assertThat(configDO.getDisplayName()).isEqualTo("站点名称");
            assertThat(configDO.getDescription()).isEqualTo("站点名称配置");
            assertThat(configDO.getInputType()).isEqualTo("TEXT");
            assertThat(configDO.getInputConfig()).isEqualTo("{\"maxLength\":100}");
            assertThat(configDO.getSort()).isEqualTo(10);
        }

        @Test
        @DisplayName("所有值对象为 null 时 DO 字段也应为 null")
        void should_set_null_when_value_objects_are_null() {
            // given
            SystemConfig config = new SystemConfig();
            config.setId(2L);
            config.setDescription("仅描述有值");

            // when
            SystemConfigDO configDO = converter.toDataObject(config);

            // then
            assertThat(configDO).isNotNull();
            assertThat(configDO.getId()).isEqualTo(2L);
            assertThat(configDO.getConfigKey()).isNull();
            assertThat(configDO.getConfigValue()).isNull();
            assertThat(configDO.getValueType()).isNull();
            assertThat(configDO.getGroupCode()).isNull();
            assertThat(configDO.getDisplayName()).isNull();
            assertThat(configDO.getInputType()).isNull();
            assertThat(configDO.getDescription()).isEqualTo("仅描述有值");
        }

        @Test
        @DisplayName("输入 null 应返回 null")
        void should_return_null_when_input_is_null() {
            SystemConfigDO configDO = converter.toDataObject(null);

            assertThat(configDO).isNull();
        }
    }

    // =========================================================================
    // toModel
    // =========================================================================

    @Nested
    @DisplayName("toModel")
    class ToModel {

        @Test
        @DisplayName("正常转换 — 所有字段非 null")
        void should_convert_all_fields_when_all_non_null() {
            // given
            SystemConfigDO configDO = SystemConfigDO.builder()
                    .configKey("site.name")
                    .configValue("MyApp")
                    .valueType("STRING")
                    .groupCode("BASIC")
                    .displayName("站点名称")
                    .description("站点名称配置")
                    .inputType("TEXT")
                    .inputConfig("{\"maxLength\":100}")
                    .sort(10)
                    .build();
            configDO.setId(1L);

            // when
            SystemConfig config = converter.toModel(configDO);

            // then
            assertThat(config).isNotNull();
            assertThat(config.getId()).isEqualTo(1L);
            assertThat(config.getConfigKey()).isNotNull();
            assertThat(config.getConfigKey().value()).isEqualTo("site.name");
            assertThat(config.getConfigValue()).isNotNull();
            assertThat(config.getConfigValue().value()).isEqualTo("MyApp");
            assertThat(config.getValueType()).isEqualTo(ValueType.STRING);
            assertThat(config.getGroupCode()).isEqualTo(ConfigGroup.BASIC);
            assertThat(config.getDisplayName()).isNotNull();
            assertThat(config.getDisplayName().value()).isEqualTo("站点名称");
            assertThat(config.getDescription()).isEqualTo("站点名称配置");
            assertThat(config.getInputType()).isEqualTo(InputType.TEXT);
            assertThat(config.getInputConfig()).isEqualTo("{\"maxLength\":100}");
            assertThat(config.getSort()).isEqualTo(10);
        }

        @Test
        @DisplayName("DO 字段为 null 时值对象应为 null")
        void should_set_null_when_do_fields_are_null() {
            // given
            SystemConfigDO configDO = SystemConfigDO.builder()
                    .description("仅描述有值")
                    .build();
            configDO.setId(2L);

            // when
            SystemConfig config = converter.toModel(configDO);

            // then
            assertThat(config).isNotNull();
            assertThat(config.getId()).isEqualTo(2L);
            assertThat(config.getConfigKey()).isNull();
            assertThat(config.getConfigValue()).isNotNull();
            assertThat(config.getConfigValue().value()).isEmpty();
            assertThat(config.getValueType()).isNull();
            assertThat(config.getGroupCode()).isNull();
            assertThat(config.getDisplayName()).isNull();
            assertThat(config.getInputType()).isNull();
            assertThat(config.getDescription()).isEqualTo("仅描述有值");
        }

        @Test
        @DisplayName("输入 null 应返回 null")
        void should_return_null_when_input_is_null() {
            SystemConfig config = converter.toModel(null);

            assertThat(config).isNull();
        }
    }

    // =========================================================================
    // 往返一致性
    // =========================================================================

    @Nested
    @DisplayName("往返一致性")
    class RoundTrip {

        @Test
        @DisplayName("toDataObject + toModel 往返应保持数据一致")
        void should_preserve_data_in_round_trip() {
            // given
            SystemConfig original = new SystemConfig();
            original.setId(100L);
            original.setConfigKey(ConfigKey.of("app.theme"));
            original.setConfigValue(ConfigValue.of("dark"));
            original.setValueType(ValueType.ENUM);
            original.setGroupCode(ConfigGroup.BASIC);
            original.setDisplayName(DisplayName.of("主题配置"));
            original.setDescription("应用主题");
            original.setInputType(InputType.SELECT);
            original.setInputConfig("{\"options\":[\"dark\",\"light\"]}");
            original.setSort(5);

            // when — Model → DO → Model
            SystemConfigDO configDO = converter.toDataObject(original);
            SystemConfig restored = converter.toModel(configDO);

            // then
            assertThat(restored).isNotNull();
            assertThat(restored.getId()).isEqualTo(original.getId());
            assertThat(restored.getConfigKey().value()).isEqualTo(original.getConfigKey().value());
            assertThat(restored.getConfigValue().value()).isEqualTo(original.getConfigValue().value());
            assertThat(restored.getValueType()).isEqualTo(original.getValueType());
            assertThat(restored.getGroupCode()).isEqualTo(original.getGroupCode());
            assertThat(restored.getDisplayName().value()).isEqualTo(original.getDisplayName().value());
            assertThat(restored.getDescription()).isEqualTo(original.getDescription());
            assertThat(restored.getInputType()).isEqualTo(original.getInputType());
            assertThat(restored.getInputConfig()).isEqualTo(original.getInputConfig());
            assertThat(restored.getSort()).isEqualTo(original.getSort());
        }
    }
}
```

### Step 6.3: 迁移 SystemConfigPaginationITest.java

**新文件:** `app/src/test/java/org/smm/archetype/systemconfig/internal/SystemConfigPaginationITest.java`

**变更说明：**
- 包声明 + import 更新
- `IPage<SystemConfig>` → `PageResult<SystemConfig>`
- `result.getRecords()` → `result.list()`
- `result.getCurrent()` → `result.pageNo()`
- `result.getSize()` → `result.pageSize()`
- `result.getPages()` → `result.totalPages()`

**完整代码：**

```java
package org.smm.archetype.systemconfig.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.shared.pagination.PageResult;
import org.smm.archetype.support.IntegrationTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 系统配置仓储分页集成测试 — 验证分页查询（真实 SQLite 数据库）
 * <p>
 * 注意：此测试在 Task 7 之前使用旧 Repository Bean，需要 Task 7 后才能通过。
 * Task 6 暂不创建此文件，在 Task 7 中统一创建并验证。
 */
class SystemConfigPaginationITest extends IntegrationTestBase {

    @Autowired
    private org.smm.archetype.systemconfig.internal.SystemConfigRepository systemConfigRepository;

    @Nested
    @DisplayName("findByPage — 无过滤条件")
    class FindByPageNoFilter {

        @Test
        @DisplayName("MFT: 无过滤条件分页查询返回所有配置，分页信息正确")
        void should_returnAllConfigs_paginated() {
            SystemConfigPageQuery query = new SystemConfigPageQuery(1, 10, null);

            PageResult<SystemConfig> result = systemConfigRepository.findByPage(query);

            assertThat(result.list()).hasSize(10);
            assertThat(result.total()).isEqualTo(15);
            assertThat(result.pageNo()).isEqualTo(1);
            assertThat(result.pageSize()).isEqualTo(10);
            assertThat(result.totalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("MFT: 第二页返回剩余 5 条配置")
        void should_returnSecondPage_withRemainingRecords() {
            SystemConfigPageQuery query = new SystemConfigPageQuery(2, 10, null);

            PageResult<SystemConfig> result = systemConfigRepository.findByPage(query);

            assertThat(result.list()).hasSize(5);
            assertThat(result.total()).isEqualTo(15);
            assertThat(result.pageNo()).isEqualTo(2);
        }

        @Test
        @DisplayName("MFT: pageSize 超过总数时返回全部记录")
        void should_returnAllRecords_whenPageSizeExceedsTotal() {
            SystemConfigPageQuery query = new SystemConfigPageQuery(1, 100, null);

            PageResult<SystemConfig> result = systemConfigRepository.findByPage(query);

            assertThat(result.list()).hasSize(15);
            assertThat(result.total()).isEqualTo(15);
            assertThat(result.totalPages()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("findByPage — 按 groupCode 过滤")
    class FindByPageWithGroupFilter {

        @Test
        @DisplayName("MFT: 按 BASIC 分组过滤，返回 4 条配置")
        void should_returnBasicConfigs_whenFilterByGroup() {
            SystemConfigPageQuery query = new SystemConfigPageQuery(1, 10, "BASIC");

            PageResult<SystemConfig> result = systemConfigRepository.findByPage(query);

            assertThat(result.list()).hasSize(4);
            assertThat(result.total()).isEqualTo(4);
            assertThat(result.list())
                    .allMatch(c -> c.getGroupCode().getCode().equals("BASIC"));
        }

        @Test
        @DisplayName("MFT: 按 EMAIL 分组过滤，返回 4 条配置")
        void should_returnEmailConfigs_whenFilterByGroup() {
            SystemConfigPageQuery query = new SystemConfigPageQuery(1, 10, "EMAIL");

            PageResult<SystemConfig> result = systemConfigRepository.findByPage(query);

            assertThat(result.list()).hasSize(4);
            assertThat(result.total()).isEqualTo(4);
            assertThat(result.list())
                    .allMatch(c -> c.getGroupCode().getCode().equals("EMAIL"));
        }

        @Test
        @DisplayName("MFT: 按 STORAGE 分组过滤，返回 3 条配置")
        void should_returnStorageConfigs_whenFilterByGroup() {
            SystemConfigPageQuery query = new SystemConfigPageQuery(1, 10, "STORAGE");

            PageResult<SystemConfig> result = systemConfigRepository.findByPage(query);

            assertThat(result.list()).hasSize(3);
            assertThat(result.total()).isEqualTo(3);
        }

        @Test
        @DisplayName("DIR: groupCode 无匹配时返回空列表，total 为 0")
        void should_returnEmpty_whenGroupCodeNotMatch() {
            SystemConfigPageQuery query = new SystemConfigPageQuery(1, 10, "NONEXISTENT");

            PageResult<SystemConfig> result = systemConfigRepository.findByPage(query);

            assertThat(result.list()).isEmpty();
            assertThat(result.total()).isEqualTo(0);
        }
    }
}
```

### Step 6.4: 迁移 SystemConfigFacadeImplUTest.java

**新文件:** `app/src/test/java/org/smm/archetype/systemconfig/internal/SystemConfigFacadeImplUTest.java`

**变更说明：**
- 包声明 + import 更新
- `systemConfigService.getAllGroups()` mock 返回 `List<ConfigGroup>`（不再是 `List<ConfigGroupVO>`）
- `systemConfigService.findByPage()` mock 返回 `PageResult<SystemConfig>`（不再是 `IPage<SystemConfig>`）
- 移除 `import com.baomidou.mybatisplus.extension.plugins.pagination.Page`
- 移除 `import com.baomidou.mybatisplus.core.metadata.IPage`

**完整代码：**

```java
package org.smm.archetype.systemconfig.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.smm.archetype.shared.pagination.PageResult;
import org.smm.archetype.shared.result.BasePageResult;
import org.smm.archetype.exception.BizException;
import org.smm.archetype.support.UnitTestBase;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("SystemConfigFacadeImpl - Facade 层单元测试")
class SystemConfigFacadeImplUTest extends UnitTestBase {

    @Mock
    private SystemConfigService systemConfigService;

    @InjectMocks
    private SystemConfigFacadeImpl facade;

    // ──────────────────────────────────────────────
    // 辅助方法：构建完整的 SystemConfig 实体
    // ──────────────────────────────────────────────

    private SystemConfig buildFullConfig() {
        SystemConfig config = new SystemConfig();
        config.setId(1L);
        config.setConfigKey(ConfigKey.of("site.name"));
        config.setConfigValue(ConfigValue.of("MyApp"));
        config.setValueType(ValueType.STRING);
        config.setGroupCode(ConfigGroup.BASIC);
        config.setDisplayName(DisplayName.of("站点名称"));
        config.setDescription("站点显示名称");
        config.setInputType(InputType.TEXT);
        config.setInputConfig("{\"placeholder\":\"请输入站点名称\"}");
        config.setSort(1);
        return config;
    }

    private SystemConfig buildPartialNullConfig() {
        SystemConfig config = new SystemConfig();
        config.setId(2L);
        // configKey, configValue, valueType, groupCode, displayName, inputType 全为 null
        config.setDescription("partial config");
        config.setSort(2);
        return config;
    }

    // ──────────────────────────────────────────────
    // getAllGroups
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("getAllGroups")
    class GetAllGroups {

        @Test
        @DisplayName("应将 Service 返回的 ConfigGroup 枚举转换为 ConfigGroupVO")
        void should_convert_groups_to_vos() {
            // given — Service 返回 List<ConfigGroup>（不再是 ConfigGroupVO）
            List<ConfigGroup> groups = List.of(ConfigGroup.BASIC, ConfigGroup.EMAIL);
            when(systemConfigService.getAllGroups()).thenReturn(groups);

            // when
            List<ConfigGroupVO> result = facade.getAllGroups();

            // then — Facade 负责转换
            assertThat(result).hasSize(2);
            assertThat(result.getFirst().code()).isEqualTo("BASIC");
            assertThat(result.getFirst().displayName()).isEqualTo("基础配置");
            assertThat(result.get(1).code()).isEqualTo("EMAIL");
            assertThat(result.get(1).displayName()).isEqualTo("邮件配置");
            verify(systemConfigService).getAllGroups();
        }

        @Test
        @DisplayName("空枚举应返回空 VO 列表")
        void should_return_empty_for_empty_groups() {
            when(systemConfigService.getAllGroups()).thenReturn(Collections.emptyList());

            List<ConfigGroupVO> result = facade.getAllGroups();

            assertThat(result).isEmpty();
        }
    }

    // ──────────────────────────────────────────────
    // getAllConfigs
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("getAllConfigs")
    class GetAllConfigs {

        @Test
        @DisplayName("应将 Model 列表转换为 VO 列表")
        void should_convert_models_to_vos() {
            // given
            SystemConfig config = buildFullConfig();
            when(systemConfigService.getAllConfigs()).thenReturn(List.of(config));

            // when
            List<SystemConfigVO> result = facade.getAllConfigs();

            // then
            assertThat(result).hasSize(1);
            SystemConfigVO vo = result.getFirst();
            assertThat(vo.id()).isEqualTo(1L);
            assertThat(vo.configKey()).isEqualTo("site.name");
            assertThat(vo.configValue()).isEqualTo("MyApp");
            assertThat(vo.valueType()).isEqualTo("STRING");
            assertThat(vo.groupCode()).isEqualTo("BASIC");
            assertThat(vo.displayName()).isEqualTo("站点名称");
            assertThat(vo.inputType()).isEqualTo("TEXT");
            assertThat(vo.sort()).isEqualTo(1);
        }

        @Test
        @DisplayName("空列表应返回空 VO 列表")
        void should_return_empty_for_empty_list() {
            // given
            when(systemConfigService.getAllConfigs()).thenReturn(Collections.emptyList());

            // when
            List<SystemConfigVO> result = facade.getAllConfigs();

            // then
            assertThat(result).isEmpty();
        }
    }

    // ──────────────────────────────────────────────
    // getConfigByKey
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("getConfigByKey")
    class GetConfigByKey {

        @Test
        @DisplayName("存在的 Key 应返回对应 VO")
        void should_return_vo_for_existing_key() {
            // given
            SystemConfig config = buildFullConfig();
            when(systemConfigService.getConfigByKey("site.name")).thenReturn(config);

            // when
            SystemConfigVO result = facade.getConfigByKey("site.name");

            // then
            assertThat(result.configKey()).isEqualTo("site.name");
            assertThat(result.configValue()).isEqualTo("MyApp");
        }

        @Test
        @DisplayName("不存在的 Key 应抛出 BizException")
        void should_throw_for_nonexistent_key() {
            // given
            when(systemConfigService.getConfigByKey("not.exist")).thenReturn(null);

            // when / then
            assertThatThrownBy(() -> facade.getConfigByKey("not.exist"))
                    .isInstanceOf(BizException.class);
        }
    }

    // ──────────────────────────────────────────────
    // getConfigsByGroup
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("getConfigsByGroup")
    class GetConfigsByGroup {

        @Test
        @DisplayName("应按分组返回 VO 列表")
        void should_return_vos_by_group() {
            // given
            SystemConfig config = buildFullConfig();
            when(systemConfigService.getConfigsByGroup("BASIC")).thenReturn(List.of(config));

            // when
            List<SystemConfigVO> result = facade.getConfigsByGroup("BASIC");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().groupCode()).isEqualTo("BASIC");
        }
    }

    // ──────────────────────────────────────────────
    // updateConfig
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("updateConfig")
    class UpdateConfig {

        @Test
        @DisplayName("应委托 Service 执行更新")
        void should_delegate_update_to_service() {
            // given
            UpdateConfigCommand command = new UpdateConfigCommand("site.name", "NewName");

            // when
            facade.updateConfig(command);

            // then
            verify(systemConfigService).updateConfig(command);
        }
    }

    // ──────────────────────────────────────────────
    // findByPage
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("findByPage")
    class FindByPage {

        @Test
        @DisplayName("应将 PageResult<Model> 转换为 BasePageResult<VO>")
        void should_convert_page_to_result() {
            // given — Service 返回 PageResult（不再是 IPage）
            SystemConfig config = buildFullConfig();
            PageResult<SystemConfig> pageResult = PageResult.of(List.of(config), 1, 1, 10);
            when(systemConfigService.findByPage(any())).thenReturn(pageResult);

            SystemConfigPageQuery query = new SystemConfigPageQuery(1, 10, null);

            // when
            BasePageResult<SystemConfigVO> result = facade.findByPage(query);

            // then
            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getPageNo()).isEqualTo(1);
            assertThat(result.getPageSize()).isEqualTo(10);
            assertThat(result.getData()).hasSize(1);
            assertThat(result.getData().getFirst().configKey()).isEqualTo("site.name");
        }

        @Test
        @DisplayName("空页应返回空数据列表")
        void should_return_empty_data_for_empty_page() {
            // given
            PageResult<SystemConfig> pageResult = PageResult.of(Collections.emptyList(), 0, 1, 10);
            when(systemConfigService.findByPage(any())).thenReturn(pageResult);

            SystemConfigPageQuery query = new SystemConfigPageQuery(1, 10, null);

            // when
            BasePageResult<SystemConfigVO> result = facade.findByPage(query);

            // then
            assertThat(result.getTotal()).isZero();
            assertThat(result.getData()).isEmpty();
        }
    }

    // ──────────────────────────────────────────────
    // toVO 边界条件（null 值对象字段）
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("toVO 边界条件")
    class ToVOEdgeCases {

        @Test
        @DisplayName("Model 值对象字段全为 null 时应安全转换")
        void should_handle_null_value_objects() {
            // given — buildPartialNullConfig 的 configKey/configValue/valueType/groupCode/displayName/inputType 全为 null
            SystemConfig config = buildPartialNullConfig();
            when(systemConfigService.getAllConfigs()).thenReturn(List.of(config));

            // when
            List<SystemConfigVO> result = facade.getAllConfigs();

            // then
            assertThat(result).hasSize(1);
            SystemConfigVO vo = result.getFirst();
            assertThat(vo.configKey()).isNull();
            assertThat(vo.configValue()).isNull();
            assertThat(vo.valueType()).isNull();
            assertThat(vo.groupCode()).isNull();
            assertThat(vo.displayName()).isNull();
            assertThat(vo.inputType()).isNull();
            assertThat(vo.description()).isEqualTo("partial config");
        }
    }
}
```

### Step 6.5: 迁移 ITest 文件（纯 import 更新）

以下文件仅做 import 路径替换，测试逻辑不变。在 **Task 7** 中创建（因为 Task 1-6 期间旧 Spring Bean 仍在工作，新 ITest 需要新 Bean 才能通过）。

**文件清单（Task 7 中创建）：**

| 新文件路径 | 变更说明 |
|-----------|---------|
| `systemconfig/internal/SystemConfigRepositoryITest.java` | import 更新，逻辑不变 |
| `systemconfig/internal/SystemConfigFacadeITest.java` | import 更新，`SystemConfigFacade` → `org.smm.archetype.systemconfig.SystemConfigFacade` |
| `systemconfig/internal/SystemConfigFacadePaginationITest.java` | import 更新，逻辑不变 |
| `systemconfig/internal/SystemConfigControllerITest.java` | import 更新，逻辑不变 |
| `systemconfig/internal/SystemConfigControllerPaginationITest.java` | import 更新，逻辑不变 |
| `systemconfig/internal/SystemConfigPaginationBoundaryITest.java` | import 更新，逻辑不变 |

**不迁移的文件（通过 HTTP 端点测试，不受包结构影响）：**
- `cases/integrationtest/WebExceptionAdviseI18nITest.java` — 无需变更
- `cases/integrationtest/PaginationTaxonomyITest.java` — 无需变更

### Step 6.6: 验证

```bash
export JAVA_HOME="/home/leonardo123/.jdks/openjdk-25.0.2" && export PATH="$JAVA_HOME/bin:$PATH"
mvn compile -pl app -q
mvn test -pl app -q
```

- [ ] 编译通过
- [ ] 所有 UTest 通过（新旧共存）
- [ ] 所有 ITest 通过（旧代码路径仍在工作）

---

## Task 7: 删除旧文件 + 添加 Spring 注解 + 全量验证

**Goal:** 删除所有旧包文件，为新文件添加 Spring 注解，更新代码生成器，完成全量验证。

**Verification:** `mvn clean test -pl app` 全部通过

### Step 7.1: 为新文件添加 Spring 注解

对以下文件添加注解（使用 edit 工具逐个修改）：

| 文件 | 添加注解 |
|------|---------|
| `systemconfig/internal/SystemConfigService.java` | `@Slf4j` + `@Service` |
| `systemconfig/internal/SystemConfigRepositoryImpl.java` | `@Repository` + `@RequiredArgsConstructor` |
| `systemconfig/internal/SystemConfigConverter.java` | `@Component` |
| `systemconfig/internal/SystemConfigFacadeImpl.java` | `@Service` |
| `systemconfig/internal/SystemConfigController.java` | `@Slf4j` + `@RestController` + `@RequestMapping("/api/system/configs")` |

**注意**：`SystemConfigRepositoryImpl` 需要从构造器注入改为 `@RequiredArgsConstructor` + `@Repository`。

### Step 7.2: 迁移 DO 和 Mapper 到新包

**创建** `app/src/main/java/org/smm/archetype/systemconfig/internal/SystemConfigDO.java`

```java
package org.smm.archetype.systemconfig.internal;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.smm.archetype.entity.base.BaseDO;

/**
 * 系统配置数据对象
 * <p>
 * 从 generated/ 包迁入模块内部。此文件由代码生成器生成。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("system_config")
public class SystemConfigDO extends BaseDO {
    private String configKey;
    private String configValue;
    private String valueType;
    private String groupCode;
    private String displayName;
    private String description;
    private String inputType;
    private String inputConfig;
    private Integer sort;
}
```

**创建** `app/src/main/java/org/smm/archetype/systemconfig/internal/SystemConfigMapper.java`

```java
package org.smm.archetype.systemconfig.internal;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统配置 Mapper
 * <p>
 * 从 generated/ 包迁入模块内部。
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfigDO> {
}
```

### Step 7.3: 更新新文件中的 DO/Mapper import

更新以下文件，将 `org.smm.archetype.generated.entity.SystemConfigDO` 替换为 `org.smm.archetype.systemconfig.internal.SystemConfigDO`，将 `org.smm.archetype.generated.mapper.SystemConfigMapper` 替换为 `org.smm.archetype.systemconfig.internal.SystemConfigMapper`：

- `SystemConfigRepositoryImpl.java`
- `SystemConfigConverter.java`
- `SystemConfigConverterUTest.java`

### Step 7.4: 删除旧源文件

```bash
# 旧 entity 包（8 files）
rm -rf app/src/main/java/org/smm/archetype/entity/system/

# 旧 repository 包（3 files）
rm -rf app/src/main/java/org/smm/archetype/repository/system/

# 旧 service 包（1 file）
rm -rf app/src/main/java/org/smm/archetype/service/system/

# 旧 facade 包（5 files）
rm -rf app/src/main/java/org/smm/archetype/facade/system/

# 旧 controller 包（2 files）
rm -rf app/src/main/java/org/smm/archetype/controller/system/

# 旧 generated 文件
rm -f app/src/main/java/org/smm/archetype/generated/entity/SystemConfigDO.java
rm -f app/src/main/java/org/smm/archetype/generated/mapper/SystemConfigMapper.java
rm -f app/src/main/java/org/smm/archetype/generated/MybatisPlusGenerator.java

# 清理空目录
rmdir app/src/main/java/org/smm/archetype/generated/entity/ 2>/dev/null || true
rmdir app/src/main/java/org/smm/archetype/generated/mapper/ 2>/dev/null || true
```

### Step 7.5: 删除旧测试文件

```bash
# 旧 entity 测试（1 file）
rm -rf app/src/test/java/org/smm/archetype/entity/system/

# 旧 repository 测试（3 files）
rm -rf app/src/test/java/org/smm/archetype/repository/system/

# 旧 facade 测试（3 files）
rm -rf app/src/test/java/org/smm/archetype/facade/system/

# 旧 controller 测试（3 files）
rm -rf app/src/test/java/org/smm/archetype/controller/system/
```

### Step 7.6: 创建 ITest 文件（从旧文件迁移）

将 Step 6.5 中列出的 6 个 ITest 文件创建到新包路径。这些文件仅做 import 路径替换，测试逻辑完全不变。

**import 替换规则：**

| 旧 import | 新 import |
|-----------|-----------|
| `org.smm.archetype.entity.system.*` | `org.smm.archetype.systemconfig.internal.*` |
| `org.smm.archetype.facade.system.SystemConfigFacade` | `org.smm.archetype.systemconfig.SystemConfigFacade` |
| `org.smm.archetype.facade.system.SystemConfigVO` | `org.smm.archetype.systemconfig.internal.SystemConfigVO` |
| `org.smm.archetype.facade.system.ConfigGroupVO` | `org.smm.archetype.systemconfig.internal.ConfigGroupVO` |
| `org.smm.archetype.facade.system.UpdateConfigCommand` | `org.smm.archetype.systemconfig.internal.UpdateConfigCommand` |
| `org.smm.archetype.repository.system.SystemConfigRepository` | `org.smm.archetype.systemconfig.internal.SystemConfigRepository` |
| `org.smm.archetype.entity.system.SystemConfigPageQuery` | `org.smm.archetype.systemconfig.internal.SystemConfigPageQuery` |
| `org.smm.archetype.entity.system.ConfigGroup` | `org.smm.archetype.systemconfig.internal.ConfigGroup` |
| `org.smm.archetype.entity.system.ConfigKey` | `org.smm.archetype.systemconfig.internal.ConfigKey` |
| `org.smm.archetype.entity.system.ConfigValue` | `org.smm.archetype.systemconfig.internal.ConfigValue` |
| `org.smm.archetype.entity.system.SystemConfig` | `org.smm.archetype.systemconfig.internal.SystemConfig` |
| `org.smm.archetype.entity.system.ValueType` | `org.smm.archetype.systemconfig.internal.ValueType` |
| `org.smm.archetype.entity.system.InputType` | `org.smm.archetype.systemconfig.internal.InputType` |
| `org.smm.archetype.entity.system.DisplayName` | `org.smm.archetype.systemconfig.internal.DisplayName` |
| `org.smm.archetype.generated.entity.SystemConfigDO` | `org.smm.archetype.systemconfig.internal.SystemConfigDO` |

**具体文件：**

#### SystemConfigRepositoryITest.java

```java
package org.smm.archetype.systemconfig.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.IntegrationTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 系统配置仓储集成测试 — 验证 CRUD 操作（真实 SQLite 数据库）
 */
class SystemConfigRepositoryITest extends IntegrationTestBase {

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Nested
    @DisplayName("findByConfigKey")
    class FindByConfigKey {

        @Test
        @DisplayName("MFT: 根据已存在的 configKey 查询返回正确配置")
        void should_returnConfig_whenKeyExists() {
            Optional<SystemConfig> result = systemConfigRepository.findByConfigKey(ConfigKey.of("site.description"));

            assertThat(result).isPresent();
            assertThat(result.get().getConfigKey().value()).isEqualTo("site.description");
            assertThat(result.get().getGroupCode()).isEqualTo(ConfigGroup.BASIC);
            assertThat(result.get().getDisplayName().value()).isEqualTo("站点描述");
        }

        @Test
        @DisplayName("DIR: 不存在的 configKey 返回 empty")
        void should_returnEmpty_whenKeyNotExists() {
            Optional<SystemConfig> result = systemConfigRepository.findByConfigKey(ConfigKey.of("nonexistent.key"));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByGroupCode")
    class FindByGroupCode {

        @Test
        @DisplayName("MFT: 按 BASIC 分组查询返回 4 条配置，按 sort 排序")
        void should_returnBasicConfigs_sortedBySort() {
            List<SystemConfig> result = systemConfigRepository.findByGroupCode(ConfigGroup.BASIC);

            assertThat(result).hasSize(4);
            assertThat(result).allMatch(c -> c.getGroupCode() == ConfigGroup.BASIC);
            assertThat(result.get(0).getSort()).isLessThanOrEqualTo(result.get(1).getSort());
            assertThat(result.get(1).getSort()).isLessThanOrEqualTo(result.get(2).getSort());
            assertThat(result.get(2).getSort()).isLessThanOrEqualTo(result.get(3).getSort());
        }

        @Test
        @DisplayName("MFT: 按 EMAIL 分组查询返回 4 条配置")
        void should_returnEmailConfigs() {
            List<SystemConfig> result = systemConfigRepository.findByGroupCode(ConfigGroup.EMAIL);

            assertThat(result).hasSize(4);
            assertThat(result).allMatch(c -> c.getGroupCode() == ConfigGroup.EMAIL);
        }

        @Test
        @DisplayName("MFT: 按 SECURITY 分组查询返回 4 条配置")
        void should_returnSecurityConfigs() {
            List<SystemConfig> result = systemConfigRepository.findByGroupCode(ConfigGroup.SECURITY);

            assertThat(result).hasSize(4);
            assertThat(result).allMatch(c -> c.getGroupCode() == ConfigGroup.SECURITY);
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("MFT: 查询所有配置返回 15 条，按 groupCode + sort 排序")
        void should_returnAllConfigs_sortedByGroupAndSort() {
            List<SystemConfig> result = systemConfigRepository.findAll();

            assertThat(result).hasSize(15);
            for (int i = 1; i < result.size(); i++) {
                SystemConfig prev = result.get(i - 1);
                SystemConfig curr = result.get(i);
                int groupCompare = prev.getGroupCode().getCode().compareTo(curr.getGroupCode().getCode());
                if (groupCompare == 0) {
                    assertThat(prev.getSort()).isLessThanOrEqualTo(curr.getSort());
                } else {
                    assertThat(groupCompare).isLessThan(0);
                }
            }
        }
    }

    @Nested
    @DisplayName("save — 新增和更新")
    class Save {

        @Test
        @DisplayName("MFT: 新增配置成功，返回含 id 的实体")
        void should_insertNewConfig_withGeneratedId() {
            SystemConfig newConfig = new SystemConfig();
            newConfig.setConfigKey(ConfigKey.of("test.new.key"));
            newConfig.setConfigValue(ConfigValue.of("test-value"));
            newConfig.setValueType(ValueType.STRING);
            newConfig.setGroupCode(ConfigGroup.BASIC);
            newConfig.setDisplayName(DisplayName.of("测试配置"));
            newConfig.setDescription("测试描述");
            newConfig.setInputType(InputType.TEXT);
            newConfig.setInputConfig("");
            newConfig.setSort(99);

            SystemConfig saved = systemConfigRepository.save(newConfig);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getConfigKey().value()).isEqualTo("test.new.key");
            assertThat(saved.getConfigValue().value()).isEqualTo("test-value");

            Optional<SystemConfig> found = systemConfigRepository.findByConfigKey(ConfigKey.of("test.new.key"));
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("MFT: 更新已有配置的 configValue")
        void should_updateExistingConfig() {
            Optional<SystemConfig> existing = systemConfigRepository.findByConfigKey(ConfigKey.of("site.description"));
            assertThat(existing).isPresent();

            SystemConfig config = existing.get();
            String originalValue = config.getConfigValue().value();
            config.updateValue(ConfigValue.of("Updated description"));

            SystemConfig updated = systemConfigRepository.save(config);

            assertThat(updated.getConfigValue().value()).isEqualTo("Updated description");
            assertThat(updated.getId()).isEqualTo(config.getId());

            Optional<SystemConfig> reloaded = systemConfigRepository.findByConfigKey(ConfigKey.of("site.description"));
            assertThat(reloaded).isPresent();
            assertThat(reloaded.get().getConfigValue().value()).isEqualTo("Updated description");
        }
    }
}
```

#### SystemConfigFacadeITest.java

```java
package org.smm.archetype.systemconfig.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.smm.archetype.exception.BizException;
import org.smm.archetype.support.IntegrationTestBase;
import org.smm.archetype.systemconfig.SystemConfigFacade;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SystemConfigFacade 集成测试
 * <p>
 * 使用真实 Spring 上下文 + 内存 SQLite，init.sql 加载 15 条 system_config
 */
@DisplayName("SystemConfigFacade")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SystemConfigFacadeITest extends IntegrationTestBase {

    @Autowired
    private SystemConfigFacade systemConfigFacade;

    @Nested
    @DisplayName("getAllGroups")
    class GetAllGroups {

        @Test
        @DisplayName("MFT: 返回所有配置分组，至少包含 BASIC/EMAIL/STORAGE/SECURITY")
        void should_returnAllGroups() {
            List<ConfigGroupVO> groups = systemConfigFacade.getAllGroups();

            assertThat(groups).isNotEmpty();
            assertThat(groups).hasSizeGreaterThanOrEqualTo(4);

            List<String> codes = groups.stream().map(ConfigGroupVO::code).toList();
            assertThat(codes).contains("BASIC", "EMAIL", "STORAGE", "SECURITY");
        }
    }

    @Nested
    @DisplayName("getAllConfigs")
    class GetAllConfigs {

        @Test
        @DisplayName("MFT: 返回所有配置 VO，字段映射正确")
        void should_returnAllConfigsAsVOs() {
            List<SystemConfigVO> configs = systemConfigFacade.getAllConfigs();

            assertThat(configs).isNotEmpty();
            assertThat(configs).hasSizeGreaterThanOrEqualTo(15);

            SystemConfigVO first = configs.getFirst();
            assertThat(first.configKey()).isNotBlank();
            assertThat(first.configValue()).isNotNull();
            assertThat(first.groupCode()).isNotBlank();
            assertThat(first.displayName()).isNotBlank();
            assertThat(first.id()).isNotNull();
            assertThat(first.valueType()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("getConfigByKey")
    class GetConfigByKey {

        @Test
        @DisplayName("MFT: 按 key 返回配置 VO")
        void should_returnConfigByKey() {
            SystemConfigVO config = systemConfigFacade.getConfigByKey("site.name");

            assertThat(config).isNotNull();
            assertThat(config.configKey()).isEqualTo("site.name");
            assertThat(config.configValue()).isNotBlank();
            assertThat(config.groupCode()).isEqualTo("BASIC");
        }

        @Test
        @DisplayName("DIR: 配置不存在时抛 BizException")
        void should_throwBizException_whenKeyNotFound() {
            assertThatThrownBy(() -> systemConfigFacade.getConfigByKey("nonexistent"))
                    .isInstanceOf(BizException.class);
        }
    }

    @Nested
    @DisplayName("getConfigsByGroup")
    class GetConfigsByGroup {

        @Test
        @DisplayName("MFT: 按分组返回 VO 列表，所有记录属于目标分组")
        void should_returnConfigsByGroup() {
            List<SystemConfigVO> configs = systemConfigFacade.getConfigsByGroup("BASIC");

            assertThat(configs).isNotEmpty();
            assertThat(configs).allMatch(c -> "BASIC".equals(c.groupCode()));
        }
    }

    @Nested
    @DisplayName("updateConfig")
    class UpdateConfig {

        @Test
        @DisplayName("MFT: 更新配置值后重新查询验证已更新")
        void should_updateConfigValue() {
            SystemConfigVO before = systemConfigFacade.getConfigByKey("site.name");

            String newValue = "Updated-" + System.currentTimeMillis();
            systemConfigFacade.updateConfig(new UpdateConfigCommand("site.name", newValue));

            SystemConfigVO after = systemConfigFacade.getConfigByKey("site.name");
            assertThat(after.configValue()).isEqualTo(newValue);
            assertThat(after.configValue()).isNotEqualTo(before.configValue());
        }
    }
}
```

#### SystemConfigFacadePaginationITest.java

```java
package org.smm.archetype.systemconfig.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.shared.result.BasePageResult;
import org.smm.archetype.support.IntegrationTestBase;
import org.smm.archetype.systemconfig.SystemConfigFacade;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SystemConfigFacade 分页查询集成测试
 */
@DisplayName("SystemConfigFacade — findByPage")
class SystemConfigFacadePaginationITest extends IntegrationTestBase {

    @Autowired
    private SystemConfigFacade systemConfigFacade;

    @Nested
    @DisplayName("findByPage")
    class FindByPage {

        @Test
        @DisplayName("MFT: 分页查询返回正确结果")
        void should_returnPagedResult() {
            SystemConfigPageQuery query = new SystemConfigPageQuery(1, 10, null);
            BasePageResult<SystemConfigVO> result = systemConfigFacade.findByPage(query);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getTotal()).isGreaterThanOrEqualTo(15);
            assertThat(result.getPageNo()).isEqualTo(1);
            assertThat(result.getPageSize()).isEqualTo(10);
            assertThat(result.getData()).hasSize(10);
        }

        @Test
        @DisplayName("MFT: 按 groupCode 过滤返回对应分组的配置")
        void should_filterByGroupCode() {
            SystemConfigPageQuery query = new SystemConfigPageQuery(1, 10, "EMAIL");
            BasePageResult<SystemConfigVO> result = systemConfigFacade.findByPage(query);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotEmpty();
            assertThat(result.getData()).allMatch(c -> "EMAIL".equals(c.groupCode()));
        }

        @Test
        @DisplayName("MFT: 不存在的分组返回空结果")
        void should_returnEmptyResult_forNonexistentGroup() {
            SystemConfigPageQuery query = new SystemConfigPageQuery(1, 10, "NONEXISTENT");
            BasePageResult<SystemConfigVO> result = systemConfigFacade.findByPage(query);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getTotal()).isEqualTo(0);
            assertThat(result.getData()).isEmpty();
        }

        @Test
        @DisplayName("MFT: 第二页分页信息正确")
        void should_returnSecondPage() {
            SystemConfigPageQuery query = new SystemConfigPageQuery(2, 10, null);
            BasePageResult<SystemConfigVO> result = systemConfigFacade.findByPage(query);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getPageNo()).isEqualTo(2);
            assertThat(result.getTotal()).isGreaterThanOrEqualTo(15);
            assertThat(result.getData()).isNotEmpty();
        }
    }
}
```

#### SystemConfigControllerITest.java

```java
package org.smm.archetype.systemconfig.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.IntegrationTestBase;

import java.util.Map;

/**
 * 系统配置控制器集成测试 — 验证所有 API 端点
 */
class SystemConfigControllerITest extends IntegrationTestBase {

    @Nested
    @DisplayName("GET /api/system/configs")
    class GetAllConfigs {

        @Test
        @DisplayName("MFT: 返回 15 条配置，按分组排序")
        void should_returnAllConfigs_sorted() {
            webTestClient.get().uri("/api/system/configs")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(15);
        }
    }

    @Nested
    @DisplayName("GET /api/system/configs/groups")
    class GetAllGroups {

        @Test
        @DisplayName("MFT: 返回 4 个配置分组")
        void should_return4Groups() {
            webTestClient.get().uri("/api/system/configs/groups")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(4)
                    .jsonPath("$.data[0].code").isEqualTo("BASIC")
                    .jsonPath("$.data[0].displayName").isEqualTo("基础配置");
        }
    }

    @Nested
    @DisplayName("GET /api/system/configs/{key}")
    class GetConfigByKey {

        @Test
        @DisplayName("MFT: 已存在的 key 返回配置详情")
        void should_returnConfig_whenKeyExists() {
            webTestClient.get().uri("/api/system/configs/site.description")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data.configKey").isEqualTo("site.description")
                    .jsonPath("$.data.groupCode").isEqualTo("BASIC")
                    .jsonPath("$.data.displayName").isEqualTo("站点描述");
        }

        @Test
        @DisplayName("DIR: 不存在的 key 返回 fail")
        void should_returnFail_whenKeyNotExists() {
            webTestClient.get().uri("/api/system/configs/nonexistent.key")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(2000)
                    .jsonPath("$.success").isEqualTo(false);
        }
    }

    @Nested
    @DisplayName("GET /api/system/configs/group/{code}")
    class GetConfigsByGroup {

        @Test
        @DisplayName("MFT: 按 EMAIL 分组返回 4 条配置")
        void should_returnEmailConfigs() {
            webTestClient.get().uri("/api/system/configs/group/EMAIL")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(4);
        }

        @Test
        @DisplayName("DIR: 无效分组 code 返回错误")
        void should_returnError_whenInvalidGroup() {
            webTestClient.get().uri("/api/system/configs/group/INVALID")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false);
        }
    }

    @Nested
    @DisplayName("PUT /api/system/configs/{key}")
    class UpdateConfig {

        @Test
        @DisplayName("MFT: 更新配置值成功")
        void should_updateConfigValue() {
            Map<String, String> requestBody = Map.of("configValue", "Updated Site Name");

            webTestClient.put().uri("/api/system/configs/site.name")
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data.configKey").isEqualTo("site.name")
                    .jsonPath("$.data.configValue").isEqualTo("Updated Site Name");
        }

        @Test
        @DisplayName("DIR: configValue 为空返回校验失败")
        void should_returnValidationError_whenConfigValueBlank() {
            Map<String, String> requestBody = Map.of("configValue", "");

            webTestClient.put().uri("/api/system/configs/site.name")
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.code").isEqualTo(2001);
        }

        @Test
        @DisplayName("DIR: 更新不存在的 key 返回错误")
        void should_returnError_whenKeyNotExists() {
            Map<String, String> requestBody = Map.of("configValue", "some value");

            webTestClient.put().uri("/api/system/configs/nonexistent.key")
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false);
        }
    }
}
```

#### SystemConfigControllerPaginationITest.java

```java
package org.smm.archetype.systemconfig.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.IntegrationTestBase;

/**
 * 系统配置控制器分页集成测试 — 验证分页 API 端点
 */
class SystemConfigControllerPaginationITest extends IntegrationTestBase {

    @Nested
    @DisplayName("GET /api/system/configs/page")
    class FindByPage {

        @Test
        @DisplayName("MFT: 无参数分页查询返回分页结果，total=15")
        void should_returnPaginatedResult_withDefaults() {
            webTestClient.get().uri("/api/system/configs/page")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(15)
                    .jsonPath("$.pageNo").isEqualTo(1)
                    .jsonPath("$.pageSize").isEqualTo(20)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(15);
        }

        @Test
        @DisplayName("MFT: 指定 pageNo=1&pageSize=5 返回 5 条记录")
        void should_return5Records_whenPageSizeIs5() {
            webTestClient.get().uri("/api/system/configs/page?pageNo=1&pageSize=5")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(15)
                    .jsonPath("$.pageNo").isEqualTo(1)
                    .jsonPath("$.pageSize").isEqualTo(5)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(5);
        }

        @Test
        @DisplayName("MFT: 第二页 pageNo=2&pageSize=5 返回 5 条记录")
        void should_returnSecondPage_whenPageNoIs2() {
            webTestClient.get().uri("/api/system/configs/page?pageNo=2&pageSize=5")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(15)
                    .jsonPath("$.pageNo").isEqualTo(2)
                    .jsonPath("$.pageSize").isEqualTo(5)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(5);
        }

        @Test
        @DisplayName("MFT: 按 groupCode=basic 过滤返回 4 条 BASIC 配置")
        void should_filterByGroupCode() {
            webTestClient.get().uri("/api/system/configs/page?groupCode=BASIC")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(4)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(4)
                    .jsonPath("$.data[0].groupCode").isEqualTo("BASIC");
        }

        @Test
        @DisplayName("MFT: groupCode=EMAIL&pageNo=1&pageSize=2 返回 2 条 EMAIL 配置")
        void should_filterByGroupCode_withPagination() {
            webTestClient.get().uri("/api/system/configs/page?groupCode=EMAIL&pageNo=1&pageSize=2")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(4)
                    .jsonPath("$.pageNo").isEqualTo(1)
                    .jsonPath("$.pageSize").isEqualTo(2)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(2)
                    .jsonPath("$.data[0].groupCode").isEqualTo("EMAIL");
        }

        @Test
        @DisplayName("DIR: pageSize > 100 返回校验失败")
        void should_returnValidationError_whenPageSizeExceeds100() {
            webTestClient.get().uri("/api/system/configs/page?pageSize=101")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.code").isEqualTo(2001);
        }

        @Test
        @DisplayName("MFT: pageSize=0 自动使用默认值 20")
        void should_useDefaultPageSize_whenPageSizeIs0() {
            webTestClient.get().uri("/api/system/configs/page?pageSize=0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.pageSize").isEqualTo(20);
        }

        @Test
        @DisplayName("MFT: pageNo=0 自动使用默认值 1")
        void should_useDefaultPageNo_whenPageNoIs0() {
            webTestClient.get().uri("/api/system/configs/page?pageNo=0")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.pageNo").isEqualTo(1);
        }
    }
}
```

#### SystemConfigPaginationBoundaryITest.java

```java
package org.smm.archetype.systemconfig.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.IntegrationTestBase;

/**
 * 系统配置分页边界值测试 — 验证分页参数边界行为
 */
class SystemConfigPaginationBoundaryITest extends IntegrationTestBase {

    @Nested
    @DisplayName("GET /api/system/configs/page — 边界值测试")
    class PaginationBoundary {

        @Test
        @DisplayName("BND: pageNo=100（超出范围）返回空 data 数组，total=15")
        void should_returnEmptyData_whenPageNoExceedsRange() {
            webTestClient.get().uri("/api/system/configs/page?pageNo=100&pageSize=10")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(15)
                    .jsonPath("$.pageNo").isEqualTo(100)
                    .jsonPath("$.pageSize").isEqualTo(10)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(0);
        }

        @Test
        @DisplayName("BND: pageSize=1 返回恰好 1 条记录")
        void should_returnExactlyOneRecord_whenPageSizeIs1() {
            webTestClient.get().uri("/api/system/configs/page?pageNo=1&pageSize=1")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(15)
                    .jsonPath("$.pageNo").isEqualTo(1)
                    .jsonPath("$.pageSize").isEqualTo(1)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(1);
        }

        @Test
        @DisplayName("BND: pageSize=100（边界值）返回全部 15 条配置")
        void should_returnAllRecords_whenPageSizeIs100() {
            webTestClient.get().uri("/api/system/configs/page?pageNo=1&pageSize=100")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(15)
                    .jsonPath("$.pageNo").isEqualTo(1)
                    .jsonPath("$.pageSize").isEqualTo(100)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(15);
        }

        @Test
        @DisplayName("BND: pageNo 超出 total（pageNo=100）→ data 为空，total=15")
        void should_returnEmptyDataWithCorrectTotal_whenPageNoBeyondTotal() {
            webTestClient.get().uri("/api/system/configs/page?pageNo=100&pageSize=5")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(15)
                    .jsonPath("$.pageNo").isEqualTo(100)
                    .jsonPath("$.pageSize").isEqualTo(5)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(0);
        }

        @Test
        @DisplayName("BND: groupCode=NONEXISTENT 返回 total=0, data=[]")
        void should_returnEmptyResult_whenGroupCodeNonExistent() {
            webTestClient.get().uri("/api/system/configs/page?groupCode=NONEXISTENT")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(0)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(0);
        }
    }
}
```

### Step 7.7: 检查残留引用

```bash
# 确认没有残留的旧包引用
grep -r "org.smm.archetype.entity.system" app/src/ --include="*.java" || echo "OK: 无 entity.system 残留"
grep -r "org.smm.archetype.facade.system" app/src/ --include="*.java" || echo "OK: 无 facade.system 残留"
grep -r "org.smm.archetype.repository.system" app/src/ --include="*.java" || echo "OK: 无 repository.system 残留"
grep -r "org.smm.archetype.service.system" app/src/ --include="*.java" || echo "OK: 无 service.system 残留"
grep -r "org.smm.archetype.controller.system" app/src/ --include="*.java" || echo "OK: 无 controller.system 残留"
grep -r "org.smm.archetype.generated" app/src/ --include="*.java" || echo "OK: 无 generated 残留"
```

### Step 7.8: 全量验证

```bash
export JAVA_HOME="/home/leonardo123/.jdks/openjdk-25.0.2" && export PATH="$JAVA_HOME/bin:$PATH"
mvn clean test -pl app
```

- [ ] 编译通过
- [ ] 所有 UTest 通过
- [ ] 所有 ITest 通过（包括跨模块测试 WebExceptionAdviseI18nITest、PaginationTaxonomyITest）
- [ ] 所有 ArchUnit 测试通过
- [ ] 无旧包残留引用
- [ ] 无 @Data 注解
- [ ] 无 @Value 注入

---

## 总结

| Task | 文件数 | KEY 变更 | 风险 |
|------|--------|---------|------|
| 1 | 3 新源文件 | 纯包迁移 | 低 |
| 2 | 3 新源文件 | 纯包迁移 | 低 |
| 3 | 2 新源文件 | isEditable() + PageQuery 适配 | 低 |
| 4 | 3 新源文件 | PageResult + toModel | 中（无 Spring 注解策略） |
| 5 | 6 新源文件 | ConfigGroup 修复 + BasePageResult.from | 中（Facade 接口位置） |
| 6 | 3 新测试文件 | UTest import + IPage→PageResult | 低 |
| 7 | 删除 20 旧 + 添加注解 + 6 新 ITest | 切换 | 高（原子操作） |
