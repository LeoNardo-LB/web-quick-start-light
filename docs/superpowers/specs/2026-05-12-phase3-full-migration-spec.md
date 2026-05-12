# 阶段 3 Spec：全模块迁移 + Spring Modulith

> 关联总纲：`docs/architecture/refactoring-plan.md` → 阶段 3
> 前置依赖：阶段 1（基础设施类型）+ 阶段 2（systemconfig 模块试点迁移）
> 创建日期：2026-05-12
> 状态：待实施

---

## 概览

阶段 3 将阶段 2 的 Spring Modulith 模块化模式推广到 **auth** 和 **operationlog** 两个模块，调整 **shared** 横切关注点的包结构，并引入 Spring Modulith 边界验证和事件系统。

### 核心变更

1. **auth 模块化**：LoginFacade → AuthFacade（重命名），所有文件迁入 `auth/` + `auth/internal/`
2. **operationlog 模块化**：全量文件迁入 `operationlog/` + `operationlog/internal/`，消除 IPage 泄漏
3. **shared 结构调整**：AOP 切面迁入 `shared/internal/aspect/`，添加 `package-info.java`
4. **Spring Modulith 引入**：`@ApplicationModule` 声明 + `ApplicationModules.verify()` + 事件持久化
5. **旧文件清理**：删除 Phase 1/2 标记的废弃文件，清理 generated/ 和旧包目录

### 操作统计

| 操作类型 | 数量 | 说明 |
|---------|------|------|
| 新建文件（源码） | 34+ | auth(13) + operationlog(12) + shared(3) + package-info(4) + 代码生成器改造(2) |
| 新建文件（测试） | 6 | 迁移后测试文件新位置 |
| 删除文件（源码） | 42 | 旧 auth 结构 + 旧 operationlog 结构 + 废弃 entity + generated 旧路径 |
| 删除文件（测试） | 6 | 旧位置测试文件 |
| 修改文件 | 3 | app/pom.xml（Spring Modulith 依赖）、MybatisPlusGenerator（输出路径） |
| 废弃标记 | 6 | shared/aspect/operationlog/ 下 4 个已迁移存根 + 2 个旧 entity base 文件 |

---

## 一、迁移映射表

### 1.1 auth 模块（包迁移 + 重命名 + 新增）

| 旧路径 | 新路径 | 变更类型 |
|--------|--------|---------|
| `controller/auth/LoginController.java` | `auth/internal/LoginController.java` | 包迁移 |
| `service/auth/LoginFacade.java` | `auth/AuthFacade.java` | 包迁移 + **重命名** |
| `service/auth/LoginFacadeImpl.java` | `auth/internal/AuthFacadeImpl.java` | 包迁移 + **重命名** |
| `entity/user/User.java` | `auth/internal/User.java` | 包迁移 |
| `repository/user/UserRepository.java` | `auth/internal/UserRepository.java` | 包迁移 |
| `repository/user/UserRepositoryImpl.java` | `auth/internal/UserRepositoryImpl.java` | 包迁移 + 提取 UserConverter |
| `generated/entity/UserDO.java` | `auth/internal/UserDO.java` | 包迁移（脱离 generated/） |
| `generated/mapper/UserMapper.java` | `auth/internal/UserMapper.java` | 包迁移（脱离 generated/） |
| — | `auth/internal/UserConverter.java` | **新增** |
| — | `auth/internal/LoginRequest.java` | **新增**（从 Controller 内联 record 提取） |
| — | `auth/internal/LoginVO.java` | **新增** |
| — | `auth/package-info.java` | **新增**（@ApplicationModule） |

**接口签名变更**：

| 旧 | 新 |
|----|----|
| `LoginFacade.login(String, String)` | `AuthFacade.login(String, String)` |
| `LoginFacade.logout()` | `AuthFacade.logout()` |

---

### 1.2 operationlog 模块（包迁移 + IPage 消除）

| 旧路径 | 新路径 | 变更类型 |
|--------|--------|---------|
| `controller/operationlog/OperationLogController.java` | `operationlog/internal/OperationLogController.java` | 包迁移 |
| `facade/operationlog/OperationLogFacade.java` | `operationlog/OperationLogFacade.java` | 包迁移 |
| `facade/operationlog/OperationLogFacadeImpl.java` | `operationlog/internal/OperationLogFacadeImpl.java` | 包迁移 + **IPage → PageResult** |
| `facade/operationlog/OperationLogVO.java` | `operationlog/internal/OperationLogVO.java` | 包迁移 |
| `service/operationlog/OperationLogService.java` | `operationlog/internal/OperationLogService.java` | 包迁移 + **IPage → PageResult** |
| `entity/operationlog/OperationLog.java` | `operationlog/internal/OperationLog.java` | 包迁移 |
| `entity/operationlog/OperationLogPageQuery.java` | `operationlog/internal/OperationLogPageQuery.java` | 包迁移 + PageQuery 校验委托 |
| `repository/operationlog/OperationLogRepository.java` | `operationlog/internal/OperationLogRepository.java` | 包迁移 + **IPage → PageResult** |
| `repository/operationlog/OperationLogRepositoryImpl.java` | `operationlog/internal/OperationLogRepositoryImpl.java` | 包迁移 + **IPage → PageResult** |
| `repository/operationlog/OperationLogConverter.java` | `operationlog/internal/OperationLogConverter.java` | 包迁移 + `toEntity()` → `toModel()` |
| `generated/entity/OperationLogDO.java` | `operationlog/internal/OperationLogDO.java` | 包迁移（脱离 generated/） |
| `generated/mapper/OperationLogMapper.java` | `operationlog/internal/OperationLogMapper.java` | 包迁移（脱离 generated/） |
| — | `operationlog/package-info.java` | **新增**（@ApplicationModule） |

**全链路 IPage → PageResult**：

| 层 | 旧 | 新 |
|----|----|----|
| Repository | `IPage<OperationLog>` | `PageResult<OperationLog>` |
| Service | `IPage<OperationLog>` | `PageResult<OperationLog>` |
| FacadeImpl | 手动 new Page + 设值 | `BasePageResult.from(PageResult)` |

---

### 1.3 shared 横切关注点（结构调整 + 包信息）

| 旧路径 | 新路径 | 变更类型 |
|--------|--------|---------|
| `shared/aspect/ratelimit/*.java`（5 文件） | `shared/internal/aspect/ratelimit/*.java` | 包迁移 |
| `shared/aspect/idempotent/*.java`（3 文件） | `shared/internal/aspect/idempotent/*.java` | 包迁移 |
| `shared/aspect/operationlog/LogAspect.java` | `shared/internal/aspect/operationlog/LogAspect.java` | 包迁移 |
| — | `shared/package-info.java` | **新增**（@ApplicationModule(type = OPEN)） |
| `shared/aspect/operationlog/BusinessLog.java`（已迁移存根） | — | **删除**（已在 common/operationlog/） |
| `shared/aspect/operationlog/OperationLogRecord.java`（已迁移存根） | — | **删除**（已在 common/operationlog/） |
| `shared/aspect/operationlog/OperationLogWriter.java`（已迁移存根） | — | **删除**（已在 common/operationlog/） |
| `shared/aspect/operationlog/OperationType.java`（已迁移存根） | — | **删除**（已在 common/operationlog/） |

**shared 模块最终结构**：

```
shared/
├── package-info.java              ← @ApplicationModule(type = OPEN)
├── pagination/                    ← PageQuery, PageResult
├── result/                        ← BaseResult, BasePageResult
├── util/                          ← 工具类（BizContext, IpUtils, KryoSerializer 等）
│   ├── context/
│   ├── dal/
│   └── logging/
└── internal/
    └── aspect/                    ← AOP 切面实现（内部细节）
        ├── ratelimit/             ← RateLimitAspect, BucketFactory, SpelKeyResolver
        ├── idempotent/            ← IdempotentAspect, IdempotentKeyResolver
        └── operationlog/          ← LogAspect
```

---

### 1.4 代码生成器

**现状**：`generated/MybatisPlusGenerator.java` 输出到 `org.smm.archetype.generated` 统一包。
**变更**：模块化后，DO/Mapper 已迁入各模块 internal/ 包。代码生成器需改造为多模块输出或提供各模块独立入口。

| 旧路径 | 操作 | 说明 |
|--------|------|------|
| `generated/MybatisPlusGenerator.java` | **修改** | 新增 `--module` 参数支持，或拆分为模块独立的 Generator main class |

**推荐方案**：改造 `MybatisPlusGenerator`，通过 `--module` 参数切换输出目标：

```bash
# 输出到 systemconfig/internal/
java org.smm.archetype.generated.MybatisPlusGenerator --module systemconfig
# 输出到 auth/internal/
java org.smm.archetype.generated.MybatisPlusGenerator --module auth
# 输出到 operationlog/internal/
java org.smm.archetype.generated.MybatisPlusGenerator --module operationlog
```

---

### 1.5 systemconfig 模块（补充 Spring Modulith）

| 文件 | 操作 | 说明 |
|------|------|------|
| `systemconfig/package-info.java` | **新增** | @ApplicationModule 声明 |
| `systemconfig/internal/MybatisPlusGenerator.java` | 不变 | 已在 Phase 2 迁移至此 |

---

## 二、关键接口签名变更

### 2.1 AuthFacade（LoginFacade 重命名）

```java
// 旧：LoginFacade
package org.smm.archetype.service.auth;
public interface LoginFacade {
    String login(String username, String password);
    void logout();
}

// 新：AuthFacade
package org.smm.archetype.auth;
public interface AuthFacade {
    String login(String username, String password);
    void logout();
}
```

### 2.2 OperationLogRepository（IPage → PageResult）

```java
// 旧签名
IPage<OperationLog> findByPage(OperationLogPageQuery query);

// 新签名
PageResult<OperationLog> findByPage(OperationLogPageQuery query);
```

**影响范围**：RepositoryImpl、Service、FacadeImpl 的 `findByPage` 调用链。

### 2.3 OperationLogService（IPage → PageResult）

```java
// 旧
public IPage<OperationLog> findByPage(OperationLogPageQuery query);

// 新
public PageResult<OperationLog> findByPage(OperationLogPageQuery query);
```

### 2.4 OperationLogConverter（toEntity → toModel）

```java
// 旧方法名
public OperationLog toEntity(OperationLogDO logDO);

// 新方法名（语义更准确：DO → 领域模型）
public OperationLog toModel(OperationLogDO logDO);
```

### 2.5 OperationLogFacadeImpl（消除 IPage 泄漏）

```java
// 旧实现 — 手动 new Page<>() 转换 + 逐字段设值
IPage<OperationLog> entityPage = operationLogService.findByPage(query);
List<OperationLogVO> voList = entityPage.getRecords().stream().map(this::toVO).toList();
BasePageResult<OperationLogVO> result = new BasePageResult<>();
result.setTotal(entityPage.getTotal());
result.setPageNo((int) entityPage.getCurrent());
result.setPageSize((int) entityPage.getSize());
result.setCode(CommonErrorCode.SUCCESS.code());
result.setMessage(CommonErrorCode.SUCCESS.message());
result.setData(voList);
result.setSuccess(true);
result.setTime(Instant.now());
result.setTraceId(Span.current().getSpanContext().getTraceId());
return result;

// 新实现 — 使用 PageResult + BasePageResult.from()
PageResult<OperationLog> pageResult = operationLogService.findByPage(query);
List<OperationLogVO> voList = pageResult.list().stream().map(this::toVO).toList();
return BasePageResult.from(new PageResult<>(voList, pageResult.total(), pageResult.pageNo(), pageResult.pageSize()));
```

### 2.6 UserRepositoryImpl（提取 UserConverter）

```java
// 旧 — 内联 toEntity 方法
private User toEntity(UserDO userDO) {
    User user = new User();
    user.setId(userDO.getId());
    user.setUsername(userDO.getUsername());
    // ...
    return user;
}

// 新 — 委托 UserConverter
private final UserConverter converter;

private User toEntity(UserDO userDO) {
    return converter.toModel(userDO);
}
```

---

## 三、新增类型定义

### 3.1 UserConverter.java

**位置**：`auth/internal/UserConverter.java`

```java
package org.smm.archetype.auth.internal;

import org.springframework.stereotype.Component;

/**
 * 用户 DO → Entity 转换器。
 * 提取自 UserRepositoryImpl 的内联 toEntity() 方法。
 */
@Component
public class UserConverter {

    public User toModel(UserDO userDO) {
        if (userDO == null) return null;
        User user = new User();
        user.setId(userDO.getId());
        user.setUsername(userDO.getUsername());
        user.setPasswordHash(userDO.getPasswordHash());
        user.setNickname(userDO.getNickname());
        user.setStatus(userDO.getStatus());
        user.setCreateTime(userDO.getCreateTime());
        user.setUpdateTime(userDO.getUpdateTime());
        return user;
    }

    public UserDO toDataObject(User user) {
        if (user == null) return null;
        return UserDO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .passwordHash(user.getPasswordHash())
                .nickname(user.getNickname())
                .status(user.getStatus())
                .build();
    }
}
```

### 3.2 LoginRequest.java（record）

**位置**：`auth/internal/LoginRequest.java`

```java
package org.smm.archetype.auth.internal;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求值对象。
 * 提取自 LoginController 的内联 record。
 */
public record LoginRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password
) {}
```

### 3.3 LoginVO.java（record）

**位置**：`auth/internal/LoginVO.java`

```java
package org.smm.archetype.auth.internal;

/**
 * 登录响应值对象。
 */
public record LoginVO(
        String token
) {}
```

---

## 四、OperationLogPageQuery 适配

与 Phase 2 的 SystemConfigPageQuery 类似，迁移后 OperationLogPageQuery 应委托 `shared.pagination.PageQuery` 做基础分页校验：

```java
package org.smm.archetype.operationlog.internal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.smm.archetype.shared.pagination.PageQuery;

/**
 * 操作日志分页查询参数。
 */
public record OperationLogPageQuery(
        @Min(1) int pageNo,
        @Min(1) @Max(100) int pageSize,
        String module,
        String operationType,
        String startTime,
        String endTime
) {
    public OperationLogPageQuery {
        // 使用 PageQuery 做基础分页校验（compact constructor 中不能抛出异常，
        // 此处仅做修正，Validator 的 @Min/@Max 注解会在验证时生效）
        PageQuery base = new PageQuery(pageNo, pageSize);
    }

    /**
     * 兼容无参绑定场景（Spring MVC @ModelAttribute）。
     */
    public OperationLogPageQuery() {
        this(1, 10, null, null, null, null);
    }
}
```

**注意**：当前 OperationLogPageQuery 不使用 Integer（非装箱类型），因此无法通过 compact constructor 做 null→默认值转换。
若需实现与 SystemConfigPageQuery 相同的 null 安全模式，需将 `int` 改为 `Integer`。本阶段不做此项调整，
保留现有 `int` 类型，保持与 Spring MVC 的 `@ModelAttribute` 兼容性。

---

## 五、Spring Modulith 配置

### 5.1 Maven 依赖（app/pom.xml）

```xml
<!-- Spring Modulith — 模块边界验证 -->
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Spring Modulith — 事件持久化（at-least-once 事件投递） -->
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-jdbc</artifactId>
</dependency>
```

Spring Boot 4.x 通过 BOM 自动管理 Modulith 版本，无需指定版本号。
若出现版本冲突，在根 pom.xml 中添加 `<spring-modulith.version>` 属性覆盖。

### 5.2 模块 package-info.java

#### systemconfig/package-info.java

```java
@org.springframework.modulith.ApplicationModule(
        displayName = "System Configuration",
        allowedDependencies = {"shared"}
)
package org.smm.archetype.systemconfig;
```

#### auth/package-info.java

```java
@org.springframework.modulith.ApplicationModule(
        displayName = "Authentication",
        allowedDependencies = {"shared"}
)
package org.smm.archetype.auth;
```

#### operationlog/package-info.java

```java
@org.springframework.modulith.ApplicationModule(
        displayName = "Operation Log",
        allowedDependencies = {"shared"}
)
package org.smm.archetype.operationlog;
```

#### shared/package-info.java

```java
@org.springframework.modulith.ApplicationModule(
        displayName = "Shared Cross-Cutting",
        type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package org.smm.archetype.shared;
```

### 5.3 ApplicationModules.verify() 测试

**位置**：`app/src/test/java/org/smm/archetype/support/basic/ModulithComplianceUTest.java`

```java
package org.smm.archetype.support.basic;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Spring Modulith 模块边界验证。
 * <p>
 * 验证规则：
 * - 无循环依赖
 * - 模块间仅通过 allowedDependencies 访问
 * - internal/ 包不对外暴露
 */
class ModulithComplianceUTest {

    @Test
    void should_verifyModulithStructure() {
        ApplicationModules modules = ApplicationModules.of(org.smm.archetype.App.class);
        modules.verify();
    }

    @Test
    void should_generateModulithDocumentation() {
        ApplicationModules modules = ApplicationModules.of(org.smm.archetype.App.class);
        new Documenter(modules)
                .writeDocumentation()
                .writeIndividualModulesAsPlantUml();
    }
}
```

---

## 六、Spring Bean 冲突处理策略

### 6.1 问题

本阶段涉及大量文件迁移。过渡期内（Task 1-5），新旧包中存在同名 Spring Bean（@Service, @Repository, @Component, @RestController），会导致 Bean 定义冲突。

### 6.2 解决方案

**新文件在 Task 1-5 期间不添加 Spring 注解**。遵循与 Phase 2 完全一致的策略。

#### auth 模块

| 类 | Task 1-5 | Task 6 |
|----|----------|--------|
| AuthFacadeImpl (new) | 无 @Service | 添加 @Service |
| UserRepositoryImpl (new) | 无 @Repository | 添加 @Repository |
| UserConverter (new) | 无 @Component | 添加 @Component |
| LoginController (new) | 无 @RestController | 添加 @RestController |
| UserMapper (new) | 无 @Mapper（引用旧 generated 包） | 迁移到新包 + @Mapper |
| UserDO (new) | 引用旧 generated 包 | 迁移到新包 |

#### operationlog 模块

| 类 | Task 1-5 | Task 6 |
|----|----------|--------|
| OperationLogFacadeImpl (new) | 无 @Service | 添加 @Service |
| OperationLogService (new) | 无 @Service | 添加 @Service |
| OperationLogRepositoryImpl (new) | 无 @Repository | 添加 @Repository |
| OperationLogConverter (new) | 无 @Component | 添加 @Component |
| OperationLogController (new) | 无 @RestController | 添加 @RestController |
| OperationLogMapper (new) | 无 @Mapper（引用旧 generated 包） | 迁移到新包 + @Mapper |
| OperationLogDO (new) | 引用旧 generated 包 | 迁移到新包 |

#### shared 模块（无冲突风险）

AOP 切面在旧位置 (`shared/aspect/`) 已有 `@Aspect` + Spring 注解。新位置 (`shared/internal/aspect/`) 在新文件创建后、旧文件删除前，遵循相同策略。

| 类 | Task 1-5 | Task 6 |
|----|----------|--------|
| LogAspect (new) | 无 @Aspect + 无 Spring 注解 | 添加 @Aspect + Spring 注解 |
| RateLimitAspect (new) | 无 @Aspect + 无 Spring 注解 | 添加 @Aspect + Spring 注解 |
| IdempotentAspect (new) | 无 @Aspect + 无 Spring 注解 | 添加 @Aspect + Spring 注解 |

### 6.3 关键约束

Task 1-5 期间，新代码的 RepositoryImpl 和 Service 仍引用**旧的** `generated.entity.*DO` 和 `generated.mapper.*Mapper`，避免 MyBatis Mapper 重复注册。

### 6.4 验证方式

- Task 1-5：新代码只有 UTest（单元测试，直接构造对象），旧代码的 ITest 不受影响
- Task 6：删除旧代码后添加 Spring 注解，更新所有 ITest 导入路径，全量运行 ITest

---

## 七、ArchUnit 规则影响分析

### 7.1 现有规则与新包结构的关系

| ArchUnit 规则 | 扫描包模式 | 影响 |
|---------------|-----------|------|
| `serviceShouldNotDependOnControllerLayer` | `..service..` | 新 Service 在 `auth/internal`/`operationlog/internal`，**不受此规则约束** |
| `repositoryShouldNotDependOnServiceOrController` | `..repository..` | 新 Repository 在 `auth/internal`/`operationlog/internal`，**不受约束** |
| `entityShouldNotDependOnSpringFramework` | `..entity..` | 新 Entity 在 `auth/internal`/`operationlog/internal`，**不受约束** |
| `controllerShouldNotDependOnServiceDirectly` | `..controller..` | 新 Controller 在 `auth/internal`/`operationlog/internal`，**不受约束** |
| `facadeShouldNotDependOnRepository` | `..facade..` | 新 Facade 在 `auth`/`operationlog`，**不受约束** |
| M-03 facade 不返回 Entity | `..facade..` | 新 Facade 在 `auth`/`operationlog`，**不受约束** |
| M-04 Controller 路径前缀 `/api` | `..controller..` | 新 Controller 在 `auth/internal`/`operationlog/internal`，**不受约束** |
| C-06 facade 下 VO/DTO 必须用 record | `..facade..` | 新 Facade 在 `auth`/`operationlog`，**不受约束** |

### 7.2 结论

上述规则对 auth 和 operationlog 模块完全失效 — 与 Phase 2 的 systemconfig 一致。这是预期行为：模块化架构下，内部结构约束由 `@ApplicationModule` + `ApplicationModules.verify()` 保障。

### 7.3 后续行动

- 本阶段不修改/不新增 ArchUnit 规则
- 模块边界由 Spring Modulith verify 保障（新增 `ModulithComplianceUTest`）
- 全局编码规范规则（C-01~C-07 中的非包路径规则：C-01 LocalDateTime 禁止、C-02 JPA 禁止、C-03 BeanUtils 禁止、C-04 System.out 禁止、C-05 @With 禁止、C-07 @Data 禁止）**仍然有效**，因为它们的 SourceScanner 基于内容扫描而非包路径

---

## 八、测试文件迁移映射

### 8.1 auth 测试

| 旧路径 | 新路径 | 变更说明 |
|--------|--------|---------|
| `service/auth/LoginFacadeITest.java` | `auth/internal/AuthFacadeITest.java` | 包迁移 + 重命名（LoginFacade → AuthFacade） |

### 8.2 operationlog 测试

| 旧路径 | 新路径 | 变更说明 |
|--------|--------|---------|
| `controller/operationlog/OperationLogControllerITest.java` | `operationlog/internal/OperationLogControllerITest.java` | 包迁移 |
| `controller/operationlog/OperationLogPaginationBoundaryITest.java` | `operationlog/internal/OperationLogPaginationBoundaryITest.java` | 包迁移 |
| `facade/operationlog/OperationLogFacadeITest.java` | `operationlog/internal/OperationLogFacadeITest.java` | 包迁移 |
| `repository/operationlog/OperationLogRepositoryITest.java` | `operationlog/internal/OperationLogRepositoryITest.java` | 包迁移 + IPage→PageResult 断言调整 |

### 8.3 shared 测试（暂不迁移）

```
shared/aspect/operationlog/*.java         — 保留（测试 LogAspect，待 Task 7 迁移）
shared/aspect/ratelimit/*.java            — 保留（测试 RateLimitAspect，待 Task 7 迁移）
shared/aspect/idempotent/*.java           — 保留（测试 IdempotentAspect，待 Task 7 迁移）
shared/pagination/*.java                  — 保留
shared/result/*.java                      — 保留
shared/util/**/*.java                     — 保留
```

> **注意**：AOP 切面的 UTest 在 Task 7 之前保留旧位置，Task 7 删除旧切面文件时一并迁移。

### 8.4 跨模块测试（不迁移，仅更新 import）

| 测试文件 | 说明 |
|---------|------|
| `cases/integrationtest/AuthE2EITest.java` | 通过 HTTP 端点测试，不受包结构影响 |
| `cases/integrationtest/LoginControllerITest.java` | **需更新 import**（LoginController 包路径变更） |
| `cases/integrationtest/AuthInterceptorITest.java` | 通过 HTTP 端点测试，不受影响 |
| `cases/integrationtest/BusinessLogITest.java` | 通过 HTTP 端点测试，不受影响 |
| `cases/integrationtest/PaginationTaxonomyITest.java` | 通过 HTTP 端点测试，不受影响 |

---

## 九、废弃与删除清单

### 9.1 旧 entity/base 废弃文件（Phase 1 标记 @Deprecated，本阶段删除）

| 文件 | 操作 |
|------|------|
| `entity/base/BaseRequest.java` | **删除** |
| `entity/base/BasePageRequest.java` | **删除** |
| `entity/base/BaseResult.java`（旧位置） | **删除** |
| `entity/base/BasePageResult.java`（旧位置） | **删除** |

### 9.2 旧位置 deprecated 存根（Phase 1 迁移，本阶段删除）

| 文件 | 操作 |
|------|------|
| `shared/aspect/operationlog/BusinessLog.java`（旧存根） | **删除** |
| `shared/aspect/operationlog/OperationLogRecord.java`（旧存根） | **删除** |
| `shared/aspect/operationlog/OperationLogWriter.java`（旧存根） | **删除** |
| `shared/aspect/operationlog/OperationType.java`（旧存根） | **删除** |

### 9.3 旧模块文件（迁移后删除的目录）

| 目录 | 操作 |
|------|------|
| `controller/auth/` | **递归删除** |
| `controller/operationlog/` | **递归删除** |
| `entity/user/` | **递归删除** |
| `entity/operationlog/` | **递归删除** |
| `repository/user/` | **递归删除** |
| `repository/operationlog/` | **递归删除** |
| `service/auth/` | **递归删除** |
| `service/operationlog/` | **递归删除** |
| `facade/operationlog/` | **递归删除** |

### 9.4 generated/ 清理

| 文件 | 操作 |
|------|------|
| `generated/entity/UserDO.java` | **删除**（已迁至 auth/internal/） |
| `generated/mapper/UserMapper.java` | **删除**（已迁至 auth/internal/） |
| `generated/entity/OperationLogDO.java` | **删除**（已迁至 operationlog/internal/） |
| `generated/mapper/OperationLogMapper.java` | **删除**（已迁至 operationlog/internal/） |
| `generated/MybatisPlusGenerator.java` | **保留并改造**（多模块输出） |

### 9.5 保留的旧文件

| 文件 | 说明 |
|------|------|
| `controller/global/WebExceptionAdvise.java` | 保留在 controller/global/ |
| `controller/global/ContextFillFilter.java` | 保留在 controller/global/ |
| `controller/test/TestController.java` | 保留在 controller/test/ |
| `entity/base/BaseDO.java` | 保留（DO 基类，被各模块 DO 继承） |
| `generated/`（空目录） | 保留空目录（可后续删除） |

---

## 十、验证清单

### 10.1 编译验证

- [ ] `mvn compile -pl app` 编译通过（每个 Task 完成后）
- [ ] 无 duplicate class 错误
- [ ] 无 missing import 错误
- [ ] 无 @Deprecated 引用警告之外的编译警告

### 10.2 单元测试

- [ ] `mvn test -Dtest="*UTest" -pl app` 全部通过
- [ ] UserConverterUTest — toModel + toDataObject + 往返一致性（**新增**）
- [ ] 所有 shared 方面 UTest 通过（IdempotentAspectUTest, RateLimitAspectUTest 等）
- [ ] ModulithComplianceUTest — ApplicationModules.verify() + Documenter（**新增**）

### 10.3 集成测试

- [ ] `mvn test -Dtest="*ITest" -pl app` 全部通过
- [ ] AuthFacadeITest — 登录/注销全链路（**重命名**）
- [ ] LoginControllerITest — HTTP 登录端点（需更新 import）
- [ ] OperationLogRepositoryITest — PageResult 断言（非 IPage）
- [ ] OperationLogFacadeITest — 全链路功能
- [ ] OperationLogControllerITest — HTTP 端点
- [ ] OperationLogPaginationBoundaryITest — 边界值
- [ ] WebExceptionAdviseI18nITest — i18n（跨模块测试）
- [ ] PaginationTaxonomyITest — 分页分类学（跨模块测试）
- [ ] AuthE2EITest — 认证端到端
- [ ] ApplicationStartupITest — 应用启动成功

### 10.4 ArchUnit 规则

- [ ] `mvn test -Dtest="*ComplianceUTest" -pl app` 全部通过
- [ ] SourceScanner 无违规（无 @Data、无 System.out、无 LocalDateTime 等）

### 10.5 代码规范

- [ ] 无 @Data 注解
- [ ] 无 @Value 注入
- [ ] 时间字段使用 Instant
- [ ] Controller 路径以 /api 开头
- [ ] 测试文件以 UTest.java 或 ITest.java 结尾
- [ ] UTest 不使用 @SpringBootTest
- [ ] ITest 不使用 @Mock
- [ ] 新增 VO/DTO 使用 record

---

## 十一、任务分解（执行顺序）

| Task | 范围 | 涉及模块 | 说明 |
|------|------|---------|------|
| **Task 1** | 源码搬迁 | auth | 创建 `auth/AuthFacade` + `auth/internal/` 下所有文件，不加 Spring 注解 |
| **Task 2** | 源码搬迁 | operationlog | 创建 `operationlog/OperationLogFacade` + `operationlog/internal/` 下所有文件，IPage→PageResult，不加 Spring 注解 |
| **Task 3** | 源码搬迁 | shared | 创建 `shared/internal/aspect/`，搬迁 AOP 切面，不加 Spring 注解 |
| **Task 4** | Spring Modulith | 全局 | 添加 modulith 依赖 + 4 个 package-info.java + ModulithComplianceUTest |
| **Task 5** | 测试搬迁 | auth + operationlog | 创建新位置测试文件，保留旧测试可编译 |
| **Task 6** | 激活 | auth + operationlog + shared | 删除旧文件，新文件添加 Spring 注解，更新所有 import |
| **Task 7** | 最终清理 | 全局 | 删除废弃存根、清理 generated/、验证全量编译和测试 |

### 各 Task 编译验证

| Task | 验证命令 | 预期结果 |
|------|---------|---------|
| 1-3 | `mvn compile -pl app` | 通过（旧文件仍有注解，新文件无注解） |
| 4 | `mvn compile -pl app` | 通过（仅增加依赖和 package-info） |
| 5 | `mvn compile -pl app` | 通过（测试文件新增，不影响编译） |
| 6 | `mvn compile -pl app` | **可能编译失败** — 需要修复所有旧 import 引用 |
| 7 | `mvn test -pl app` | 全部通过 |

---

## 十二、风险点

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Spring Bean 冲突（过渡期） | 应用启动失败 | Task 1-5 新代码不加 Spring 注解 |
| MyBatis Mapper 重复注册 | SQL 执行异常 | Task 1-5 引用旧 generated 包的 Mapper |
| AuthFacade 重命名导致引用断裂 | 编译错误 | Task 6 统一更新所有 import（含测试和跨模块引用） |
| OperationLog IPage→PageResult 全链路变更 | 测试断言失败 | 同步更新 OperationLogRepositoryITest 断言 |
| Spring Modulith 版本兼容性 | 类加载异常 | Spring Boot 4.x BOM 应管理 modulith 版本；若不存在则需手动添加 `<spring-modulith.version>` |
| event_publication 表自动管理 | DDL 自动执行 | spring-modulith-starter-jdbc 自动管理，无需手动建表 |
| shared 标注 OPEN 类型 | 模块边界防护减弱 | 已确认 shared 是工具性模块，OPEN 是合理选择 |
| ArchUnit 旧包规则失效 | 架构约束盲区 | 由 Modulith verify 补位 |
| 旧测试引用旧 import 未及时更新 | ITest 编译失败 | Task 5 先创建新测试，Task 6 统一删除旧测试 |
| OperationLogPageQuery int vs Integer 类型 | 默认值处理差异 | 保持现有 int 类型，不做 null→默认值改造 |

---

## 十三、执行后验证脚本

```bash
# 1. 全量编译
mvn clean compile -pl app

# 2. 单元测试（含 Modulith 验证）
mvn test -Dtest="*UTest" -pl app

# 3. 集成测试
mvn test -Dtest="*ITest" -pl app

# 4. 单独运行 Modulith 验证
mvn test -Dtest="ModulithComplianceUTest" -pl app

# 5. ArchUnit 验证
mvn test -Dtest="*ComplianceUTest" -pl app

# 6. 应用启动验证
mvn spring-boot:run -pl app

# 7. 全量测试 + 覆盖率
mvn clean verify -pl app
```
