# 编码规范 (Coding Standards)

> **职责**: 定义项目编码规范和架构守护规则
> **轨道**: Constraint
> **维护者**: Human

---

## 目录

- [概述](#概述)
- [编码规则](#编码规则)
  - [分层架构规则](#分层架构规则)
  - [依赖方向规则](#依赖方向规则)
  - [Entity 纯净性规则](#entity-纯净性规则)
  - [注解使用规则](#注解使用规则)
  - [依赖注入规则](#依赖注入规则)
  - [代码生成包规则](#代码生成包规则)
- [代码示例](#代码示例)
  - [分层结构示例](#分层结构示例)
  - [命名规范示例](#命名规范示例)
  - [注解使用示例](#注解使用示例)
- [违规示例](#违规示例)
- [检查方法](#检查方法)
  - [ArchUnit 架构守护测试](#archunit-架构守护测试)
  - [NoDataAnnotation 检查](#nodataannotation-检查)
  - [NoRedundantConfigure 检查](#noredundantconfigure-检查)
  - [NoValueInjection 检查](#novalueinjection-检查)
- [检查清单](#检查清单)
- [相关文档](#相关文档)
- [变更历史](#变更历史)

---

## 概述

项目采用 **ArchUnit 架构守护测试** 自动化强制编码规范，所有规则在 CI 中作为单元测试执行。违反任何规则将导致构建失败。

规范覆盖四个维度：

| 维度 | 规则数 | 执行方式 |
|------|:------:|---------|
| 分层架构 | 6 | ArchUnit 自动化 |
| 注解约束 | 2 | ArchUnit 自动化 |
| Configure 约束 | 1 | ArchUnit 自动化 |
| DI 约束 | 1 | ArchUnit 自动化 |

---

## 编码规则

### 分层架构规则

项目采用 Controller → Facade → Service → Repository → Mapper 五层架构。

| # | 规则 | 说明 | 严重级别 |
|---|------|------|---------|
| R1 | Controller 不得直接依赖 Mapper | Controller 只能依赖 Facade，不能跳过 Facade/Service 直接调用 Repository Mapper | 🔴 严重 |
| R2 | Service 不得依赖 Controller | Service 层不能反向依赖 Controller | 🔴 严重 |
| R3 | Repository 不得依赖 Service 或 Controller | Repository 层保持向下独立 | 🔴 严重 |
| R4 | Entity 不得依赖 Spring Framework | Entity 保持 POJO 纯净性 | 🔴 严重 |
| R5 | Controller 不得直接依赖 Service（Login 除外） | Controller 通过 Facade 调用 Service，Login 例外 | 🟡 警告 |
| R6 | Facade 不得直接依赖 Repository | Facade 通过 Service 调用 Repository | 🟡 警告 |

### 依赖方向规则

```
Controller → Facade → Service → Repository → Mapper
     │           │          │
     │           │          └── Entity (纯 POJO)
     │           └── Entity (纯 POJO)
     └── Entity (纯 POJO)
```

**允许的依赖**：
- Controller → Facade
- Controller → Entity（值对象）
- Facade → Service
- Facade → Entity
- Service → Repository
- Service → Entity
- Repository → Mapper
- Repository → Entity

**禁止的依赖**：
- 任何层 → Mapper（Controller/Service/Facade 不可直接使用 Mapper）
- Service → Controller（反向依赖）
- Repository → Service/Controller（向上依赖）
- Entity → Spring Framework（纯净性）

### Entity 纯净性规则

| # | 规则 | 说明 |
|---|------|------|
| R7 | Entity 禁止使用 `@Data` | Lombok `@Data` 生成 `equals/hashCode/toString` 可能暴露敏感字段 |
| R7b | Entity 推荐使用 `@Getter/@Setter` | 精确控制字段访问级别 |

### 注解使用规则

| # | 规则 | 说明 |
|---|------|------|
| R8 | 禁止 `@Value` 字段注入 | 必须使用构造器注入（Properties 类） |
| R8b | 配置通过 Properties 类绑定 | `@ConfigurationProperties` + 构造器注入 |

### 依赖注入规则

| # | 规则 | 说明 |
|---|------|------|
| R9 | 禁止冗余 Configure 类 | 无自定义 `@Bean` 的 `@Configure` 类应删除 |

### 代码生成包规则

| # | 规则 | 说明 |
|---|------|------|
| R10 | `generated` 包禁止手动修改 | 由 MybatisPlusGenerator 自动生成，手动修改会被覆盖 |
| R10b | 新增 Entity/Mapper 通过生成器 | 运行 `MybatisPlusGenerator.main()` 生成 |

---

## 代码示例

### 分层结构示例

```
org.smm.archetype/
├── controller/          # 控制器层
│   ├── global/          # 全局过滤器、异常处理
│   ├── system/          # 系统配置 Controller
│   └── operationlog/    # 操作日志 Controller
├── facade/              # 门面层（Controller → Service 桥接）
│   ├── system/
│   └── operationlog/
├── service/             # 服务层
│   ├── auth/
│   └── system/
├── repository/          # 仓储层（Service → Mapper 桥接）
│   ├── system/
│   └── user/
├── entity/              # 实体层（纯 POJO）
│   ├── base/
│   └── system/
├── config/              # 配置类
├── shared/              # 共享横切关注点
│   ├── aspect/          # AOP 切面
│   └── util/            # 工具类
└── generated/           # 代码生成（禁止手动修改）
    ├── entity/
    └── mapper/
```

### 命名规范示例

| 类型 | 命名规范 | 正确示例 | 错误示例 |
|------|---------|---------|---------|
| Controller | `{Domain}Controller` | `SystemConfigController` | `SystemConfigCtrl` |
| Facade | `{Domain}Facade` | `SystemConfigFacade` | `SystemConfigManager` |
| Facade 实现 | `{Domain}FacadeImpl` | `SystemConfigFacadeImpl` | `SystemConfigFacadeV1` |
| Service | `{Domain}Service` | `SystemConfigService` | `SystemConfigMgr` |
| Repository | `{Domain}Repository` | `SystemConfigRepository` | `SystemConfigDao` |
| Entity | `{Domain}` | `SystemConfig` | `SystemConfigEntity` |
| 值对象 | `{Concept}` | `ConfigKey`, `DisplayName` | `ConfigKeyVO` |
| DO (生成) | `{Domain}DO` | `SystemConfigDO` | `SystemConfigEntity` |
| Mapper (生成) | `{Domain}Mapper` | `SystemConfigMapper` | `SystemConfigDao` |
| 测试 | `{Target}UTest` / `{Target}ITest` | `SystemConfigFacadeImplUTest` | `TestSystemConfig` |

### 注解使用示例

**正确 — 构造器注入**：
```java
@Configuration
public class LoggingConfigure {

    private final LoggingProperties loggingProperties;

    public LoggingConfigure(LoggingProperties loggingProperties) {
        this.loggingProperties = loggingProperties;
    }

    @Bean
    public LogAspect logAspect(OperationLogWriter writer) {
        return new LogAspect(writer);
    }
}
```

**正确 — Entity 使用 @Getter/@Setter**：
```java
@Getter
@Setter
public class SystemConfig {
    private ConfigKey configKey;
    private DisplayName displayName;
    private ConfigValue configValue;
    // 不使用 @Data
}
```

**正确 — 使用 Properties 类绑定配置**：
```java
@ConfigurationProperties(prefix = "logging")
public class LoggingProperties {
    private SlowQuery slowQuery = new SlowQuery();
    private Sampling sampling = new Sampling();

    @Getter @Setter
    public static class SlowQuery {
        private boolean enabled = false;
        private long thresholdMs = 1000;
    }
}
```

---

## 违规示例

### 违规 1：Controller 直接依赖 Mapper

```java
// ❌ 违规 R1：Controller 直接注入 Mapper
@RestController
public class UserController {
    @Autowired
    private UserMapper userMapper;  // ❌ 禁止
}

// ✅ 正确：Controller 依赖 Facade
@RestController
public class UserController {
    private final UserFacade userFacade;  // ✅ 正确

    public UserController(UserFacade userFacade) {
        this.userFacade = userFacade;
    }
}
```

### 违规 2：Entity 使用 @Data

```java
// ❌ 违规 R7：Entity 使用 @Data
@Data
public class SystemConfig {
    private String configKey;
    private String passwordHash;  // toString 可能暴露密码
}

// ✅ 正确：使用 @Getter/@Setter
@Getter
@Setter
public class SystemConfig {
    private String configKey;
    private String passwordHash;
}
```

### 违规 3：使用 @Value 注入

```java
// ❌ 违规 R8：使用 @Value 字段注入
@Configuration
public class MyConfigure {
    @Value("${my.config.timeout}")
    private int timeout;  // ❌ 禁止
}

// ✅ 正确：使用 Properties 类 + 构造器注入
@ConfigurationProperties(prefix = "my.config")
public class MyProperties {
    private int timeout = 1000;
    // getter/setter
}

@Configuration
@EnableConfigurationProperties(MyProperties.class)
public class MyConfigure {
    private final MyProperties myProperties;

    public MyConfigure(MyProperties myProperties) {
        this.myProperties = myProperties;
    }
}
```

### 违规 4：Entity 依赖 Spring

```java
// ❌ 违规 R4：Entity 依赖 Spring Framework
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component  // ❌ Entity 不能使用 Spring 注解
public class SystemConfig {
    @Autowired  // ❌ Entity 不能注入依赖
    private SomeService someService;
}
```

### 违规 5：手动修改 generated 包

```java
// ❌ 违规 R10：手动修改 generated 包中的文件
package org.smm.archetype.generated.entity;
public class SystemConfigDO {
    // 手动添加字段 — 下次生成时会被覆盖 ❌
    private String customField;
}
```

---

## 检查方法

### ArchUnit 架构守护测试

项目使用 ArchUnit 自动化执行架构规则检查，位于 `support/basic/ArchitectureComplianceUTest.java`：

| 测试方法 | 守护规则 | 规则编号 |
|---------|---------|---------|
| `controllerShouldOnlyDependOnServiceLayer` | Controller 不得依赖 Mapper | R1 |
| `serviceShouldNotDependOnControllerLayer` | Service 不得依赖 Controller | R2 |
| `repositoryShouldNotDependOnServiceOrControllerLayer` | Repository 不得依赖 Service/Controller | R3 |
| `entityShouldNotDependOnSpringFramework` | Entity 不得依赖 Spring | R4 |
| `controllerShouldNotDependOnServiceDirectly` | Controller 不得直接依赖 Service（Login 除外） | R5 |
| `facadeShouldNotDependOnRepository` | Facade 不得依赖 Repository | R6 |

**执行方式**：
```bash
# 运行全部架构守护测试
mvn test -pl app -Dtest="ArchitectureComplianceUTest"

# 运行单个规则
mvn test -pl app -Dtest="ArchitectureComplianceUTest#controllerShouldOnlyDependOnServiceLayer"
```

### NoDataAnnotation 检查

**守护文件**：`support/basic/NoDataAnnotationUTest.java`

**规则**：扫描整个 `org.smm.archetype` 包，检查是否有类使用了 `@Data` 注解。

```java
// 检查逻辑（简化）
ArchRuleDefinition.noClasses()
    .that().resideInAPackage("org.smm.archetype..")
    .should().beAnnotatedWith(Data.class)
    .check(importedClasses);
```

### NoRedundantConfigure 检查

**守护文件**：`support/basic/NoRedundantConfigureUTest.java`

**规则**：检查 `config` 包下的 `@Configure` 类是否有自定义 `@Bean` 方法。无 `@Bean` 的 Configure 类被视为冗余。

```java
// 检查逻辑（简化）
// 1. 扫描 config 包下所有 @Configure 类
// 2. 检查每个类是否有 @Bean 方法
// 3. 无 @Bean → 报错：冗余 Configure 类应删除
```

### NoValueInjection 检查

**守护文件**：`support/basic/NoValueInjectionUTest.java`

**规则**：扫描整个 `org.smm.archetype` 包，检查是否有字段使用了 `@Value` 注解。

```java
// 检查逻辑（简化）
ArchRuleDefinition.noFields()
    .that().areAnnotatedWith(Value.class)
    .should().exist()
    .check(importedClasses);
```

---

## 检查清单

> 以下清单适用于 Code Review 和提交前自检。

### 分层架构

- [ ] Controller 是否只依赖 Facade（不直接调用 Service/Mapper）
- [ ] Service 是否不依赖 Controller
- [ ] Repository 是否不依赖 Service/Controller
- [ ] Entity 是否不依赖 Spring Framework（无 `@Component`/`@Autowired`）
- [ ] Facade 是否不直接依赖 Repository

### 注解规范

- [ ] Entity 是否使用 `@Getter/@Setter` 而非 `@Data`
- [ ] 配置注入是否使用 Properties 类 + 构造器注入（非 `@Value`）
- [ ] Configure 类是否有实际 `@Bean` 方法（非空壳）

### 代码生成

- [ ] `generated` 包下文件是否未被手动修改
- [ ] 新增 Entity/Mapper 是否通过 `MybatisPlusGenerator` 生成

### 命名规范

- [ ] Controller 命名是否为 `{Domain}Controller`
- [ ] Facade 命名是否为 `{Domain}Facade`/`{Domain}FacadeImpl`
- [ ] Service 命名是否为 `{Domain}Service`
- [ ] Repository 命名是否为 `{Domain}Repository`
- [ ] 测试命名是否为 `{Target}UTest`/`{Target}ITest`

### 自动化检查

- [ ] `mvn test` 是否全部通过（含 ArchUnit 守护测试）
- [ ] `NoDataAnnotationUTest` 是否通过
- [ ] `NoRedundantConfigureUTest` 是否通过
- [ ] `NoValueInjectionUTest` 是否通过

---

## 相关文档

| 文档 | 关系 | 说明 |
|------|------|------|
| AOP 切面 | infrastructure/aop-aspects.md | AOP 切面使用规范 |
| 测试指南 | guides/testing-guide.md | 测试命名与结构规范 |
| 配置参考 | guides/configuration-reference.md | Properties 类配置规范 |
| 数据库 Schema | guides/database.md | 代码生成器配置 |

---

## 变更历史

| 版本 | 日期 | 变更内容 |
|------|------|---------|
| 1.0.0 | 2026-04-25 | 初始版本：10 条规则 + ArchUnit 自动化检查 |
