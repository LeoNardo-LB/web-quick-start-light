# Phase 6: Architecture Evolution — MapStruct + DDD Sub-packaging + Domain Event + Modulith Verification

> 日期: 2026-05-13
> 状态: Approved
> 前置: Phase 5b (shared/ functional domain reorganization) — commit `7ae6a31`

---

## 1. 概述

一次性实施 4 个架构演进方向，将 web-quick-start-light 从"教科书级 Modular Monolith"推向"生产级 Modular Monolith"。

| 方向 | 工作量 | 目标 |
|------|--------|------|
| MapStruct 激活 | 小 | 3 个手写 Converter → MapStruct @Mapper 接口 |
| internal/ 两层分包 | 中 | 业务代码 vs 技术实现分离 |
| Domain Event | 大 | 事件基础设施 + 2 个示例业务事件 |
| Spring Modulith 边界验证 | 中 | 启用 modules.verify() + 补充验证测试 |

---

## 2. MapStruct 激活

### 2.1 现状

- MapStruct 1.6.3 依赖已引入，mapstruct-processor 已配置
- 3 个手写 Converter：`UserConverter`、`OperationLogConverter`、`SystemConfigConverter`
- 由模块 `XxxConfigure` 通过 `@Bean` 注册
- 方法名：`toModel()` / `toDO()`

### 2.2 目标

- Converter 接口改为 `@Mapper`（MapStruct 注解，非 Spring 注解）
- 保持 `XxxConverter` 命名（规避 MyBatis `XxxMapper` 冲突）
- 保持 `toModel()` / `toDO()` 方法名
- 使用 `componentModel = "spring"`，MapStruct 生成 `@Component` Impl 类
- Converter 在 ArchUnit M-05 例外列表中，不冲突

### 2.3 全局配置

**新增 `@MapperConfig` 集中配置**（放在 `shared/` 下合适位置）：

```java
@MapperConfig(
    componentModel = MappingConstants.ComponentModel.SPRING,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface CentralMapperConfig {
}
```

**Maven compiler 全局参数**（app/pom.xml）：

```xml
<compilerArgs>
    <arg>-Amapstruct.defaultComponentModel=spring</arg>
    <arg>-Amapstruct.defaultInjectionStrategy=constructor</arg>
    <arg>-Amapstruct.unmappedTargetPolicy=ERROR</arg>
</compilerArgs>
```

### 2.4 Converter 改造

以 `UserConverter` 为例：

```java
// 改造前: 手写 class
class UserConverter {
    User toModel(UserDO userDO) { ... }
    UserDO toDO(User user) { ... }
}

// 改造后: MapStruct 接口
@Mapper(config = CentralMapperConfig.class)
interface UserConverter {
    User toModel(UserDO userDO);
    UserDO toDO(User user);
}
```

### 2.5 Configure 类调整

MapStruct 生成 `@Component` Impl 后，`XxxConfigure` 不再需要手动 `@Bean` 注册 Converter：

```java
// 改造前
@Configuration
class AuthConfigure {
    @Bean
    UserConverter userConverter() { return new UserConverter(); }
}

// 改造后: Converter 自动注册为 Spring Bean，Configure 可删除 Converter 的 @Bean
// 如果 Configure 只有 Converter 的 @Bean，则整个 Configure 可删除
// 如果 Configure 还有其他 Bean，仅移除 Converter 的 @Bean
```

### 2.6 约束

- 所有现有测试方法签名不变
- ArchUnit M-05 例外列表中 Converter 已存在，无需调整
- MapStruct `@Mapper` 是 MapStruct 注解（非 Spring 注解），不违反 M-05

---

## 3. internal/ 两层分包

### 3.1 现状

所有模块 `internal/` 下文件扁平排列（11~21 个文件/模块）。

### 3.2 目标结构

**两层分离：业务代码在上层，技术实现在 infrastructure/**

```
模块/
├── XxxFacade.java                              # 公开 API
├── package-info.java                           # Modulith 声明
└── internal/
    ├── Entity.java                             # 业务实体
    ├── XxxService.java                         # 业务逻辑
    ├── XxxFacadeImpl.java                      # Facade 实现
    ├── XxxVO.java                              # 响应 VO (record)
    ├── XxxPageQuery.java                       # 分页查询
    ├── XxxRequest.java                         # 请求 DTO
    ├── XxxCommand.java                         # 命令对象
    ├── 值对象.java / 枚举.java                    # 值对象和枚举
    ├── XxxController.java                      # Controller
    ├── XxxRepository.java                      # Repository 接口（框架无关）
    ├── XxxEvent.java                           # 领域事件（如有）
    ├── XxxEventHandler.java                    # 事件处理器（如有）
    │
    └── infrastructure/                         # 技术实现
        ├── XxxDO.java                          # 数据库映射
        ├── XxxMapper.java                      # MyBatis Mapper
        ├── XxxRepositoryImpl.java              # Repository 实现
        ├── XxxConverter.java                   # MapStruct 转换器
        └── XxxConfigure.java                   # @Configuration + @Bean
```

### 3.3 分层规则

| 层 | 内容 | 特征 |
|----|------|------|
| `internal/` 上层 | Entity、Service、FacadeImpl、Controller、VO、DTO、Repository 接口、值对象、枚举、事件 | 业务核心，依赖 Repository 接口不依赖 infrastructure |
| `internal/infrastructure/` | DO、Mapper、RepositoryImpl、Converter、Configure | 技术实现，依赖上层 Entity 和接口 |

**依赖方向**：上层 → Repository 接口 ← infrastructure/（实现接口）

### 3.4 各模块具体文件分配

#### auth/internal/ (11 files → 上层 8 + infrastructure 4)

根包（API 包，事件）：
- `UserLoggedInEvent.java`（新增，事件 record — 根包以便其他模块消费）

上层 `internal/`：
- `AuthFacadeImpl.java`、`LoginController.java`
- `User.java`（Entity）
- `LoginRequest.java`、`LoginVO.java`
- `UserRepository.java`（接口）
- `ConfigChangedEventHandler.java`（新增，消费 systemconfig 的事件）
- `AuthConfigure.java` → 评估是否保留（如果只有 Converter @Bean 则可删除）

infrastructure/：
- `UserDO.java`、`UserMapper.java`
- `UserRepositoryImpl.java`
- `UserConverter.java`

#### operationlog/internal/ (12 files → 上层 9 + infrastructure 4)

上层 `internal/`：
- `OperationLogFacadeImpl.java`、`OperationLogController.java`
- `OperationLog.java`（Entity）
- `OperationLogService.java`
- `OperationLogVO.java`、`OperationLogPageQuery.java`
- `OperationLogRepository.java`（接口）
- `UserLoggedInEventHandler.java`（新增，事件处理器）

infrastructure/：
- `OperationLogDO.java`、`OperationLogMapper.java`
- `OperationLogRepositoryImpl.java`
- `OperationLogConverter.java`
- `OperationLogConfigure.java`

#### systemconfig/internal/ (21 files → 上层 14 + infrastructure 5)

根包（API 包，事件）：
- `ConfigChangedEvent.java`（新增，事件 record — 根包以便其他模块消费）

上层 `internal/`：
- `SystemConfigFacadeImpl.java`、`SystemConfigController.java`
- `SystemConfig.java`（Entity）
- `SystemConfigService.java`
- `SystemConfigVO.java`、`ConfigGroupVO.java`
- `SystemConfigPageQuery.java`、`UpdateConfigRequest.java`、`UpdateConfigCommand.java`
- `ConfigGroup.java`、`ConfigKey.java`、`ConfigValue.java`、`DisplayName.java`（值对象）
- `InputType.java`、`ValueType.java`（枚举）
- `SystemConfigRepository.java`（接口）

infrastructure/：
- `SystemConfigDO.java`、`SystemConfigMapper.java`
- `SystemConfigRepositoryImpl.java`
- `SystemConfigConverter.java`
- `SystemConfigConfigure.java`

### 3.5 ArchUnit 适配

- M-05 例外列表：从具体类名（Controller/Service/Converter/...）更新为 `infrastructure/` 整包例外 + 上层中的 Controller/FacadeImpl
- 更准确：`internal/infrastructure/` 包下允许 Spring 依赖，上层除 Controller 和 FacadeImpl 外零 Spring 依赖
- M-07（模块间不得直接访问其他模块 internal/）保持不变
- M-06（Repository 接口方法签名不得出现 MyBatis-Plus 类型）保持不变

### 3.6 约束

- 分包后所有 import 路径变更，测试文件需同步更新
- `package-info.java` 的 `@ApplicationModule` 不变（仍在模块根包）
- shared/ 模块不分子包（已经是功能域自包含）

---

## 4. Domain Event

### 4.1 事件基础设施

**新增文件**（全部在 `shared/event/`）：

```
shared/event/
├── DomainEvent.java                  # 事件接口（零 Spring 依赖）
├── DomainEventPublisher.java         # 事件发布接口（零 Spring 依赖）
└── SpringDomainEventPublisher.java   # ApplicationEventPublisher 适配实现
```

**DomainEvent 接口**（Java record 不能 extends record，因此用接口定义契约）：

```java
public interface DomainEvent {
    String eventId();
    Instant occurredAt();
}
```

**DomainEventPublisher 接口**：

```java
public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
```

**Spring 适配实现**（在 `shared/event/` 中，因为 shared/ 不分子包）：

```java
@Component
class SpringDomainEventPublisher implements DomainEventPublisher {
    private final ApplicationEventPublisher delegate;

    // constructor injection

    @Override
    public void publish(DomainEvent event) {
        delegate.publishEvent(event);
    }
}
```

### 4.2 示例事件 1：UserLoggedInEvent

**事件 record 必须放在模块根包**（API 包），以便其他模块可以消费（Modulith 边界验证要求）。

**发布模块：auth**

```
auth/UserLoggedInEvent.java           # 根包（API 包），其他模块可消费
```

```java
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
}
```

**消费模块：operationlog**

```
operationlog/internal/UserLoggedInEventHandler.java
```

```java
@Component
class UserLoggedInEventHandler {
    private final OperationLogRepository logRepository;
    // constructor injection

    @ApplicationModuleListener  // 异步 + 独立事务 + 自动重试
    void on(UserLoggedInEvent event) {
        // 异步记录登录日志
    }
}
```

### 4.3 示例事件 2：ConfigChangedEvent

**发布模块：systemconfig**

```
systemconfig/ConfigChangedEvent.java   # 根包（API 包），其他模块可消费
```

```java
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
}
```

**消费模块：auth**

```
auth/internal/ConfigChangedEventHandler.java
```

```java
@Component
class ConfigChangedEventHandler {
    @EventListener  // 同步通知，缓存刷新需即时
    void on(ConfigChangedEvent event) {
        // 刷新认证相关配置缓存
    }
}
```

### 4.4 事件通信模式

| 事件 | 发布模块 | 消费模块 | 模式 | 一致性 |
|------|---------|---------|------|--------|
| `UserLoggedInEvent` | auth | operationlog | `@ApplicationModuleListener` | 异步 + 独立事务 |
| `ConfigChangedEvent` | systemconfig | auth | `@EventListener` | 同步 |

### 4.5 发布点

- `UserLoggedInEvent`：在 `AuthFacadeImpl.login()` 成功后发布
- `ConfigChangedEvent`：在 `SystemConfigService` 更新配置成功后发布

### 4.6 约束

- 事件 record 仅包含基本类型字段（String、Instant、数值），不传递 Entity
- 事件 record 放在**模块根包**（API 包），使得其他模块可消费（Modulith 边界验证要求）
- 事件处理器（EventHandler）放在消费模块的 `internal/` 中
- `@ApplicationModuleListener` 由 Spring Modulith 提供，自带事务边界隔离和自动重试
- DomainEvent 接口在 `shared/event/`，零 Spring 依赖
- SpringDomainEventPublisher 适配器在 `shared/event/` 中

---

## 5. Spring Modulith 边界验证启用

### 5.1 现状

- `ModulithComplianceUTest.should_verifyModulithStructure()` 被 `@Disabled`
- 4 个 `package-info.java` 已声明 `@ApplicationModule`
- 失败原因：跨包依赖违规

### 5.2 目标

1. 启用 `modules.verify()` 测试（移除 `@Disabled`）
2. 修复所有模块边界违规
3. 更新 `allowedDependencies` 反映事件依赖关系
4. 补充额外验证测试

### 5.3 事件文件位置确认

事件 record 放在**模块根包**（API 包），其他模块可直接消费：

```
auth/
├── UserLoggedInEvent.java             # API 包，operationlog 可消费
├── AuthFacade.java
├── package-info.java
└── internal/
    ├── ConfigChangedEventHandler.java  # 消费 systemconfig 的事件
    └── ...

systemconfig/
├── ConfigChangedEvent.java            # API 包，auth 可消费
├── SystemConfigFacade.java
├── package-info.java
└── internal/
    └── ...
```

### 5.4 allowedDependencies 更新

```java
// auth — 消费 ConfigChangedEvent（来自 systemconfig）
@ApplicationModule(displayName = "Authentication", allowedDependencies = {"shared", "systemconfig"})
package org.smm.archetype.auth;

// operationlog — 消费 UserLoggedInEvent（来自 auth）
@ApplicationModule(displayName = "Operation Log", allowedDependencies = {"shared", "auth"})
package org.smm.archetype.operationlog;

// systemconfig — 无额外依赖
@ApplicationModule(displayName = "System Configuration", allowedDependencies = {"shared"})
package org.smm.archetype.systemconfig;

// shared — 保持 OPEN
@ApplicationModule(displayName = "Shared Cross-Cutting", type = Type.OPEN)
package org.smm.archetype.shared;
```

### 5.5 ModulithComplianceUTest 增强

```java
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
        .containsExactlyInAnyOrder("Authentication", "Operation Log", "System Configuration", "Shared Cross-Cutting");
}

@Test
@DisplayName("应验证所有模块均使用 @ApplicationModule 显式声明")
void should_verifyAllModulesExplicitlyDeclared() {
    ApplicationModules modules = ApplicationModules.of(WebStartLightApplication.class);
    assertThat(modules.stream().count()).isEqualTo(4);
}
```

### 5.6 约束

- `modules.verify()` 必须通过（CI 级别门禁）
- shared 保持 `Type.OPEN`
- 业务模块 `allowedDependencies` 精确声明
- 事件 record 放在模块根包（API 包）

---

## 6. shared/ 模块新增文件汇总

| 文件 | 包路径 | 说明 |
|------|--------|------|
| `DomainEvent.java` | `shared/event/` | 事件接口（eventId + occurredAt） |
| `DomainEventPublisher.java` | `shared/event/` | 事件发布接口 |
| `SpringDomainEventPublisher.java` | `shared/event/` | Spring ApplicationEventPublisher 适配实现 |
| `CentralMapperConfig.java` | `shared/` | MapStruct 全局 @MapperConfig |

---

## 7. 测试策略

### 7.1 现有测试

- 所有现有测试方法签名不变（Converter toModel/toDO 方法名不变）
- import 路径变更需同步更新测试文件
- `XxxConfigure` 如被删除，对应测试（如有）同步删除或调整

### 7.2 新增测试

| 测试文件 | 测试内容 |
|---------|---------|
| `ModulithComplianceUTest` | modules.verify()、无循环依赖、模块命名、显式声明 |
| `UserLoggedInEventUTest` | 事件 record 构建、字段校验 |
| `ConfigChangedEventUTest` | 事件 record 构建、字段校验 |
| `DomainEventPublisherUTest` | 接口契约验证 |
| `UserConverterUTest` | MapStruct 映射正确性（已有测试需验证） |
| `SystemConfigConverterUTest` | MapStruct 映射正确性 |
| `OperationLogConverterUTest` | MapStruct 映射正确性 |
| `UserLoggedInEventHandlerITest` | 事件处理集成测试 |

### 7.3 验证命令

```bash
mvn test -pl app
# 预期：全部通过，无 failures，无 errors（除 JaegerDataVerificationITest Docker 依赖）
```

---

## 8. 实施顺序

| 步骤 | 内容 | 依赖 |
|------|------|------|
| Step 1 | MapStruct 激活（全局配置 + 3 个 Converter 改造 + Configure 调整） | 无 |
| Step 2 | internal/ 两层分包（3 个模块文件迁移 + import 修复 + 测试同步） | 无 |
| Step 3 | Domain Event 基础设施（DomainEvent + DomainEventPublisher + 适配器） | 无 |
| Step 4 | 示例事件（UserLoggedInEvent + ConfigChangedEvent + Handler） | Step 2, Step 3 |
| Step 5 | Spring Modulith 边界验证（allowedDependencies 更新 + @Disabled 移除 + 新增测试） | Step 1-4 |

Step 1 和 Step 2 无依赖关系，可并行。Step 3 也可与前两步并行。Step 4 依赖 2 和 3。Step 5 依赖全部。

---

## 9. 风险与缓解

| 风险 | 影响 | 缓解 |
|------|------|------|
| MapStruct 1.6.3 与 Lombok edge-SNAPSHOT (Java 25) 不兼容 | 编译失败 | 测试编译；如有问题降级 Lombok 或升级 MapStruct |
| modules.verify() 发现未知违规 | 测试失败 | 逐项修复，必要时扩展 allowedDependencies |
| @ApplicationModuleListener 异步行为导致测试不稳定 | ITest 偶发失败 | 使用 AssertablePublishedEvents 或 Scenario API |
| internal/ 分包后 import 变更量大 | 修改文件多 | IDE refactor 辅助 + 全量编译验证 |
