# 阶段 4 Spec：ArchUnit 规则加强 + 代码生成器升级 + 废弃清理

## 概览

阶段 4 在 Phase 3 完成的基础上，加强架构约束的自动化守护能力，升级代码生成器至完整模块骨架输出，并清理所有历史遗留的废弃代码。

### 前置条件

- Phase 3 已完成：auth/、operationlog/、systemconfig/ 三个业务模块均已模块化（`模块名/` + `模块名/internal/`）
- Spring Modulith 已引入，`ApplicationModules.verify()` 测试通过
- 所有业务模块的 Facade 接口位于根包（如 `systemconfig.SystemConfigFacade`），内部实现位于 `internal/`

### 核心变更

1. **新增 ArchUnit 规则 5 条**（M-05~M-09）：守护模块化架构的内部边界和接口规范
2. **新增测试规范规则 1 条**（T-05）：ETest 禁止使用 @Mock
3. **代码生成器升级**：从 2 文件（DO+Mapper）升级到 11 文件完整模块骨架
4. **Entity → Model 全局重命名**：统一领域模型命名
5. **废弃代码清理**：删除 Phase 1~3 标记的历史遗留

### 操作统计

| 操作类型 | 数量 | 说明 |
|---------|------|------|
| 新增规则 | 6 | M-05~M-09 + T-05 |
| 修改测试文件 | 2 | ModuleArchitectureComplianceUTest + TestConventionComplianceUTest |
| 修改源码文件 | 1 | MybatisPlusGenerator.java（11 文件输出） |
| 重命名文件/变量 | 10+ | Entity → Model 命名统一 |
| 删除废弃文件 | 3 | BasePageRequest/BaseRequest + generated/ 旧输出 |
| 修改文档 | 1 | AGENTS.md（新增规则描述） |

---

## 一、新增 ArchUnit 规则详解

### 1.1 M-05: 模块 `internal/` 包零 Spring 依赖（Controller/RepositoryImpl 除外）

**目标**：确保模块内部实现不直接依赖 Spring Framework，降低框架耦合。Controller 和 RepositoryImpl 因需要 Spring 注解（`@RestController`、`@Repository`）而例外。

**检测逻辑**：
- 扫描所有业务模块（auth/、operationlog/、systemconfig/）下的 `internal/` 包
- 检查是否有类 `dependOnClassesThat().resideInAPackage("org.springframework..")`
- **例外类**：
  - `*Controller.java`（需要 `@RestController`、`@RequestMapping` 等）
  - `*RepositoryImpl.java`（需要 `@Repository`）
  - `*FacadeImpl.java`（需要 `@Service`）

**实现方式**：ArchUnit API

```java
// 伪代码
noClasses()
    .that().resideInAPackage("..auth.internal..")
    .and().resideInAPackage("..operationlog.internal..")
    .and().resideInAPackage("..systemconfig.internal..")
    .and().haveSimpleNameNotEndingWith("Controller")
    .and().haveSimpleNameNotEndingWith("RepositoryImpl")
    .and().haveSimpleNameNotEndingWith("FacadeImpl")
    .should().dependOnClassesThat()
    .resideInAPackage("org.springframework..")
```

**注意**：
- `shared/` 模块不在检测范围（shared 是跨层基础设施，合理使用 Spring）
- Model/DO/Converter/Service/PageQuery/VO 等类型不应依赖 Spring
- 动态发现模块列表，而非硬编码模块名

### 1.2 M-06: Repository 接口方法签名不得出现 MyBatis-Plus 类型

**目标**：确保 Repository 接口不泄漏 ORM 框架细节（`IPage`、`Page` 等类型），保持接口的框架无关性。

**检测逻辑**：
- 扫描所有 `*Repository.java` 接口（非 Impl）
- 检查所有 public 方法的参数和返回值类型
- 禁止出现 `com.baomidou.mybatisplus.*` 包下的任何类型

**实现方式**：ArchUnit API

```java
// 伪代码：检查方法签名中的类型引用
classes()
    .that().haveSimpleNameEndingWith("Repository")
    .and().areInterfaces()
    .should(notReferenceMybatisPlusTypes())
```

**例外**：无。Repository 接口应完全框架无关，MyBatis-Plus 类型仅允许出现在 `*RepositoryImpl.java` 中。

### 1.3 M-07: 模块间不得直接访问其他模块的 `internal/` 包

**目标**：模块间通信必须通过根包的 Facade 接口，禁止直接引用其他模块的内部实现。

**检测逻辑**：
- 对每个业务模块，检查其代码是否依赖了其他业务模块的 `internal/` 包
- 业务模块列表：auth、operationlog、systemconfig（动态发现）
- `shared/` 模块不在限制范围（它是基础设施层）

**实现方式**：ArchUnit API

```java
// 对每个模块
noClasses()
    .that().resideInAPackage("..auth..")
    .should().dependOnClassesThat()
    .resideInAPackage("..operationlog.internal..")

noClasses()
    .that().resideInAPackage("..auth..")
    .should().dependOnClassesThat()
    .resideInAPackage("..systemconfig.internal..")
// ... 其他组合
```

**例外**：
- `shared/` 包可以被任何模块访问（基础设施层）
- `controller/` 包中的全局异常处理可以引用 Facade 接口

### 1.4 M-08: Facade 接口不得依赖 MyBatis-Plus 类型

**目标**：Facade 接口是模块的公开 API，不应暴露 ORM 框架细节。

**检测逻辑**：
- 扫描所有业务模块根包下的 `*Facade.java` 接口
- 检查方法签名、参数、返回值中是否引用 `com.baomidou.mybatisplus.*` 包的类型

**实现方式**：ArchUnit API

```java
classes()
    .that().haveSimpleNameEndingWith("Facade")
    .and().areInterfaces()
    .should(notDependOnMybatisPlus())
```

**例外**：无。Facade 接口是公开契约，必须框架无关。

### 1.5 M-09: 业务模块间通过根包 Facade 接口通信

**目标**：业务模块间不得直接注入其他模块的 internal 类（如 Service、Repository），必须通过 Facade 接口。

**检测逻辑**：
- 等价于 M-07，但更侧重于 Bean 注入层面
- 实际上 M-07 已覆盖包级别的隔离，M-09 作为补充语义约束
- 检测方式：与 M-07 合并实现，使用 ArchUnit 的包依赖检查

**实现方式**：与 M-07 合并，通过 Spring Modulith 的 `ApplicationModules.verify()` 辅助验证

### 1.6 T-05: ETest 禁止使用 @Mock

**目标**：端到端测试（ETest）应使用真实依赖，不使用 Mockito mock，与 T-03（ITest 禁止 @Mock）保持一致。

**检测逻辑**：
- 扫描所有 `*ETest.java` 文件
- 检查是否出现 `@Mock` 注解（排除 `@MockBean`、`@MockitoBean` 等变体）
- 与 T-03 实现模式完全一致

**实现方式**：SourceScanner

### 1.7 M-10: Model 类不得使用 `@Data`（已有 NoDataAnnotationUTest 扩展）

**说明**：NoDataAnnotationUTest 已经覆盖全项目的 `@Data` 禁止检查。Phase 4 不需要新增规则，只需确认现有规则已覆盖 Model 类。

---

## 二、代码生成器升级

### 2.1 当前状态

MybatisPlusGenerator 仅生成 2 个文件：
- `{TableName}DO.java` — 数据对象（Entity）
- `{TableName}Mapper.java` — MyBatis-Plus Mapper 接口

### 2.2 目标状态

升级为生成完整模块骨架（11 个文件），输出到指定模块的包结构中：

| # | 文件 | 说明 | 模板类型 |
|---|------|------|---------|
| 1 | `{Name}DO.java` | 数据对象 | MyBatis-Plus 生成 |
| 2 | `{Name}Mapper.java` | Mapper 接口 | MyBatis-Plus 生成 |
| 3 | `{Name}.java` | 领域模型（Model） | 自定义模板 |
| 4 | `{Name}Repository.java` | Repository 接口 | 自定义模板 |
| 5 | `{Name}RepositoryImpl.java` | Repository 实现 | 自定义模板 |
| 6 | `{Name}Converter.java` | DO↔Model 转换器 | 自定义模板 |
| 7 | `{Name}Facade.java` | Facade 接口（根包） | 自定义模板 |
| 8 | `{Name}FacadeImpl.java` | Facade 实现 | 自定义模板 |
| 9 | `{Name}Service.java` | Service 层 | 自定义模板 |
| 10 | `{Name}Controller.java` | Controller | 自定义模板 |
| 11 | `{Name}VO.java` | 视图对象 | 自定义模板 |

### 2.3 输出路径

```
org.smm.archetype.{module}/
├── {Name}Facade.java                    ← 根包（公开 API）
└── internal/
    ├── {Name}.java                      ← Model
    ├── {Name}DO.java                    ← DO（MyBatis-Plus 生成）
    ├── {Name}Mapper.java                ← Mapper（MyBatis-Plus 生成）
    ├── {Name}Repository.java            ← Repository 接口
    ├── {Name}RepositoryImpl.java        ← Repository 实现
    ├── {Name}Service.java               ← Service
    ├── {Name}Converter.java             ← DO↔Model 转换
    ├── {Name}FacadeImpl.java            ← Facade 实现
    ├── {Name}Controller.java            ← Controller
    ├── {Name}VO.java                    ← VO
    └── {Name}PageQuery.java             ← 分页查询参数（可选）
```

### 2.4 生成的代码必须满足

- 所有 ArchUnit 规则（M-01~M-09）
- 不使用 `@Data`（用 `@Builder` + `@RequiredArgsConstructor`）
- 时间字段使用 `Instant`
- Facade 接口不依赖 MyBatis-Plus 类型
- Repository 接口不暴露 `IPage`/`Page`
- Controller 路径以 `/api` 开头

---

## 三、Entity → Model 全局重命名

### 3.1 目标

在 Phase 3 中已对 operationlog 模块完成了 `toEntity → toModel` 重命名。Phase 4 将此重命名推广到所有模块，统一领域模型的命名。

### 3.2 重命名映射

| 位置 | 旧名称 | 新名称 | 说明 |
|------|--------|--------|------|
| Converter 方法名 | `toEntity()` | `toModel()` | DO→Model 转换 |
| Converter 方法名 | `toEntity(DO)` | `toModel(DO)` | 重载方法 |
| Converter 方法名 | `toDO(Entity)` | `toDO(Model)` | 参数类型 |
| Service 方法注释 | "Entity" | "Model" | Javadoc |
| Facade 方法注释 | "Entity" | "Model" | Javadoc |
| 变量名 | `entity` / `entities` | `model` / `models` | 局部变量 |

### 3.3 不重命名的内容

- `BaseDO` 保持不变（数据对象基类）
- `*DO.java` 文件名保持不变（数据对象）
- `entity/base/` 包名保持不变（历史兼容）

---

## 四、废弃代码清理

### 4.1 Phase 1~3 标记的废弃文件

| 文件 | 标记阶段 | 删除原因 |
|------|---------|---------|
| `entity/base/BasePageRequest.java` | Phase 1 | 被 `PageQuery` 替代 |
| `entity/base/BaseRequest.java` | Phase 1 | traceId 由 OTel 管理 |
| `generated/entity/UserDO.java` | Phase 3 | 已迁入 auth 模块 |
| `generated/entity/OperationLogDO.java` | Phase 3 | 已迁入 operationlog 模块 |
| `generated/mapper/UserMapper.java` | Phase 3 | 已迁入 auth 模块 |
| `generated/mapper/OperationLogMapper.java` | Phase 3 | 已迁入 operationlog 模块 |

### 4.2 BasePageResult.fromPage(IPage) 方法清理

- `BasePageResult.java` 中如果仍有 `fromPage(IPage)` 静态工厂方法，需删除
- 此方法直接依赖 MyBatis-Plus 的 `IPage` 类型，违反 M-08

### 4.3 旧包结构下的空目录

删除迁移完成后残留的空目录（仅删除确认为空的）。

### 4.4 保留的旧文件

- `entity/base/BaseDO.java` — 仍在使用（数据对象基类）
- `entity/base/BaseResult.java` — 如已迁移到 `shared/result/` 则可删除旧版
- `entity/base/BasePageResult.java` — 如已迁移到 `shared/result/` 则可删除旧版

---

## 五、验证标准

### 5.1 编译验证

```bash
mvn clean compile -pl app
```

预期：BUILD SUCCESS

### 5.2 ArchUnit 规则

```bash
mvn test -Dtest="*ComplianceUTest,*ModulithComplianceUTest" -pl app
```

预期：全部通过，包括新增的 M-05~M-09、T-05 规则

### 5.3 全量测试

```bash
mvn clean test -pl app
```

预期：全部通过

### 5.4 Spring Modulith

```bash
mvn test -Dtest="ModulithComplianceUTest" -pl app
```

预期：PASS

### 5.5 应用启动

```bash
mvn spring-boot:run -pl app
```

预期：应用正常启动，无 Bean 冲突

### 5.6 代码生成器

```bash
# 使用 test profile 连接测试数据库
mvn exec:java -pl app -Dexec.mainClass="org.smm.archetype.generated.MybatisPlusGenerator" -Dexec.args="--module=systemconfig"
```

预期：生成 11 个文件，内容符合所有 ArchUnit 规则

---

## 六、风险点

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| M-05 Spring 依赖检测误报（如 Model 使用 Spring 注解） | 中 | 规则失败 | 精确设计例外类列表 |
| 代码生成器模板不符合 ArchUnit 规则 | 中 | 生成代码需手动修复 | 先运行 ArchUnit 验证模板输出 |
| Entity→Model 重命名导致编译失败 | 低 | 编译中断 | IDE 重构 + 全量编译验证 |
| BasePageResult.fromPage(IPage) 被其他代码引用 | 低 | 编译失败 | 先搜索引用，再决定是否删除 |

---

## 七、任务分解概览

| Task | 范围 | 说明 |
|------|------|------|
| **Task 1** | M-05~M-09 规则 | 新增 5 条 ArchUnit 规则到 ModuleArchitectureComplianceUTest |
| **Task 2** | T-05 规则 | 新增 ETest 禁止 @Mock 规则到 TestConventionComplianceUTest |
| **Task 3** | 修复违规 | 运行新规则，修复代码使规则通过 |
| **Task 4** | 代码生成器升级 | MybatisPlusGenerator 从 2 文件升级到 11 文件 |
| **Task 5** | Entity→Model 重命名 | 全局重命名 Converter/Service/Facade 中的 Entity→Model |
| **Task 6** | 废弃清理 | 删除废弃文件 + 清理空目录 + 删除 BasePageResult.fromPage(IPage) |
| **Task 7** | 全量验证 | 编译 + ArchUnit + 单元测试 + 集成测试 + 应用启动 |

---

## 变更历史

| 日期 | 版本 | 说明 |
|------|------|------|
| 2026-05-12 | v1.0 | 初始版本 |
