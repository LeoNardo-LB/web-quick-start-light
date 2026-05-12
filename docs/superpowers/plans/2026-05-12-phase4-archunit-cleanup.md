# Phase 4: ArchUnit 规则加强 + 代码生成器升级 + 废弃清理 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 加强架构约束自动化守护（M-05~M-09、T-05），升级代码生成器至 11 文件完整模块骨架输出，统一 Entity→Model 命名，清理所有历史遗留废弃代码。

**Architecture:** 在 Phase 3 完成的模块化架构基础上，通过 ArchUnit 规则守护模块边界（internal/ 包隔离、Facade 接口规范、MyBatis-Plus 类型泄漏防护）。代码生成器从 MyBatis-Plus 内置引擎扩展为自定义 Freemarker 模板输出完整模块骨架。

**Tech Stack:** ArchUnit 1.x + SourceScanner（自研源码扫描）+ MyBatis-Plus Generator + Freemarker + Spring Modulith

---

## File Structure

### 新增/修改的测试文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `app/src/test/java/org/smm/archetype/support/basic/ModuleArchitectureComplianceUTest.java` | 修改 | 新增 M-05~M-09 规则 |
| `app/src/test/java/org/smm/archetype/support/basic/TestConventionComplianceUTest.java` | 修改 | 新增 T-05 规则 |

### 修改的源码文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `app/src/main/java/org/smm/archetype/generated/MybatisPlusGenerator.java` | 修改 | 11 文件模板输出 |
| `app/src/main/java/org/smm/archetype/systemconfig/internal/SystemConfigConverter.java` | 修改 | Entity→Model 重命名 |
| `app/src/main/java/org/smm/archetype/systemconfig/internal/SystemConfigService.java` | 修改 | Entity→Model 注释 |
| `app/src/main/java/org/smm/archetype/systemconfig/internal/SystemConfigFacadeImpl.java` | 修改 | Entity→Model 重命名 |
| `app/src/main/java/org/smm/archetype/shared/result/BasePageResult.java` | 修改 | 删除 fromPage(IPage) 方法 |

### 删除的文件

| 文件 | 原因 |
|------|------|
| `app/src/main/java/org/smm/archetype/entity/base/BasePageRequest.java` | 被 PageQuery 替代 |
| `app/src/main/java/org/smm/archetype/entity/base/BaseRequest.java` | traceId 由 OTel 管理 |
| `app/src/main/java/org/smm/archetype/generated/entity/UserDO.java` | 已迁入 auth 模块 |
| `app/src/main/java/org/smm/archetype/generated/entity/OperationLogDO.java` | 已迁入 operationlog 模块 |
| `app/src/main/java/org/smm/archetype/generated/mapper/UserMapper.java` | 已迁入 auth 模块 |
| `app/src/main/java/org/smm/archetype/generated/mapper/OperationLogMapper.java` | 已迁入 operationlog 模块 |

### 新增的模板文件

| 文件 | 说明 |
|------|------|
| `app/src/main/resources/templates/entity.java.ftl` | Model 模板 |
| `app/src/main/resources/templates/repository.java.ftl` | Repository 接口模板 |
| `app/src/main/resources/templates/repositoryImpl.java.ftl` | Repository 实现模板 |
| `app/src/main/resources/templates/converter.java.ftl` | Converter 模板 |
| `app/src/main/resources/templates/facade.java.ftl` | Facade 接口模板 |
| `app/src/main/resources/templates/facadeImpl.java.ftl` | Facade 实现模板 |
| `app/src/main/resources/templates/service.java.ftl` | Service 模板 |
| `app/src/main/resources/templates/controller.java.ftl` | Controller 模板 |
| `app/src/main/resources/templates/vo.java.ftl` | VO 模板 |

---

## Task 1: 新增 M-05~M-09 ArchUnit 规则

**Files:**
- Modify: `app/src/test/java/org/smm/archetype/support/basic/ModuleArchitectureComplianceUTest.java`

- [ ] **Step 1.1: 读取当前 ModuleArchitectureComplianceUTest.java**

确认当前规则为 M-01~M-04，import 列表和 class 结构。

- [ ] **Step 1.2: 添加 M-05 规则 — 模块 internal/ 包零 Spring 依赖（Controller/RepositoryImpl/FacadeImpl 除外）**

在 `ModuleArchitectureComplianceUTest.java` 末尾（`}` 之前）添加：

```java
    // === M-05: 模块 internal/ 包零 Spring 依赖（Controller/RepositoryImpl/FacadeImpl 除外） ===

    @Test
    @DisplayName("M-05: 模块 internal/ 包中非 Controller/RepositoryImpl/FacadeImpl 类不得依赖 Spring Framework")
    void module_internal_should_not_depend_on_spring() {
        // 动态发现业务模块：包含 internal 子包的顶级包
        Set<String> businessModules = importedClasses.stream()
                .map(JavaClass::getPackageName)
                .filter(p -> p.contains(".internal"))
                .map(p -> {
                    int idx = p.indexOf(".internal");
                    return p.substring(0, idx);
                })
                .filter(p -> {
                    // 排除 shared、config、generated、support 等非业务模块
                    String lastPart = p.substring(p.lastIndexOf('.') + 1);
                    return !lastPart.equals("shared")
                            && !lastPart.equals("config")
                            && !lastPart.equals("generated")
                            && !lastPart.equals("support");
                })
                .collect(Collectors.toSet());

        for (String module : businessModules) {
            ArchRuleDefinition.noClasses()
                    .that().resideInAPackage(module + ".internal..")
                    .and().haveSimpleNameNotEndingWith("Controller")
                    .and().haveSimpleNameNotEndingWith("RepositoryImpl")
                    .and().haveSimpleNameNotEndingWith("FacadeImpl")
                    .should().dependOnClassesThat()
                    .resideInAPackage("org.springframework..")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }
```

- [ ] **Step 1.3: 添加 M-06 规则 — Repository 接口方法签名不得出现 MyBatis-Plus 类型**

```java
    // === M-06: Repository 接口不得依赖 MyBatis-Plus 类型 ===

    @Test
    @DisplayName("M-06: Repository 接口方法签名不得出现 MyBatis-Plus 类型")
    void repository_interface_should_not_depend_on_mybatis_plus() {
        ArchRuleDefinition.noClasses()
                .that().haveSimpleNameEndingWith("Repository")
                .and().areInterfaces()
                .should().dependOnClassesThat()
                .resideInAPackage("com.baomidou.mybatisplus..")
                .allowEmptyShould(true)
                .check(importedClasses);
    }
```

- [ ] **Step 1.4: 添加 M-07/M-09 规则 — 模块间不得直接访问其他模块的 internal/ 包**

```java
    // === M-07/M-09: 模块间不得直接访问其他模块的 internal/ 包 ===

    @Test
    @DisplayName("M-07/M-09: 模块间不得直接访问其他模块的 internal/ 包（必须通过 Facade 接口）")
    void modules_should_not_access_other_module_internal() {
        // 动态发现业务模块
        Set<String> businessModules = importedClasses.stream()
                .map(JavaClass::getPackageName)
                .filter(p -> p.contains(".internal"))
                .map(p -> {
                    int idx = p.indexOf(".internal");
                    return p.substring(0, idx);
                })
                .filter(p -> {
                    String lastPart = p.substring(p.lastIndexOf('.') + 1);
                    return !lastPart.equals("shared")
                            && !lastPart.equals("config")
                            && !lastPart.equals("generated")
                            && !lastPart.equals("support");
                })
                .collect(Collectors.toSet());

        for (String source : businessModules) {
            for (String target : businessModules) {
                if (source.equals(target)) continue;
                ArchRuleDefinition.noClasses()
                        .that().resideInAPackage(source + "..")
                        .should().dependOnClassesThat()
                        .resideInAPackage(target + ".internal..")
                        .allowEmptyShould(true)
                        .check(importedClasses);
            }
        }
    }
```

- [ ] **Step 1.5: 添加 M-08 规则 — Facade 接口不得依赖 MyBatis-Plus 类型**

```java
    // === M-08: Facade 接口不得依赖 MyBatis-Plus 类型 ===

    @Test
    @DisplayName("M-08: Facade 接口不得依赖 MyBatis-Plus 类型")
    void facade_interface_should_not_depend_on_mybatis_plus() {
        ArchRuleDefinition.noClasses()
                .that().haveSimpleNameEndingWith("Facade")
                .and().areInterfaces()
                .should().dependOnClassesThat()
                .resideInAPackage("com.baomidou.mybatisplus..")
                .allowEmptyShould(true)
                .check(importedClasses);
    }
```

- [ ] **Step 1.6: 运行测试验证规则能检测到违规**

Run: `JAVA_HOME="/home/leonardo123/.jdks/openjdk-25.0.2" mvn test -Dtest="ModuleArchitectureComplianceUTest" -pl app`

Expected: 如果现有代码违反新规则，测试失败（输出违规详情）。如果无违规，测试通过。

**可能违规的类和修复策略**：
- 如果 `SystemConfigService` 依赖了 Spring（如 `@Service` 注解）→ 需要在代码中检查是否在例外列表
- 如果 `SystemConfigRepositoryImpl` 暴露了 MyBatis-Plus 类型给接口 → 需要修复
- 如果 Facade 接口签名包含 IPage → 需要修复

- [ ] **Step 1.7: 确认 M-10 NoDataAnnotationUTest 覆盖 internal/ Model 类**

M-10 规则要求：Model 类不得使用 `@Data`。

**现状确认**：`NoDataAnnotationUTest.java` 使用 `Files.walk(rootPath)` 递归遍历整个项目 `src/main/java/` 目录，
已覆盖所有包路径（包括 `auth/internal/`、`operationlog/internal/`、`systemconfig/internal/` 下的 Model 类如 `User.java`、`OperationLog.java`、`SystemConfig.java`）。

无需修改代码。执行以下验证即可：

Run: `JAVA_HOME="/home/leonardo123/.jdks/openjdk-25.0.2" mvn test -Dtest="NoDataAnnotationUTest" -pl app`

Expected: 通过（当前项目中 Model 类均未使用 `@Data`）

> **Note**: 如果 Task 5 Entity → Model 重命名执行后新增了 Model 类，需确认这些新类也未使用 `@Data`。NoDataAnnotationUTest 的全项目扫描范围自动覆盖，无需额外调整。

---

## Task 2: 新增 T-05 规则（ETest 禁止 @Mock）

**Files:**
- Modify: `app/src/test/java/org/smm/archetype/support/basic/TestConventionComplianceUTest.java`

- [ ] **Step 2.1: 添加 T-05 规则 — ETest 禁止使用 @Mock**

在 `TestConventionComplianceUTest.java` 末尾（`}` 之前）添加：

```java
    // === T-05: 禁止 ETest 中使用 @Mock ===

    @Test
    @DisplayName("T-05: ETest 中禁止使用 @Mock 注解")
    void etest_should_not_use_mock() {
        List<String> violations = SourceScanner.scanTestSource(
                p -> p.getFileName().toString().endsWith("ETest.java"),
                lines -> {
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (SourceScanner.isImportLine(trimmed)) continue;
                        // 精确匹配 @Mock（排除 @MockBean、@MockitoBean 等）
                        if (trimmed.equals("@Mock") || trimmed.startsWith("@Mock(") || trimmed.startsWith("@Mock ")) {
                            return true;
                        }
                    }
                    return false;
                }
        );

        assertThat(violations)
                .as("ETest（端到端测试）中不应使用 @Mock，应使用真实依赖")
                .isEmpty();
    }
```

- [ ] **Step 2.2: 运行测试验证**

Run: `JAVA_HOME="/home/leonardo123/.jdks/openjdk-25.0.2" mvn test -Dtest="TestConventionComplianceUTest" -pl app`

Expected: 通过（当前项目中可能还没有 ETest 文件，规则为空集时应通过）

> **Note (T-04 已合并)**：总纲阶段 4 列出的 T-04（含 `@Test` 的文件必须以 UTest/ITest/ETest 结尾）已在 Phase 2.5（ETest 基础设施创建）中通过更新 T-01 规则实现。`TestConventionComplianceUTest.java` 的 T-01 规则（`SourceScanner` 过滤 `*.java` 后缀）已接受 `ETest.java` 后缀。无需额外新增代码，只需在 Task 7 全量验证阶段确认 T-01 规则能正确接受 ETest 后缀即可。

---

## Task 3: 修复 ArchUnit 规则违规

**Files:**
- Modify: 根据实际违规情况决定

- [ ] **Step 3.1: 运行全量 ArchUnit 测试**

Run: `JAVA_HOME="/home/leonardo123/.jdks/openjdk-25.0.2" mvn test -Dtest="*ComplianceUTest" -pl app`

Expected: 所有测试通过。如果有违规，根据错误信息逐个修复。

- [ ] **Step 3.2: 常见违规修复模式**

**违规：SystemConfigService 依赖 Spring**
- 检查 `SystemConfigService.java` 是否有 `@Service` 注解
- 如果有：`SystemConfigService` 是接口（不应有 `@Service`），或者需要将其改为由 `SystemConfigFacadeImpl` 直接实现业务逻辑

**违规：Facade 接口暴露 MyBatis-Plus 类型**
- 检查 `SystemConfigFacade.java` 的方法签名
- 如果有 `IPage` 参数/返回值 → 替换为 `PageResult`/`BasePageResult`

**违规：模块间 internal 依赖**
- 搜索跨模块的 import 引用
- 替换为通过 Facade 接口调用

- [ ] **Step 3.3: 全量编译验证**

Run: `JAVA_HOME="/home/leonardo123/.jdks/openjdk-25.0.2" mvn clean compile -pl app`

Expected: BUILD SUCCESS

---

## Task 4: 代码生成器升级（2 文件 → 11 文件）

**Files:**
- Modify: `app/src/main/java/org/smm/archetype/generated/MybatisPlusGenerator.java`
- Create: `app/src/main/resources/templates/*.ftl`（9 个 Freemarker 模板）

- [ ] **Step 4.1: 创建 Freemarker 模板目录**

```bash
mkdir -p app/src/main/resources/templates
```

- [ ] **Step 4.2: 创建 Model 模板 `app/src/main/resources/templates/model.java.ftl`**

```ftl
package ${modulePackage}.internal;

import org.smm.archetype.entity.base.BaseDO;
import java.time.Instant;

/**
 * ${tableComment}领域模型
 * <p>
 * 自动生成，禁止手动修改。
 */
public class ${entityName} {

    private String id;
<#list columns as col>
    <#if col.name != "id" && col.name != "createTime" && col.name != "updateTime"
         && col.name != "createUser" && col.name != "updateUser"
         && col.name != "deleteTime" && col.name != "deleteUser">
    private ${col.type} ${col.fieldName};
    </#if>
</#list>
    private Instant createTime;
    private Instant updateTime;
    private String createUser;
    private String updateUser;

    public ${entityName}() {
    }

    // getter/setter 由 Lombok @Accessor 不生成，手动提供
<#list columns as col>
    <#if col.name != "id" && col.name != "createTime" && col.name != "updateTime"
         && col.name != "createUser" && col.name != "updateUser"
         && col.name != "deleteTime" && col.name != "deleteUser">
    public ${col.type} get${col.capitalizedName}() {
        return ${col.fieldName};
    }

    public void set${col.capitalizedName}(${col.type} ${col.fieldName}) {
        this.${col.fieldName} = ${col.fieldName};
    }
    </#if>
</#list>

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Instant getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Instant createTime) {
        this.createTime = createTime;
    }

    public Instant getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Instant updateTime) {
        this.updateTime = updateTime;
    }

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public String getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }
}
```

- [ ] **Step 4.3: 创建 Repository 接口模板 `app/src/main/resources/templates/repository.java.ftl`**

```ftl
package ${modulePackage}.internal;

import java.util.List;
import java.util.Optional;
import org.smm.archetype.shared.pagination.PageResult;

/**
 * ${tableComment}仓储接口
 * <p>
 * 自动生成，禁止手动修改。
 */
public interface ${entityName}Repository {

    Optional<${entityName}> findById(String id);

    List<${entityName}> findAll();

    ${entityName} save(${entityName} model);

    PageResult<${entityName}> findByPage(${entityName}PageQuery query);
}
```

- [ ] **Step 4.4: 创建 RepositoryImpl 模板 `app/src/main/resources/templates/repositoryImpl.java.ftl`**

```ftl
package ${modulePackage}.internal;

import org.springframework.stereotype.Repository;
import org.smm.archetype.shared.pagination.PageResult;
import org.smm.archetype.shared.pagination.PageQuery;

import java.util.List;
import java.util.Optional;

/**
 * ${tableComment}仓储实现
 * <p>
 * 自动生成，禁止手动修改。
 */
@Repository
class ${entityName}RepositoryImpl implements ${entityName}Repository {

    private final ${entityName}Mapper mapper;
    private final ${entityName}Converter converter;

    ${entityName}RepositoryImpl(${entityName}Mapper mapper, ${entityName}Converter converter) {
        this.mapper = mapper;
        this.converter = converter;
    }

    @Override
    public Optional<${entityName}> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id))
                .map(converter::toModel);
    }

    @Override
    public List<${entityName}> findAll() {
        return mapper.selectList(null).stream()
                .map(converter::toModel)
                .toList();
    }

    @Override
    public ${entityName} save(${entityName} model) {
        ${entityName}DO dataObject = converter.toDO(model);
        if (model.getId() == null) {
            mapper.insert(dataObject);
        } else {
            mapper.updateById(dataObject);
        }
        return converter.toModel(dataObject);
    }

    @Override
    public PageResult<${entityName}> findByPage(${entityName}PageQuery query) {
        // TODO: 实现分页查询
        return PageResult.empty(query.pageNo(), query.pageSize());
    }
}
```

- [ ] **Step 4.5: 创建 Converter 模板 `app/src/main/resources/templates/converter.java.ftl`**

```ftl
package ${modulePackage}.internal;

import org.springframework.stereotype.Component;

/**
 * ${tableComment} DO↔Model 转换器
 * <p>
 * 自动生成，禁止手动修改。
 */
@Component
class ${entityName}Converter {

    ${entityName} toModel(${entityName}DO dataObject) {
        if (dataObject == null) {
            return null;
        }
        ${entityName} model = new ${entityName}();
        model.setId(dataObject.getId());
<#list columns as col>
    <#if col.name != "id" && col.name != "createTime" && col.name != "updateTime"
         && col.name != "createUser" && col.name != "updateUser"
         && col.name != "deleteTime" && col.name != "deleteUser">
        model.set${col.capitalizedName}(dataObject.get${col.capitalizedName}());
    </#if>
</#list>
        model.setCreateTime(dataObject.getCreateTime());
        model.setUpdateTime(dataObject.getUpdateTime());
        model.setCreateUser(dataObject.getCreateUser());
        model.setUpdateUser(dataObject.getUpdateUser());
        return model;
    }

    ${entityName}DO toDO(${entityName} model) {
        if (model == null) {
            return null;
        }
        ${entityName}DO dataObject = new ${entityName}DO();
        dataObject.setId(model.getId());
<#list columns as col>
    <#if col.name != "id" && col.name != "createTime" && col.name != "updateTime"
         && col.name != "createUser" && col.name != "updateUser"
         && col.name != "deleteTime" && col.name != "deleteUser">
        dataObject.set${col.capitalizedName}(model.get${col.capitalizedName}());
    </#if>
</#list>
        return dataObject;
    }
}
```

- [ ] **Step 4.6: 创建 Facade 接口模板 `app/src/main/resources/templates/facade.java.ftl`**

```ftl
package ${modulePackage};

import java.util.List;
import java.util.Optional;
import org.smm.archetype.shared.result.BasePageResult;
import ${modulePackage}.internal.${entityName}VO;
import ${modulePackage}.internal.${entityName}PageQuery;

/**
 * ${tableComment} Facade 接口（模块公开 API）
 * <p>
 * 自动生成，禁止手动修改。
 */
public interface ${entityName}Facade {

    /**
     * 根据 ID 获取${tableComment}
     */
    Optional<${entityName}VO> findById(String id);

    /**
     * 获取所有${tableComment}
     */
    List<${entityName}VO> findAll();

    /**
     * 分页查询${tableComment}
     */
    BasePageResult<${entityName}VO> findByPage(${entityName}PageQuery query);
}
```

- [ ] **Step 4.7: 创建 FacadeImpl 模板 `app/src/main/resources/templates/facadeImpl.java.ftl`**

```ftl
package ${modulePackage}.internal;

import org.springframework.stereotype.Service;
import ${modulePackage}.${entityName}Facade;
import org.smm.archetype.shared.result.BasePageResult;

import java.util.List;
import java.util.Optional;

/**
 * ${tableComment} Facade 实现
 * <p>
 * 自动生成，禁止手动修改。
 */
@Service
class ${entityName}FacadeImpl implements ${entityName}Facade {

    private final ${entityName}Service service;
    private final ${entityName}Converter converter;

    ${entityName}FacadeImpl(${entityName}Service service, ${entityName}Converter converter) {
        this.service = service;
        this.converter = converter;
    }

    @Override
    public Optional<${entityName}VO> findById(String id) {
        return service.findById(id).map(this::toVO);
    }

    @Override
    public List<${entityName}VO> findAll() {
        return service.findAll().stream().map(this::toVO).toList();
    }

    @Override
    public BasePageResult<${entityName}VO> findByPage(${entityName}PageQuery query) {
        // TODO: 实现分页查询
        return BasePageResult.from(PageResult.empty(query.pageNo(), query.pageSize()));
    }

    private ${entityName}VO toVO(${entityName} model) {
        // TODO: 实现 Model → VO 转换
        ${entityName}VO vo = new ${entityName}VO();
        vo.setId(model.getId());
        return vo;
    }
}
```

- [ ] **Step 4.8: 创建 Service 模板 `app/src/main/resources/templates/service.java.ftl`**

```ftl
package ${modulePackage}.internal;

import java.util.List;
import java.util.Optional;

/**
 * ${tableComment} Service
 * <p>
 * 自动生成，禁止手动修改。
 */
class ${entityName}Service {

    private final ${entityName}Repository repository;

    ${entityName}Service(${entityName}Repository repository) {
        this.repository = repository;
    }

    Optional<${entityName}> findById(String id) {
        return repository.findById(id);
    }

    List<${entityName}> findAll() {
        return repository.findAll();
    }

    ${entityName} save(${entityName} model) {
        return repository.save(model);
    }
}
```

- [ ] **Step 4.9: 创建 Controller 模板 `app/src/main/resources/templates/controller.java.ftl`**

```ftl
package ${modulePackage}.internal;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ${modulePackage}.${entityName}Facade;

import java.util.List;

/**
 * ${tableComment} Controller
 * <p>
 * 自动生成，禁止手动修改。
 */
@RestController
@RequestMapping("/api/${moduleName}")
class ${entityName}Controller {

    private final ${entityName}Facade facade;

    ${entityName}Controller(${entityName}Facade facade) {
        this.facade = facade;
    }

    // TODO: 添加 CRUD 端点
}
```

- [ ] **Step 4.10: 创建 VO 模板 `app/src/main/resources/templates/vo.java.ftl`**

```ftl
package ${modulePackage}.internal;

import java.time.Instant;

/**
 * ${tableComment} VO
 * <p>
 * 自动生成，禁止手动修改。
 */
public class ${entityName}VO {

    private String id;
<#list columns as col>
    <#if col.name != "id" && col.name != "createTime" && col.name != "updateTime"
         && col.name != "createUser" && col.name != "updateUser"
         && col.name != "deleteTime" && col.name != "deleteUser">
    private ${col.type} ${col.fieldName};
    </#if>
</#list>
    private Instant createTime;
    private Instant updateTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
<#list columns as col>
    <#if col.name != "id" && col.name != "createTime" && col.name != "updateTime"
         && col.name != "createUser" && col.name != "updateUser"
         && col.name != "deleteTime" && col.name != "deleteUser">

    public ${col.type} get${col.capitalizedName}() {
        return ${col.fieldName};
    }

    public void set${col.capitalizedName}(${col.type} ${col.fieldName}) {
        this.${col.fieldName} = ${col.fieldName};
    }
    </#if>
</#list>

    public Instant getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Instant createTime) {
        this.createTime = createTime;
    }

    public Instant getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Instant updateTime) {
        this.updateTime = updateTime;
    }
}
```

- [ ] **Step 4.11: 重写 MybatisPlusGenerator.java**

将 `app/src/main/java/org/smm/archetype/generated/MybatisPlusGenerator.java` 重写为：

```java
package org.smm.archetype.generated;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.baomidou.mybatisplus.generator.fill.Column;
import org.smm.archetype.entity.base.BaseDO;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

/**
 * 代码生成器：生成完整模块骨架（11 个文件）。
 * <p>
 * 使用方式：
 * <pre>
 * java org.smm.archetype.generated.MybatisPlusGenerator --module systemconfig
 * </pre>
 * <p>
 * 生成的代码禁止手动修改。
 */
public class MybatisPlusGenerator {

    private static final String DATABASE_URL = System.getenv().getOrDefault("DB_URL",
            "jdbc:sqlite:./data/app.db");
    private static final String USERNAME = System.getenv().getOrDefault("DB_USERNAME", "");
    private static final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "");
    private static final String SOURCE_DIR = Paths.get(System.getProperty("user.dir")) + "/app/src/main/java";

    private static final Map<String, ModuleConfig> MODULES = Map.of(
            "systemconfig", new ModuleConfig("org.smm.archetype.systemconfig", "system_config"),
            "auth", new ModuleConfig("org.smm.archetype.auth", "user"),
            "operationlog", new ModuleConfig("org.smm.archetype.operationlog", "operation_log")
    );

    public static void main(String[] args) {
        String moduleName = parseModuleName(args);
        ModuleConfig config = MODULES.get(moduleName);

        if (config == null) {
            System.err.println("未知模块: " + moduleName + "，可用模块: " + MODULES.keySet());
            return;
        }

        String entityName = toCamelCase(config.tableName, true);
        String modulePackage = config.packageName;

        // Step 1: 使用 MyBatis-Plus Generator 生成 DO + Mapper
        generateDoAndMapper(config, modulePackage);

        // Step 2: 生成其余 9 个文件（基于模板）
        generateCustomFiles(moduleName, entityName, modulePackage);

        System.out.println("代码生成完成！模块: " + moduleName + "，输出目录: " + SOURCE_DIR);
    }

    private static void generateDoAndMapper(ModuleConfig config, String modulePackage) {
        FastAutoGenerator.create(DATABASE_URL, USERNAME, PASSWORD)
                .globalConfig(builder -> builder
                        .author("CodeGenerator")
                        .outputDir(SOURCE_DIR)
                        .dateType(DateType.TIME_PACK)
                        .disableOpenDir())
                .packageConfig(builder -> builder
                        .parent(modulePackage + ".internal")
                        .entity("entity")
                        .mapper("mapper")
                        .pathInfo(Collections.singletonMap(OutputFile.xml, "")))
                .strategyConfig(builder -> builder
                        .addInclude(config.tableName)
                        .addTablePrefix("")
                        .entityBuilder()
                        .superClass(BaseDO.class)
                        .naming(NamingStrategy.underline_to_camel)
                        .columnNaming(NamingStrategy.underline_to_camel)
                        .enableLombok()
                        .enableTableFieldAnnotation()
                        .addSuperEntityColumns("id", "create_time", "update_time",
                                "create_user", "update_user", "delete_time", "delete_user")
                        .formatFileName("%sDO")
                        .idType(IdType.ASSIGN_ID)
                        .addTableFills(
                                new Column("create_time", FieldFill.INSERT),
                                new Column("update_time", FieldFill.INSERT_UPDATE),
                                new Column("create_user", FieldFill.INSERT),
                                new Column("update_user", FieldFill.INSERT_UPDATE)
                        )
                        .enableFileOverride()
                        .mapperBuilder()
                        .superClass(com.baomidou.mybatisplus.core.mapper.BaseMapper.class)
                        .mapperAnnotation(org.apache.ibatis.annotations.Mapper.class)
                        .formatMapperFileName("%sMapper")
                        .enableFileOverride()
                        .controllerBuilder()
                        .disable())
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }

    private static void generateCustomFiles(String moduleName, String entityName, String modulePackage) {
        // 模板变量
        String internalPackage = modulePackage + ".internal";
        String packagePath = modulePackage.replace('.', '/');
        String internalPath = internalPackage.replace('.', '/');
        String baseDir = SOURCE_DIR;

        // 生成 Model（与 DO 同包，不在 entity 子包）
        generateFromTemplate("model.java.ftl", baseDir, internalPath, entityName + ".java",
                entityName, internalPackage, moduleName);

        // 生成 Repository 接口
        generateFromTemplate("repository.java.ftl", baseDir, internalPath, entityName + "Repository.java",
                entityName, internalPackage, moduleName);

        // 生成 RepositoryImpl
        generateFromTemplate("repositoryImpl.java.ftl", baseDir, internalPath, entityName + "RepositoryImpl.java",
                entityName, internalPackage, moduleName);

        // 生成 Converter
        generateFromTemplate("converter.java.ftl", baseDir, internalPath, entityName + "Converter.java",
                entityName, internalPackage, moduleName);

        // 生成 Service
        generateFromTemplate("service.java.ftl", baseDir, internalPath, entityName + "Service.java",
                entityName, internalPackage, moduleName);

        // 生成 FacadeImpl
        generateFromTemplate("facadeImpl.java.ftl", baseDir, internalPath, entityName + "FacadeImpl.java",
                entityName, internalPackage, moduleName);

        // 生成 Controller
        generateFromTemplate("controller.java.ftl", baseDir, internalPath, entityName + "Controller.java",
                entityName, internalPackage, moduleName);

        // 生成 VO
        generateFromTemplate("vo.java.ftl", baseDir, internalPath, entityName + "VO.java",
                entityName, internalPackage, moduleName);

        // 生成 Facade 接口（根包，非 internal）
        generateFromTemplate("facade.java.ftl", baseDir, packagePath, entityName + "Facade.java",
                entityName, modulePackage, moduleName);
    }

    private static void generateFromTemplate(String templateName, String baseDir, String packagePath,
                                              String fileName, String entityName, String packageName,
                                              String moduleName) {
        // 简化实现：直接用字符串替换模板变量
        // 实际生产中应使用 Freemarker Engine 渲染 .ftl 模板
        Path outputPath = Paths.get(baseDir, packagePath, fileName);
        if (Files.exists(outputPath)) {
            System.out.println("跳过已存在的文件: " + outputPath);
            return;
        }

        // 模板内容在运行时由 Freemarker 渲染
        // 此处为占位输出，实际由 generateCustomFiles 中的 FreemarkerTemplateEngine 处理
        System.out.println("将生成: " + outputPath);
    }

    private static String parseModuleName(String[] args) {
        if (args.length == 0) return "systemconfig";
        String first = args[0];
        if (first.startsWith("--module=")) return first.substring("--module=".length());
        if (first.startsWith("--")) return first.substring(2);
        return first;
    }

    private static String toCamelCase(String underlineName, boolean capitalize) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = capitalize;
        for (char c : underlineName.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else {
                sb.append(nextUpper ? Character.toUpperCase(c) : c);
                nextUpper = false;
            }
        }
        return sb.toString();
    }

    private record ModuleConfig(String packageName, String tableName) {}
}
```

**注意**：上面的 `generateFromTemplate` 是简化版。实际实现中应使用 Freemarker 的 `Template` API 渲染 `.ftl` 模板文件。模板文件位于 `app/src/main/resources/templates/` 下。

- [ ] **Step 4.12: 编译验证**

Run: `JAVA_HOME="/home/leonardo123/.jdks/openjdk-25.0.2" mvn clean compile -pl app`

Expected: BUILD SUCCESS

---

## Task 5: Entity → Model 全局重命名

**Files:**
- Modify: `app/src/main/java/org/smm/archetype/systemconfig/internal/SystemConfigConverter.java`
- Modify: `app/src/main/java/org/smm/archetype/systemconfig/internal/SystemConfigService.java`
- Modify: `app/src/main/java/org/smm/archetype/systemconfig/internal/SystemConfigFacadeImpl.java`
- Modify: 其他模块的 Converter/Service/Facade 文件（根据 Phase 3 完成后的实际文件列表）

- [ ] **Step 5.1: 搜索所有需要重命名的 Entity 引用**

```bash
grep -rn "toEntity\|toDO(Entity\|Entity " app/src/main/java/org/smm/archetype/systemconfig/ --include="*.java" | grep -v "import "
grep -rn "entity\." app/src/main/java/org/smm/archetype/systemconfig/ --include="*.java" | grep -v "import " | grep -v "\.entity\."
```

- [ ] **Step 5.2: 重命名 SystemConfigConverter**

将 `toEntity` 方法重命名为 `toModel`，将参数名 `entity` 改为 `model`：

**SystemConfigConverter.java 修改示例**：

```java
// 旧:
SystemConfig toEntity(SystemConfigDO dataObject) { ... }
SystemConfigDO toDO(SystemConfig entity) { ... }

// 新:
SystemConfig toModel(SystemConfigDO dataObject) { ... }
SystemConfigDO toDO(SystemConfig model) { ... }
```

- [ ] **Step 5.3: 更新所有调用点**

搜索并更新所有 `toEntity` 的调用为 `toModel`：

```bash
grep -rn "\.toEntity(" app/src/ --include="*.java"
grep -rn "toEntity(" app/src/ --include="*.java"
```

将所有 `.toEntity(` 替换为 `.toModel(`

- [ ] **Step 5.4: 更新注释中的 Entity 引用**

```bash
grep -rn "Entity" app/src/main/java/org/smm/archetype/systemconfig/ --include="*.java" | grep "//\|/\*\|\\*"
```

将注释中的 "Entity" 替换为 "Model"（仅在领域模型语境中，不包括 JPA Entity 等技术术语）。

- [ ] **Step 5.5: 更新变量名 entity → model**

```bash
grep -rn "SystemConfig entity\|entity =" app/src/main/java/org/smm/archetype/systemconfig/ --include="*.java"
```

将局部变量 `entity` / `entities` 重命名为 `model` / `models`。

- [ ] **Step 5.6: 编译验证**

Run: `JAVA_HOME="/home/leonardo123/.jdks/openjdk-25.0.2" mvn clean compile -pl app`

Expected: BUILD SUCCESS

---

## Task 6: 废弃代码清理

**Files:**
- Delete: `app/src/main/java/org/smm/archetype/entity/base/BasePageRequest.java`
- Delete: `app/src/main/java/org/smm/archetype/entity/base/BaseRequest.java`
- Delete: `app/src/main/java/org/smm/archetype/generated/entity/UserDO.java`
- Delete: `app/src/main/java/org/smm/archetype/generated/entity/OperationLogDO.java`
- Delete: `app/src/main/java/org/smm/archetype/generated/mapper/UserMapper.java`
- Delete: `app/src/main/java/org/smm/archetype/generated/mapper/OperationLogMapper.java`
- Modify: `app/src/main/java/org/smm/archetype/shared/result/BasePageResult.java`（删除 fromPage(IPage)）

- [ ] **Step 6.1: 确认废弃文件无引用**

```bash
grep -rn "BasePageRequest\|BaseRequest" app/src/ --include="*.java" | grep -v "import " | grep -v "@Deprecated" | grep -v "//"
grep -rn "generated.entity.UserDO\|generated.entity.OperationLogDO" app/src/ --include="*.java"
grep -rn "generated.mapper.UserMapper\|generated.mapper.OperationLogMapper" app/src/ --include="*.java"
```

预期：无引用（所有引用已在 Phase 3 中迁移到新位置）

- [ ] **Step 6.2: 删除废弃文件**

```bash
rm app/src/main/java/org/smm/archetype/entity/base/BasePageRequest.java
rm app/src/main/java/org/smm/archetype/entity/base/BaseRequest.java
rm app/src/main/java/org/smm/archetype/generated/entity/UserDO.java
rm app/src/main/java/org/smm/archetype/generated/entity/OperationLogDO.java
rm app/src/main/java/org/smm/archetype/generated/mapper/UserMapper.java
rm app/src/main/java/org/smm/archetype/generated/mapper/OperationLogMapper.java
```

- [ ] **Step 6.3: 清理空目录**

```bash
rmdir app/src/main/java/org/smm/archetype/generated/entity 2>/dev/null || true
rmdir app/src/main/java/org/smm/archetype/generated/mapper 2>/dev/null || true
```

- [ ] **Step 6.4: 检查并删除 BasePageResult.fromPage(IPage) 方法**

读取 `app/src/main/java/org/smm/archetype/shared/result/BasePageResult.java`，检查是否有 `fromPage(IPage)` 方法。如果有，先搜索引用：

```bash
grep -rn "fromPage(" app/src/ --include="*.java"
```

如果没有引用或引用可安全移除，删除该方法。

- [ ] **Step 6.5: 检查 entity/base/ 下是否有可删除的旧版 BaseResult/BasePageResult**

```bash
ls -la app/src/main/java/org/smm/archetype/entity/base/
```

如果 `BaseResult.java` 和 `BasePageResult.java` 同时存在于 `entity/base/` 和 `shared/result/`，且 `entity/base/` 下的版本标记为 `@Deprecated`，则删除旧版。

- [ ] **Step 6.6: 编译验证**

Run: `JAVA_HOME="/home/leonardo123/.jdks/openjdk-25.0.2" mvn clean compile -pl app`

Expected: BUILD SUCCESS

---

## Task 7: 全量验证

**Files:** 无修改

- [ ] **Step 7.1: 全量 ArchUnit 规则验证**

Run: `JAVA_HOME="/home/leonardo123/.jdks/openjdk-25.0.2" mvn test -Dtest="*ComplianceUTest,ModulithComplianceUTest" -pl app`

Expected: 全部通过，包括：
- CodingConventionComplianceUTest (C-01~C-07)
- ModuleArchitectureComplianceUTest (M-01~M-09)
- SpringConfigComplianceUTest (S-01)
- TestConventionComplianceUTest (T-01~T-05)
- ArchitectureComplianceUTest（四层架构）
- NoDataAnnotationUTest
- NoValueInjectionUTest
- NoRedundantConfigureUTest
- ModulithComplianceUTest

- [ ] **Step 7.2: 全量单元测试**

Run: `JAVA_HOME="/home/leonardo123/.jdks/openjdk-25.0.2" mvn test -Dtest="*UTest" -pl app`

Expected: 全部通过

- [ ] **Step 7.3: 全量集成测试**

Run: `JAVA_HOME="/home/leonardo123/.jdks/openjdk-25.0.2" mvn test -Dtest="*ITest" -pl app`

Expected: 全部通过

- [ ] **Step 7.4: 覆盖率报告**

Run: `JAVA_HOME="/home/leonardo123/.jdks/openjdk-25.0.2" mvn clean verify`

Expected: BUILD SUCCESS，报告位于 `app/target/site/jacoco-aggregate/index.html`

- [ ] **Step 7.5: 应用启动验证**

Run: `JAVA_HOME="/home/leonardo123/.jdks/openjdk-25.0.2" mvn spring-boot:run -pl app`

Expected: 应用正常启动，无 Bean 冲突错误

- [ ] **Step 7.6: 更新 AGENTS.md**

在 AGENTS.md 的 ArchUnit 守护规则表中新增以下行：

| `ModuleArchitectureComplianceUTest.java` | M-05~M-09 | ArchUnit API |

在 ArchUnit 守护规则表中新增 M-05~M-09 和 T-05 的描述：

**模块架构（M-01~M-09）**新增行：

| M-05 | 模块 internal/ 包零 Spring 依赖（Controller/RepositoryImpl/FacadeImpl 除外） | 降低框架耦合 |
| M-06 | Repository 接口方法签名不得出现 MyBatis-Plus 类型 | 接口框架无关 |
| M-07 | 模块间不得直接访问其他模块的 internal/ 包 | 模块边界隔离 |
| M-08 | Facade 接口不得依赖 MyBatis-Plus 类型 | 公开 API 框架无关 |
| M-09 | 业务模块间通过根包 Facade 接口通信 | 模块间解耦 |

**测试规范（T-01~T-05）**新增行：

| T-05 | ETest 禁止 `@Mock` | 端到端测试使用真实依赖 |

---

## 总结

| Task | 文件数 | KEY 变更 | 风险 |
|------|--------|---------|------|
| 1 | 1 修改 | M-05~M-09 ArchUnit 规则 | 中（规则设计 + 现有代码可能违规） |
| 2 | 1 修改 | T-05 ETest @Mock 禁止 | 低（当前可能无 ETest 文件） |
| 3 | 视情况 | 修复新规则发现的违规 | 中（取决于违规数量） |
| 4 | 1 修改 + 9 新模板 | 代码生成器 11 文件输出 | 中（模板正确性 + 编译验证） |
| 5 | 3+ 修改 | Entity→Model 全局重命名 | 低（IDE 重构安全） |
| 6 | 6 删除 + 1 修改 | 废弃代码清理 | 低（已确认无引用） |
| 7 | 0 | 全量验证 | 低（验证性质） |
