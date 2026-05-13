# Phase 6: Architecture Evolution Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Activate MapStruct, reorganize internal/ into business/infrastructure layers, introduce Domain Event infrastructure with two example events, and enable Spring Modulith boundary verification.

**Architecture:** Modular Monolith with Facade + internal/ pattern. Adding infrastructure/ sub-package for technical implementations, MapStruct for compile-safe mapping, and Domain Event for cross-module async communication.

**Tech Stack:** Spring Boot 4.x, MapStruct 1.6.3, Spring Modulith 2.0.6, Java 25, ArchUnit

---

## File Structure

### New Files to Create

| File | Responsibility |
|------|---------------|
| `app/src/main/java/org/smm/archetype/shared/event/DomainEvent.java` | Event interface (eventId + occurredAt) |
| `app/src/main/java/org/smm/archetype/shared/event/DomainEventPublisher.java` | Event publishing interface |
| `app/src/main/java/org/smm/archetype/shared/event/SpringDomainEventPublisher.java` | Spring ApplicationEventPublisher adapter |
| `app/src/main/java/org/smm/archetype/shared/CentralMapperConfig.java` | MapStruct global @MapperConfig |
| `app/src/main/java/org/smm/archetype/auth/UserLoggedInEvent.java` | Login event record (API package) |
| `app/src/main/java/org/smm/archetype/systemconfig/ConfigChangedEvent.java` | Config change event record (API package) |
| `app/src/main/java/org/smm/archetype/auth/internal/ConfigChangedEventHandler.java` | Auth module consumes config changes |
| `app/src/main/java/org/smm/archetype/operationlog/internal/UserLoggedInEventHandler.java` | OperationLog module consumes login events |

### Files to Modify (Move to infrastructure/)

**auth module:**
- `auth/internal/UserDO.java` → `auth/internal/infrastructure/UserDO.java`
- `auth/internal/UserMapper.java` → `auth/internal/infrastructure/UserMapper.java`
- `auth/internal/UserRepositoryImpl.java` → `auth/internal/infrastructure/UserRepositoryImpl.java`
- `auth/internal/UserConverter.java` → rewrite as MapStruct interface → `auth/internal/infrastructure/UserConverter.java`
- `auth/internal/AuthConfigure.java` → DELETE (Converter auto-registers via @Component)

**operationlog module:**
- `operationlog/internal/OperationLogDO.java` → `operationlog/internal/infrastructure/OperationLogDO.java`
- `operationlog/internal/OperationLogMapper.java` → `operationlog/internal/infrastructure/OperationLogMapper.java`
- `operationlog/internal/OperationLogRepositoryImpl.java` → `operationlog/internal/infrastructure/OperationLogRepositoryImpl.java`
- `operationlog/internal/OperationLogConverter.java` → rewrite as MapStruct interface → `operationlog/internal/infrastructure/OperationLogConverter.java`
- `operationlog/internal/OperationLogConfigure.java` → DELETE

**systemconfig module:**
- `systemconfig/internal/SystemConfigDO.java` → `systemconfig/internal/infrastructure/SystemConfigDO.java`
- `systemconfig/internal/SystemConfigMapper.java` → `systemconfig/internal/infrastructure/SystemConfigMapper.java`
- `systemconfig/internal/SystemConfigRepositoryImpl.java` → `systemconfig/internal/infrastructure/SystemConfigRepositoryImpl.java`
- `systemconfig/internal/SystemConfigConverter.java` → rewrite as MapStruct interface → `systemconfig/internal/infrastructure/SystemConfigConverter.java`
- `systemconfig/internal/SystemConfigConfigure.java` → DELETE

### Files to Modify (Content changes)

| File | Change |
|------|--------|
| `auth/internal/AuthFacadeImpl.java` | Inject DomainEventPublisher, publish UserLoggedInEvent after login |
| `systemconfig/internal/SystemConfigService.java` | Inject DomainEventPublisher, publish ConfigChangedEvent after update |
| `auth/package-info.java` | allowedDependencies add "systemconfig" |
| `operationlog/package-info.java` | allowedDependencies add "auth" |
| `app/pom.xml` | Add MapStruct compiler args |
| `ModulithComplianceUTest.java` | Remove @Disabled, add module name + count tests |
| `ModuleArchitectureComplianceUTest.java` | M-05 update exception list for infrastructure/ sub-package |
| All test files referencing moved classes | Update import paths |

---

## Task 1: MapStruct Global Configuration + Maven Compiler Args

**Files:**
- Create: `app/src/main/java/org/smm/archetype/shared/CentralMapperConfig.java`
- Modify: `app/pom.xml` (maven-compiler-plugin configuration)

- [ ] **Step 1: Create CentralMapperConfig**

```java
package org.smm.archetype.shared;

import org.mapstruct.MapperConfig;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct 全局配置
 * <p>
 * 所有 Converter 接口通过 config = CentralMapperConfig.class 引用此配置。
 * 使用 componentModel = SPRING 让 MapStruct 自动生成 @Component 实现类。
 */
@MapperConfig(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface CentralMapperConfig {
}
```

- [ ] **Step 2: Update maven-compiler-plugin in app/pom.xml**

In the `<configuration>` block of maven-compiler-plugin, add `<compilerArgs>`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <compilerArgs>
            <arg>-Amapstruct.defaultComponentModel=spring</arg>
            <arg>-Amapstruct.unmappedTargetPolicy=ERROR</arg>
        </compilerArgs>
        <annotationProcessorPaths>
            <!-- existing paths unchanged -->
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

- [ ] **Step 3: Verify compilation**

Run: `mvn compile -pl app -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/org/smm/archetype/shared/CentralMapperConfig.java app/pom.xml
git commit -m "feat(mapstruct): add CentralMapperConfig + Maven compiler args"
```

---

## Task 2: Rewrite UserConverter as MapStruct Interface

**Files:**
- Modify: `app/src/main/java/org/smm/archetype/auth/internal/UserConverter.java` (rewrite in-place, will move to infrastructure/ later)

- [ ] **Step 1: Rewrite UserConverter**

Current file is at `auth/internal/UserConverter.java`. Rewrite as:

```java
package org.smm.archetype.auth.internal;

import org.mapstruct.Mapper;
import org.smm.archetype.shared.CentralMapperConfig;

/**
 * 用户 DO ↔ Model 转换器（MapStruct 生成实现）
 */
@Mapper(config = CentralMapperConfig.class)
interface UserConverter {

    User toModel(UserDO userDO);

    UserDO toDO(User user);
}
```

**Note**: Method name changed from `toDataObject` to `toDO` — verify callers already use `toDO`. If any caller still uses `toDataObject`, rename the call.

- [ ] **Step 2: Delete AuthConfigure.java**

AuthConfigure only has the UserConverter @Bean. MapStruct generates @Component impl, so this is no longer needed.

Delete: `app/src/main/java/org/smm/archetype/auth/internal/AuthConfigure.java`

- [ ] **Step 3: Verify compilation + existing tests**

Run: `mvn test -pl app -Dtest="*UserConverter*UTest,*AuthFacade*UTest" -q`
Expected: All pass

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor(auth): rewrite UserConverter as MapStruct interface, delete AuthConfigure"
```

---

## Task 3: Rewrite OperationLogConverter as MapStruct Interface

**Files:**
- Modify: `app/src/main/java/org/smm/archetype/operationlog/internal/OperationLogConverter.java`

- [ ] **Step 1: Rewrite OperationLogConverter**

```java
package org.smm.archetype.operationlog.internal;

import org.mapstruct.Mapper;
import org.smm.archetype.shared.CentralMapperConfig;

/**
 * 操作日志 DO → Model 转换器（MapStruct 生成实现）
 */
@Mapper(config = CentralMapperConfig.class)
interface OperationLogConverter {

    OperationLog toModel(OperationLogDO logDO);
}
```

- [ ] **Step 2: Delete OperationLogConfigure.java**

Delete: `app/src/main/java/org/smm/archetype/operationlog/internal/OperationLogConfigure.java`

- [ ] **Step 3: Verify compilation + existing tests**

Run: `mvn test -pl app -Dtest="*OperationLog*UTest" -q`
Expected: All pass

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor(operationlog): rewrite OperationLogConverter as MapStruct interface, delete OperationLogConfigure"
```

---

## Task 4: Rewrite SystemConfigConverter as MapStruct Interface

**Files:**
- Modify: `app/src/main/java/org/smm/archetype/systemconfig/internal/SystemConfigConverter.java`

**IMPORTANT**: SystemConfigConverter has non-trivial mappings — ConfigKey.value(), ConfigGroup.getCode(), etc. MapStruct needs explicit @Mapping annotations.

- [ ] **Step 1: Rewrite SystemConfigConverter**

```java
package org.smm.archetype.systemconfig.internal;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.smm.archetype.shared.CentralMapperConfig;

/**
 * 系统配置 DO ↔ Model 转换器（MapStruct 生成实现）
 */
@Mapper(config = CentralMapperConfig.class)
interface SystemConfigConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    SystemConfigDO toDO(SystemConfig config);

    SystemConfig toModel(SystemConfigDO configDO);
}
```

**WAIT** — SystemConfig has value objects (ConfigKey, ConfigValue, DisplayName, ConfigGroup) that don't directly map to String fields in DO. MapStruct can't auto-map these. We need custom mapping methods:

```java
package org.smm.archetype.systemconfig.internal;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.smm.archetype.shared.CentralMapperConfig;

/**
 * 系统配置 DO ↔ Model 转换器（MapStruct 生成实现）
 * <p>
 * 包含值对象（ConfigKey/ConfigValue/DisplayName/ConfigGroup）的自定义映射。
 */
@Mapper(config = CentralMapperConfig.class)
interface SystemConfigConverter {

    SystemConfig toModel(SystemConfigDO configDO);

    SystemConfigDO toDO(SystemConfig config);

    // === 值对象 ↔ String 映射（MapStruct 自动使用） ===

    default String map(ConfigKey key) {
        return key != null ? key.value() : null;
    }

    default ConfigKey map(String value) {
        return value != null ? ConfigKey.of(value) : null;
    }

    default String map(ConfigValue value) {
        return value != null ? value.value() : null;
    }

    default ConfigValue toConfigValue(String value) {
        return ConfigValue.of(value);
    }

    default String map(DisplayName name) {
        return name != null ? name.value() : null;
    }

    default DisplayName toDisplayName(String value) {
        return value != null ? DisplayName.of(value) : null;
    }

    default String map(ConfigGroup group) {
        return group != null ? group.getCode() : null;
    }

    default ConfigGroup toConfigGroup(String code) {
        return ConfigGroup.fromCode(code);
    }

    default String map(ValueType type) {
        return type != null ? type.getCode() : null;
    }

    default ValueType toValueType(String code) {
        return ValueType.fromCode(code);
    }

    default String map(InputType type) {
        return type != null ? type.getCode() : null;
    }

    default InputType toInputType(String code) {
        return InputType.fromCode(code);
    }
}
```

**Note**: MapStruct will use these default methods automatically when mapping between the value object types and String. The method names follow MapStruct's convention: it picks the method by matching parameter type and return type.

- [ ] **Step 2: Delete SystemConfigConfigure.java**

Delete: `app/src/main/java/org/smm/archetype/systemconfig/internal/SystemConfigConfigure.java`

- [ ] **Step 3: Verify compilation + existing tests**

Run: `mvn test -pl app -Dtest="*SystemConfig*UTest" -q`
Expected: All pass (may need to fix test import if test references SystemConfigConfigure)

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor(systemconfig): rewrite SystemConfigConverter as MapStruct interface with value object mappings, delete SystemConfigConfigure"
```

---

## Task 5: Domain Event Infrastructure

**Files:**
- Create: `app/src/main/java/org/smm/archetype/shared/event/DomainEvent.java`
- Create: `app/src/main/java/org/smm/archetype/shared/event/DomainEventPublisher.java`
- Create: `app/src/main/java/org/smm/archetype/shared/event/SpringDomainEventPublisher.java`

- [ ] **Step 1: Create DomainEvent interface**

```java
package org.smm.archetype.shared.event;

import java.time.Instant;

/**
 * 领域事件接口。
 * <p>
 * 所有领域事件 record 实现此接口，提供 eventId 和 occurredAt。
 * 零 Spring 依赖。
 */
public interface DomainEvent {

    /**
     * 事件唯一标识
     */
    String eventId();

    /**
     * 事件发生时间
     */
    Instant occurredAt();
}
```

- [ ] **Step 2: Create DomainEventPublisher interface**

```java
package org.smm.archetype.shared.event;

/**
 * 领域事件发布接口。
 * <p>
 * 零 Spring 依赖，由 Spring 适配器实现。
 */
@FunctionalInterface
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
```

- [ ] **Step 3: Create SpringDomainEventPublisher adapter**

```java
package org.smm.archetype.shared.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Spring ApplicationEventPublisher 适配实现。
 */
@Component
@RequiredArgsConstructor
class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher delegate;

    @Override
    public void publish(DomainEvent event) {
        delegate.publishEvent(event);
    }
}
```

- [ ] **Step 4: Verify compilation**

Run: `mvn compile -pl app -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/org/smm/archetype/shared/event/
git commit -m "feat(event): add DomainEvent interface + DomainEventPublisher + Spring adapter"
```

---

## Task 6: Example Events (UserLoggedInEvent + ConfigChangedEvent)

**Files:**
- Create: `app/src/main/java/org/smm/archetype/auth/UserLoggedInEvent.java` (root package = API package)
- Create: `app/src/main/java/org/smm/archetype/systemconfig/ConfigChangedEvent.java` (root package = API package)

- [ ] **Step 1: Create UserLoggedInEvent**

```java
package org.smm.archetype.auth;

import org.smm.archetype.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * 用户登录成功事件。
 * <p>
 * 放在 auth 模块根包（API 包），其他模块可消费。
 */
public record UserLoggedInEvent(
    String eventId,
    Instant occurredAt,
    String username,
    String ip
) implements DomainEvent {

    public UserLoggedInEvent {
        if (eventId == null || eventId.isBlank()) {
            eventId = UUID.randomUUID().toString();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    /**
     * 便捷工厂方法
     */
    public static UserLoggedInEvent of(String username, String ip) {
        return new UserLoggedInEvent(null, null, username, ip);
    }
}
```

- [ ] **Step 2: Create ConfigChangedEvent**

```java
package org.smm.archetype.systemconfig;

import org.smm.archetype.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * 系统配置变更事件。
 * <p>
 * 放在 systemconfig 模块根包（API 包），其他模块可消费。
 */
public record ConfigChangedEvent(
    String eventId,
    Instant occurredAt,
    String configKey,
    String oldValue,
    String newValue
) implements DomainEvent {

    public ConfigChangedEvent {
        if (eventId == null || eventId.isBlank()) {
            eventId = UUID.randomUUID().toString();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    /**
     * 便捷工厂方法
     */
    public static ConfigChangedEvent of(String configKey, String oldValue, String newValue) {
        return new ConfigChangedEvent(null, null, configKey, oldValue, newValue);
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `mvn compile -pl app -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/org/smm/archetype/auth/UserLoggedInEvent.java app/src/main/java/org/smm/archetype/systemconfig/ConfigChangedEvent.java
git commit -m "feat(event): add UserLoggedInEvent + ConfigChangedEvent records"
```

---

## Task 7: Event Handlers + Publishing Integration

**Files:**
- Create: `app/src/main/java/org/smm/archetype/operationlog/internal/UserLoggedInEventHandler.java`
- Create: `app/src/main/java/org/smm/archetype/auth/internal/ConfigChangedEventHandler.java`
- Modify: `app/src/main/java/org/smm/archetype/auth/internal/AuthFacadeImpl.java` (publish UserLoggedInEvent)
- Modify: `app/src/main/java/org/smm/archetype/systemconfig/internal/SystemConfigService.java` (publish ConfigChangedEvent)

- [ ] **Step 1: Create UserLoggedInEventHandler**

```java
package org.smm.archetype.operationlog.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.auth.UserLoggedInEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * 用户登录事件处理器。
 * <p>
 * 监听 auth 模块发布的 UserLoggedInEvent，异步记录登录日志。
 * 使用 @ApplicationModuleListener 实现异步 + 独立事务 + 自动重试。
 */
@Slf4j
@Component
@RequiredArgsConstructor
class UserLoggedInEventHandler {

    private final OperationLogRepository operationLogRepository;

    @ApplicationModuleListener
    void on(UserLoggedInEvent event) {
        log.info("Received login event: user={}, ip={}", event.username(), event.ip());
        // TODO: 后续可在此记录登录日志到 operation_log 表
        // 当前仅做日志输出，作为事件基础设施的示例
    }
}
```

- [ ] **Step 2: Create ConfigChangedEventHandler**

```java
package org.smm.archetype.auth.internal;

import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.systemconfig.ConfigChangedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 配置变更事件处理器。
 * <p>
 * 监听 systemconfig 模块发布的 ConfigChangedEvent，同步刷新认证相关配置。
 */
@Slf4j
@Component
class ConfigChangedEventHandler {

    @EventListener
    void on(ConfigChangedEvent event) {
        log.info("Config changed: key={}, old={}, new={}",
                event.configKey(), event.oldValue(), event.newValue());
        // TODO: 后续可在此刷新认证相关配置缓存
        // 当前仅做日志输出，作为事件基础设施的示例
    }
}
```

- [ ] **Step 3: Modify AuthFacadeImpl to publish UserLoggedInEvent**

Add DomainEventPublisher injection and publish event after successful login:

```java
package org.smm.archetype.auth.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.auth.AuthFacade;
import org.smm.archetype.auth.UserLoggedInEvent;
import org.smm.archetype.component.auth.AuthComponent;
import org.smm.archetype.exception.BizException;
import org.smm.archetype.exception.CommonErrorCode;
import org.smm.archetype.shared.event.DomainEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class AuthFacadeImpl implements AuthFacade {

    private final UserRepository userRepository;
    private final AuthComponent authComponent;
    private final DomainEventPublisher eventPublisher;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public String login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException(CommonErrorCode.AUTH_USER_NOT_FOUND, "用户不存在"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BizException(CommonErrorCode.AUTH_BAD_CREDENTIALS, "用户名或密码错误");
        }

        String token = authComponent.login(user.getId());

        // 发布登录成功事件
        eventPublisher.publish(UserLoggedInEvent.of(username, null));

        return token;
    }

    @Override
    public void logout() {
        authComponent.logout();
    }
}
```

- [ ] **Step 4: Modify SystemConfigService to publish ConfigChangedEvent**

Add DomainEventPublisher injection and publish event after config update:

```java
// Add import at top:
import org.smm.archetype.systemconfig.ConfigChangedEvent;
import org.smm.archetype.shared.event.DomainEventPublisher;

// Add field:
private final DomainEventPublisher eventPublisher;

// Modify updateConfig method:
@Transactional
public void updateConfig(UpdateConfigCommand command) {
    log.info("Updating config: {}", command.configKey());
    SystemConfig config = systemConfigRepository.findByConfigKey(ConfigKey.of(command.configKey()))
            .orElseThrow(() -> new IllegalArgumentException("Config not found: " + command.configKey()));
    String oldValue = config.getConfigValue() != null ? config.getConfigValue().value() : null;
    config.updateValue(ConfigValue.of(command.configValue()));
    systemConfigRepository.save(config);
    log.info("Config updated: {}", command.configKey());

    // 发布配置变更事件
    eventPublisher.publish(ConfigChangedEvent.of(command.configKey(), oldValue, command.configValue()));
}
```

Full updated SystemConfigService:

```java
package org.smm.archetype.systemconfig.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.shared.event.DomainEventPublisher;
import org.smm.archetype.shared.pagination.PageResult;
import org.smm.archetype.systemconfig.ConfigChangedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 系统配置服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * 获取所有配置分组（返回枚举值，由 Facade 层转换为 VO）
     */
    @Transactional(readOnly = true)
    public List<ConfigGroup> getAllGroups() {
        return Arrays.stream(ConfigGroup.values()).toList();
    }

    @Transactional(readOnly = true)
    public List<SystemConfig> getConfigsByGroup(String groupCode) {
        ConfigGroup group = ConfigGroup.fromCode(groupCode);
        if (group == null) {
            throw new IllegalArgumentException("Invalid group: " + groupCode);
        }
        return systemConfigRepository.findByGroupCode(group);
    }

    @Transactional(readOnly = true)
    public List<SystemConfig> getAllConfigs() {
        return systemConfigRepository.findAll();
    }

    @Transactional(readOnly = true)
    public SystemConfig getConfigByKey(String key) {
        return systemConfigRepository.findByConfigKey(ConfigKey.of(key))
                .orElse(null);
    }

    @Transactional
    public void updateConfig(UpdateConfigCommand command) {
        log.info("Updating config: {}", command.configKey());
        SystemConfig config = systemConfigRepository.findByConfigKey(ConfigKey.of(command.configKey()))
                .orElseThrow(() -> new IllegalArgumentException("Config not found: " + command.configKey()));
        String oldValue = config.getConfigValue() != null ? config.getConfigValue().value() : null;
        config.updateValue(ConfigValue.of(command.configValue()));
        systemConfigRepository.save(config);
        log.info("Config updated: {}", command.configKey());

        // 发布配置变更事件
        eventPublisher.publish(ConfigChangedEvent.of(command.configKey(), oldValue, command.configValue()));
    }

    /**
     * 分页查询系统配置
     *
     * @param query 分页查询参数
     * @return 分页结果（框架无关）
     */
    @Transactional(readOnly = true)
    public PageResult<SystemConfig> findByPage(SystemConfigPageQuery query) {
        return systemConfigRepository.findByPage(query);
    }
}
```

- [ ] **Step 5: Verify compilation + run all tests**

Run: `mvn test -pl app -q`
Expected: BUILD SUCCESS (570+ tests, JaegerDataVerificationITest error expected)

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat(event): add event handlers + publishing in AuthFacadeImpl and SystemConfigService"
```

---

## Task 8: internal/ Two-Layer Sub-packaging (Move Files to infrastructure/)

**Files:**
Move files in all 3 modules from `internal/` flat to `internal/infrastructure/` for technical implementations.

### 8a: auth module

- [ ] **Step 1: Create infrastructure directory and move files**

Move these files from `auth/internal/` to `auth/internal/infrastructure/`:
- `UserDO.java`
- `UserMapper.java`
- `UserRepositoryImpl.java`
- `UserConverter.java` (already MapStruct interface from Task 2)

For each file, update `package` declaration from:
```java
package org.smm.archetype.auth.internal;
```
to:
```java
package org.smm.archetype.auth.internal.infrastructure;
```

And add any needed imports for types now in a different package (User, UserRepository interface stay in `internal/`).

**UserDO.java** — update package + add imports:
```java
package org.smm.archetype.auth.internal.infrastructure;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import org.smm.archetype.auth.internal.User;
import org.smm.archetype.shared.dal.BaseDO;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("user")
class UserDO extends BaseDO {
    private String username;
    private String passwordHash;
    private String nickname;
    private String status;
}
```

**UserMapper.java** — update package + add imports:
```java
package org.smm.archetype.auth.internal.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
// ... other imports unchanged
```

**UserRepositoryImpl.java** — update package + add imports for User, UserRepository, UserMapper, UserDO:
```java
package org.smm.archetype.auth.internal.infrastructure;

// Add imports for types in parent package:
import org.smm.archetype.auth.internal.User;
import org.smm.archetype.auth.internal.UserRepository;
// ... rest unchanged
```

**UserConverter.java** — update package + add imports:
```java
package org.smm.archetype.auth.internal.infrastructure;

import org.mapstruct.Mapper;
import org.smm.archetype.auth.internal.User;
import org.smm.archetype.shared.CentralMapperConfig;

@Mapper(config = CentralMapperConfig.class)
interface UserConverter {
    User toModel(UserDO userDO);
    UserDO toDO(User user);
}
```

- [ ] **Step 2: Delete old files from auth/internal/ that were moved**

After creating new files in infrastructure/, delete the old files from auth/internal/:
- Delete `auth/internal/UserDO.java`
- Delete `auth/internal/UserMapper.java`
- Delete `auth/internal/UserRepositoryImpl.java`
- Delete `auth/internal/UserConverter.java`
- Delete `auth/internal/AuthConfigure.java` (already deleted in Task 2, verify it's gone)

- [ ] **Step 3: Update test imports for auth module**

Search all test files under `app/src/test/` that import from `auth.internal.UserDO`, `auth.internal.UserMapper`, `auth.internal.UserRepositoryImpl`, `auth.internal.UserConverter` and update to `auth.internal.infrastructure.*`.

### 8b: operationlog module

- [ ] **Step 4: Move files to infrastructure/**

Move from `operationlog/internal/` to `operationlog/internal/infrastructure/`:
- `OperationLogDO.java`
- `OperationLogMapper.java`
- `OperationLogRepositoryImpl.java`
- `OperationLogConverter.java`

Update package declarations and add imports for types staying in parent package (OperationLog, OperationLogRepository, OperationLogFacade).

- [ ] **Step 5: Delete old files + update test imports**

### 8c: systemconfig module

- [ ] **Step 6: Move files to infrastructure/**

Move from `systemconfig/internal/` to `systemconfig/internal/infrastructure/`:
- `SystemConfigDO.java`
- `SystemConfigMapper.java`
- `SystemConfigRepositoryImpl.java`
- `SystemConfigConverter.java`

Update package declarations and add imports for types staying in parent package (SystemConfig, SystemConfigRepository, value objects, enums).

**SystemConfigConverter.java** — needs many imports since it uses value objects:
```java
package org.smm.archetype.systemconfig.internal.infrastructure;

import org.mapstruct.Mapper;
import org.smm.archetype.shared.CentralMapperConfig;
import org.smm.archetype.systemconfig.internal.SystemConfig;
import org.smm.archetype.systemconfig.internal.SystemConfigDO;
import org.smm.archetype.systemconfig.internal.ConfigGroup;
import org.smm.archetype.systemconfig.internal.ConfigKey;
import org.smm.archetype.systemconfig.internal.ConfigValue;
import org.smm.archetype.systemconfig.internal.DisplayName;
import org.smm.archetype.systemconfig.internal.InputType;
import org.smm.archetype.systemconfig.internal.ValueType;

@Mapper(config = CentralMapperConfig.class)
interface SystemConfigConverter {
    // ... methods unchanged
}
```

- [ ] **Step 7: Delete old files + update test imports**

- [ ] **Step 8: Verify compilation**

Run: `mvn compile -pl app -q`
Expected: BUILD SUCCESS

- [ ] **Step 9: Run all tests**

Run: `mvn test -pl app -q`
Expected: All pass

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "refactor: move DO/Mapper/RepositoryImpl/Converter to internal/infrastructure/ sub-package"
```

---

## Task 9: Update ArchUnit M-05 for infrastructure/ Exception

**Files:**
- Modify: `app/src/test/java/org/smm/archetype/support/basic/ModuleArchitectureComplianceUTest.java`

- [ ] **Step 1: Update M-05 test**

The current M-05 exception list checks by simple name suffix (Controller, Service, Converter, RepositoryImpl, FacadeImpl, Configure). After moving to infrastructure/, we need to add the infrastructure package as an exception.

Add `.and().resideOutsideOfPackage("..internal.infrastructure..")` to the exclusion predicates:

In the M-05 test method, update the rule to:

```java
@Test
@DisplayName("M-05: 模块 internal/ 包零 Spring 依赖（infrastructure/ 包 + Controller/Service/RepositoryImpl/FacadeImpl/测试类除外）")
void module_internal_should_not_depend_on_spring_except_allowed() {
    List<String> modules = discoverBusinessModules();

    for (String module : modules) {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..archetype." + module + ".internal..")
                // infrastructure/ 包允许 Spring 依赖
                .and().resideOutsideOfPackage("..archetype." + module + ".internal.infrastructure..")
                // 上层中需要 Spring 的组件
                .and().haveSimpleNameNotEndingWith("Controller")
                .and().haveSimpleNameNotEndingWith("Service")
                .and().haveSimpleNameNotEndingWith("RepositoryImpl")
                .and().haveSimpleNameNotEndingWith("FacadeImpl")
                .and().haveSimpleNameNotEndingWith("EventHandler")
                .and(new DescribedPredicate<>("is not a test class") {
                    @Override
                    public boolean test(JavaClass javaClass) {
                        return !isTestClass(javaClass);
                    }
                })
                .should().dependOnClassesThat()
                .resideInAPackage("org.springframework..")
                .allowEmptyShould(true)
                .check(importedClasses);
    }
}
```

**Changes:**
- Removed `Converter` and `Configure` from name-based exceptions (they're now in infrastructure/)
- Added `resideOutsideOfPackage("..internal.infrastructure..")` to allow Spring in entire infrastructure sub-package
- Added `EventHandler` exception for event handlers using `@Component`/`@EventListener`
- Updated display name

- [ ] **Step 2: Run ArchUnit tests**

Run: `mvn test -pl app -Dtest="ModuleArchitectureComplianceUTest" -q`
Expected: All pass

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "refactor(archunit): update M-05 to allow Spring in infrastructure/ sub-package"
```

---

## Task 10: Update Modulith allowedDependencies + Enable Verification

**Files:**
- Modify: `app/src/main/java/org/smm/archetype/auth/package-info.java`
- Modify: `app/src/main/java/org/smm/archetype/operationlog/package-info.java`
- Modify: `app/src/test/java/org/smm/archetype/support/basic/ModulithComplianceUTest.java`

- [ ] **Step 1: Update auth/package-info.java**

```java
@org.springframework.modulith.ApplicationModule(
        displayName = "Authentication",
        allowedDependencies = {"shared", "systemconfig"}
)
package org.smm.archetype.auth;
```

(auth consumes ConfigChangedEvent from systemconfig)

- [ ] **Step 2: Update operationlog/package-info.java**

```java
@org.springframework.modulith.ApplicationModule(
        displayName = "Operation Log",
        allowedDependencies = {"shared", "auth"}
)
package org.smm.archetype.operationlog;
```

(operationlog consumes UserLoggedInEvent from auth)

- [ ] **Step 3: Update ModulithComplianceUTest**

Remove @Disabled and add comprehensive tests:

```java
package org.smm.archetype.support.basic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.WebStartLightApplication;
import org.smm.archetype.support.UnitTestBase;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Spring Modulith 模块结构验证")
class ModulithComplianceUTest extends UnitTestBase {

    @Test
    @DisplayName("应验证模块结构合规（含边界验证、循环依赖检测）")
    void should_verifyModulithStructure() {
        ApplicationModules modules = ApplicationModules.of(WebStartLightApplication.class);
        modules.verify();
    }

    @Test
    @DisplayName("应验证模块命名符合约定")
    void should_verifyModuleNames() {
        ApplicationModules modules = ApplicationModules.of(WebStartLightApplication.class);
        assertThat(modules.stream().map(m -> m.getDisplayName()).toList())
                .containsExactlyInAnyOrder(
                        "Authentication", "Operation Log", "System Configuration", "Shared Cross-Cutting");
    }

    @Test
    @DisplayName("应验证所有模块均使用 @ApplicationModule 显式声明")
    void should_verifyAllModulesExplicitlyDeclared() {
        ApplicationModules modules = ApplicationModules.of(WebStartLightApplication.class);
        assertThat(modules.stream().count()).isEqualTo(4);
    }
}
```

- [ ] **Step 4: Run Modulith tests**

Run: `mvn test -pl app -Dtest="ModulithComplianceUTest" -q`
Expected: All 3 tests pass

If modules.verify() fails, inspect the error output to identify violations and fix them:
- Check if any module accesses another module's internal/ without being in allowedDependencies
- Check if event records in root packages are accessible
- Expand allowedDependencies if needed

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor(modulith): update allowedDependencies + enable modules.verify() + add comprehensive tests"
```

---

## Task 11: Full Verification + Cleanup

- [ ] **Step 1: Run full test suite**

Run: `mvn test -pl app`
Expected: BUILD SUCCESS, 0 failures

Acceptable: 1 error from JaegerDataVerificationITest (Docker dependency), 1 skipped from ModulithComplianceUTest if any issue

- [ ] **Step 2: Run ArchUnit compliance tests specifically**

Run: `mvn test -pl app -Dtest="*ComplianceUTest,NoRedundantConfigureUTest" -q`
Expected: All pass

- [ ] **Step 3: Verify no empty directories**

Run: `find app/src -type d -empty`
Expected: No output

- [ ] **Step 4: Update AGENTS.md if needed**

Update shared/ description to include `event/` package. Verify module structure documentation reflects infrastructure/ sub-packages.

- [ ] **Step 5: Final commit**

```bash
git add -A
git commit -m "docs: update AGENTS.md with event infrastructure + infrastructure sub-package docs"
```

---

## Execution Notes

- **Task 1-3** can be parallelized (MapStruct config, 3 Converter rewrites)
- **Task 4** (SystemConfigConverter) should come after Task 1 (CentralMapperConfig)
- **Task 5** (event infrastructure) can be parallelized with Task 1-4
- **Task 6-7** depend on Task 5
- **Task 8** (sub-packaging) can be done in parallel with Tasks 5-7
- **Task 9** (ArchUnit) depends on Task 8
- **Task 10** (Modulith) depends on Tasks 6-9
- **Task 11** (verification) depends on all
