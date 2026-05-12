# 阶段 2 Spec：systemconfig 模块化迁移

> 关联总纲：`docs/architecture/refactoring-plan.md` → 阶段 2
> 关联 Plan：`docs/superpowers/plans/2026-05-12-phase2-systemconfig.md`
> 前置依赖：阶段 1（PageQuery、PageResult、BasePageResult、BaseResult）
> 创建日期：2026-05-12
> 状态：待实施

---

## 概览

阶段 2 将 **systemconfig 模块** 从传统四层包结构（controller/service/repository/entity/facade）迁移到 Spring Modulith 风格的模块包结构（`systemconfig/` + `systemconfig/internal/`）。同时完成以下架构改进：

1. **消除 IPage 泄漏**：Repository/Service/Facade 全链路使用 `PageResult<T>` 替代 `IPage<T>`
2. **修复层级违规**：Service 层不再返回 Facade 层的 `ConfigGroupVO`
3. **增强领域模型**：SystemConfig 实体新增 `isEditable()` 业务方法
4. **DO/Mapper 归属**：将 generated/ 下的 DO 和 Mapper 迁入模块 internal/ 包

| 操作类型 | 数量 | 说明 |
|---------|------|------|
| 新建文件（源码） | 18 | 新包结构下的所有源文件 |
| 新建文件（测试） | 12 | 新包结构下的所有测试文件 |
| 删除文件（源码） | 20 | 旧包结构下的源文件 + generated 下的 DO/Mapper/Generator |
| 删除文件（测试） | 12 | 旧包结构下的测试文件 |
| 修改文件 | 1 | 代码生成器配置（更新输出包路径） |

---

## 一、迁移映射表

### 1.1 值对象（纯包迁移，无逻辑变更）

| 旧路径 | 新路径 | 变更类型 |
|--------|--------|---------|
| `entity.system.ConfigKey` | `systemconfig.internal.ConfigKey` | 包迁移 |
| `entity.system.ConfigValue` | `systemconfig.internal.ConfigValue` | 包迁移 |
| `entity.system.DisplayName` | `systemconfig.internal.DisplayName` | 包迁移 |

### 1.2 枚举（纯包迁移，无逻辑变更）

| 旧路径 | 新路径 | 变更类型 |
|--------|--------|---------|
| `entity.system.ConfigGroup` | `systemconfig.internal.ConfigGroup` | 包迁移 |
| `entity.system.ValueType` | `systemconfig.internal.ValueType` | 包迁移 |
| `entity.system.InputType` | `systemconfig.internal.InputType` | 包迁移 |

### 1.3 实体 + 查询对象（包迁移 + 逻辑增强）

| 旧路径 | 新路径 | 变更类型 |
|--------|--------|---------|
| `entity.system.SystemConfig` | `systemconfig.internal.SystemConfig` | 包迁移 + 新增 `isEditable()` |
| `entity.system.SystemConfigPageQuery` | `systemconfig.internal.SystemConfigPageQuery` | 包迁移 + 紧凑构造器适配 `PageQuery` 校验规则 |

### 1.4 数据层（包迁移 + 接口签名变更）

| 旧路径 | 新路径 | 变更类型 |
|--------|--------|---------|
| `generated.entity.SystemConfigDO` | `systemconfig.internal.SystemConfigDO` | 包迁移（脱离 generated/） |
| `generated.mapper.SystemConfigMapper` | `systemconfig.internal.SystemConfigMapper` | 包迁移（脱离 generated/） |
| `repository.system.SystemConfigConverter` | `systemconfig.internal.SystemConfigConverter` | 包迁移 + `toEntity()` → `toModel()` |
| `repository.system.SystemConfigRepository` | `systemconfig.internal.SystemConfigRepository` | 包迁移 + `IPage` → `PageResult` |
| `repository.system.SystemConfigRepositoryImpl` | `systemconfig.internal.SystemConfigRepositoryImpl` | 包迁移 + IPage→PageResult 转换 |

### 1.5 业务层（包迁移 + 接口/实现变更）

| 旧路径 | 新路径 | 变更类型 |
|--------|--------|---------|
| `facade.system.SystemConfigFacade` | `systemconfig.SystemConfigFacade` | 包迁移（模块公开 API） |
| `facade.system.SystemConfigFacadeImpl` | `systemconfig.internal.SystemConfigFacadeImpl` | 包迁移 + PageResult + ConfigGroupVO 修复 |
| `facade.system.SystemConfigVO` | `systemconfig.internal.SystemConfigVO` | 包迁移 |
| `facade.system.ConfigGroupVO` | `systemconfig.internal.ConfigGroupVO` | 包迁移 |
| `facade.system.UpdateConfigCommand` | `systemconfig.internal.UpdateConfigCommand` | 包迁移 |
| `service.system.SystemConfigService` | `systemconfig.internal.SystemConfigService` | 包迁移 + `getAllGroups()` 返回 `List<ConfigGroup>` |
| `controller.system.SystemConfigController` | `systemconfig.internal.SystemConfigController` | 包迁移 |
| `controller.system.UpdateConfigRequest` | `systemconfig.internal.UpdateConfigRequest` | 包迁移 |

### 1.6 代码生成器

| 旧路径 | 新路径 | 变更类型 |
|--------|--------|---------|
| `generated.MybatisPlusGenerator` | **修改**（保留位置） | 升级为多模块支持（--module 参数），不再迁移到各模块。详见 Phase 3/4 spec |

---

## 二、关键接口签名变更

### 2.1 SystemConfigRepository

```java
// 旧签名
IPage<SystemConfig> findByPage(SystemConfigPageQuery query);

// 新签名
PageResult<SystemConfig> findByPage(SystemConfigPageQuery query);
```

**影响范围**：RepositoryImpl、Service、FacadeImpl 的 `findByPage` 调用链。

### 2.2 SystemConfigService

```java
// 旧签名 — Service 层直接返回 Facade 层 VO（层级违规）
List<ConfigGroupVO> getAllGroups();
IPage<SystemConfig> findByPage(SystemConfigPageQuery query);

// 新签名 — Service 层返回领域对象
List<ConfigGroup> getAllGroups();
PageResult<SystemConfig> findByPage(SystemConfigPageQuery query);
```

**影响范围**：FacadeImpl（需要自行将 `ConfigGroup` 转换为 `ConfigGroupVO`）。

### 2.3 SystemConfigFacadeImpl.findByPage

```java
// 旧实现 — 手动 IPage→IPage<VO> 转换 + fromPage(IPage)
IPage<SystemConfig> entityPage = systemConfigService.findByPage(query);
Page<SystemConfigVO> voPage = new Page<>(...);
return BasePageResult.fromPage(voPage);

// 新实现 — 使用 PageResult + BasePageResult.from(PageResult)
PageResult<SystemConfig> pageResult = systemConfigService.findByPage(query);
List<SystemConfigVO> voList = pageResult.list().stream().map(this::toVO).toList();
return BasePageResult.from(new PageResult<>(voList, pageResult.total(), pageResult.pageNo(), pageResult.pageSize()));
```

### 2.4 SystemConfigConverter

```java
// 旧方法名
public SystemConfig toEntity(SystemConfigDO configDO);

// 新方法名（语义更准确：DO → 领域模型）
public SystemConfig toModel(SystemConfigDO configDO);
```

### 2.5 SystemConfig 实体

```java
// 新增业务方法
public boolean isEditable() {
    // 当配置值类型为 BOOLEAN 时不可编辑配置值本身，只能切换开关
    return this.valueType != ValueType.BOOLEAN;
}
```

---

## 三、SystemConfigPageQuery 适配

当前 `SystemConfigPageQuery` 使用独立的紧凑构造器做默认值处理。迁移后应复用 `shared.pagination.PageQuery` 的校验规则：

```java
// 新实现 — 委托 PageQuery 做基础分页校验，新增 groupCode 过滤
public record SystemConfigPageQuery(
        @Min(1) Integer pageNo,
        @Min(1) @Max(100) Integer pageSize,
        String groupCode
) {
    public SystemConfigPageQuery {
        PageQuery base = new PageQuery(
                pageNo == null ? 1 : pageNo,
                pageSize == null ? 20 : pageSize
        );
        pageNo = base.pageNo();
        pageSize = base.pageSize();
    }

    /**
     * 兼容无参绑定场景（Spring MVC @ModelAttribute）
     */
    public SystemConfigPageQuery() {
        this(null, null, null);
    }
}
```

---

## 四、ArchUnit 规则影响分析

### 4.1 现有规则与新包结构的关系

| ArchUnit 规则 | 扫描包模式 | 影响 |
|---------------|-----------|------|
| serviceShouldNotDependOnControllerLayer | `..service..` | 新 Service 在 `systemconfig.internal`，不在 `..service..`，**不受此规则约束** |
| repositoryShouldNotDependOnServiceOrController | `..repository..` | 新 Repository 在 `systemconfig.internal`，**不受约束** |
| entityShouldNotDependOnSpringFramework | `..entity..` | 新 Entity 在 `systemconfig.internal`，**不受约束** |
| controllerShouldNotDependOnServiceDirectly | `..controller..` | 新 Controller 在 `systemconfig.internal`，**不受约束** |
| facadeShouldNotDependOnRepository | `..facade..` | 新 Facade 在 `systemconfig`，**不受约束** |
| M-03 facade 不返回 Entity | `..facade..` | 新 Facade 在 `systemconfig`，**不受约束** |

### 4.2 结论

旧包结构删除后，以上规则对 systemconfig 模块**完全失效**。这是预期行为 — 模块化架构下，模块内部的结构约束由模块自身保证，不再依赖全局包模式匹配。

### 4.3 后续行动（不在本阶段范围）

- 阶段 3+ 需要为模块化结构添加新的 ArchUnit 规则（如 `systemconfig.internal` 不对外暴露等）
- 本阶段不新增/不修改 ArchUnit 规则

---

## 五、Spring Bean 冲突处理策略

### 5.1 问题

迁移过渡期（Task 1-6），新旧包中存在同名 Spring Bean（@Service, @Repository, @Component, @RestController），会导致 Bean 定义冲突。

### 5.2 解决方案

**新文件在 Task 1-6 期间不添加 Spring 注解**。具体策略：

| 类别 | Task 1-6 | Task 7 |
|------|----------|--------|
| SystemConfigService | 无 @Service | 添加 @Service |
| SystemConfigRepositoryImpl | 无 @Repository | 添加 @Repository |
| SystemConfigConverter | 无 @Component | 添加 @Component |
| SystemConfigController | 无 @RestController | 添加 @RestController |
| SystemConfigFacadeImpl | 无 @Service | 添加 @Service |
| SystemConfigMapper | 无 @Mapper（引用旧 generated 包） | 迁移到新包 + @Mapper |
| SystemConfigDO | 引用旧 generated 包 | 迁移到新包 |

**关键约束**：Task 1-6 期间，新代码的 RepositoryImpl 和 Service 仍引用**旧的** `generated.entity.SystemConfigDO` 和 `generated.mapper.SystemConfigMapper`，避免 MyBatis Mapper 重复注册。

### 5.3 验证方式

- Task 1-6：新代码只有 UTest（单元测试，直接构造对象），旧代码的 ITest 不受影响
- Task 7：删除旧代码后添加 Spring 注解，更新 ITest 导入路径

---

## 六、测试文件迁移映射

| 旧路径 | 新路径 | 变更说明 |
|--------|--------|---------|
| `entity.system.ConfigKeyDisplayNameUTest` | `systemconfig.internal.ConfigKeyDisplayNameUTest` | 更新 import 路径 |
| `repository.system.SystemConfigConverterUTest` | `systemconfig.internal.SystemConfigConverterUTest` | 更新 import + `toEntity` → `toModel` |
| `repository.system.SystemConfigRepositoryITest` | `systemconfig.internal.SystemConfigRepositoryITest` | 更新 import |
| `repository.system.SystemConfigPaginationITest` | `systemconfig.internal.SystemConfigPaginationITest` | `IPage` → `PageResult` 断言 |
| `facade.system.SystemConfigFacadeImplUTest` | `systemconfig.internal.SystemConfigFacadeImplUTest` | `IPage` → `PageResult` mock + ConfigGroup 修复 |
| `facade.system.SystemConfigFacadeITest` | `systemconfig.internal.SystemConfigFacadeITest` | 更新 import |
| `facade.system.SystemConfigFacadePaginationITest` | `systemconfig.internal.SystemConfigFacadePaginationITest` | 更新 import |
| `controller.system.SystemConfigControllerITest` | `systemconfig.internal.SystemConfigControllerITest` | 更新 import |
| `controller.system.SystemConfigControllerPaginationITest` | `systemconfig.internal.SystemConfigControllerPaginationITest` | 更新 import |
| `controller.system.SystemConfigPaginationBoundaryITest` | `systemconfig.internal.SystemConfigPaginationBoundaryITest` | 更新 import |
| `cases.integrationtest.WebExceptionAdviseI18nITest` | 不迁移 | 通过 HTTP 端点测试，不受包结构影响 |
| `cases.integrationtest.PaginationTaxonomyITest` | 不迁移 | 通过 HTTP 端点测试，不受包结构影响 |

---

## 七、验证清单

### 7.1 编译验证

- [ ] `mvn compile -pl app` 编译通过（每个 Task 完成后）
- [ ] 无 duplicate class 错误
- [ ] 无 missing import 错误

### 7.2 单元测试

- [ ] `mvn test -Dtest="*UTest" -pl app` 全部通过
- [ ] ConfigKeyDisplayNameUTest — 值对象校验 + SystemConfig.isEditable()
- [ ] SystemConfigConverterUTest — toDataObject + toModel + 往返一致性
- [ ] SystemConfigFacadeImplUTest — IPage mock 替换为 PageResult、ConfigGroup mock 修复

### 7.3 集成测试

- [ ] `mvn test -Dtest="*ITest" -pl app` 全部通过
- [ ] SystemConfigRepositoryITest — CRUD 操作
- [ ] SystemConfigPaginationITest — PageResult 断言（非 IPage）
- [ ] SystemConfigFacadeITest — 全链路功能
- [ ] SystemConfigFacadePaginationITest — 分页
- [ ] SystemConfigControllerITest — HTTP 端点
- [ ] SystemConfigControllerPaginationITest — 分页端点
- [ ] SystemConfigPaginationBoundaryITest — 边界值
- [ ] WebExceptionAdviseI18nITest — i18n（跨模块测试）
- [ ] PaginationTaxonomyITest — 分页分类学（跨模块测试）

### 7.4 ArchUnit 规则

- [ ] `mvn test -pl app` 中所有 ArchUnit 测试通过
- [ ] 旧包删除后无残余 import 引用旧包路径

### 7.5 代码规范

- [ ] 无 @Data 注解
- [ ] 无 @Value 注入
- [ ] 时间字段使用 Instant
- [ ] Controller 路径以 /api 开头
- [ ] 测试文件以 UTest.java 或 ITest.java 结尾
- [ ] UTest 不使用 @SpringBootTest
- [ ] ITest 不使用 @Mock

---

## 八、风险点

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Spring Bean 冲突（过渡期） | 应用启动失败 | Task 1-6 新代码不加 Spring 注解 |
| MyBatis Mapper 重复注册 | SQL 执行异常 | Task 1-6 引用旧 generated 包的 Mapper |
| ArchUnit 规则失效 | 架构约束缺失 | 本阶段不处理，阶段 3+ 补充模块化规则 |
| 跨模块测试引用旧 import | ITest 编译失败 | Task 6 统一更新所有测试 import |
| PageResult.totalPages 与 IPage.getPages 差异 | 断言失败 | SystemConfigPaginationITest 需同步调整 |
