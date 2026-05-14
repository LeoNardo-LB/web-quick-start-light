# shared/ 功能域隔离守护设计

> 日期：2026-05-14
> 状态：Draft
> 范围：web-quick-start-light 项目 — shared/ 横切关注点

## 背景与目标

`app/src/main/java/org/smm/archetype/shared/` 下有 15 个自包含功能域（context, dal, event, idempotent, logging, generated, mybatis, operationlog, pagination, ratelimit, result, threadpool, util, web, internal），目前通过约定保证各功能域间的依赖方向正确，但没有 ArchUnit 自动化守护。

本设计新增 **1 条 ArchUnit 规则**（S-02），确保 shared/ 功能域的分层依赖关系不被破坏。

## 现状分析

### 功能域分类

| 层级 | 功能域 | 特征 |
|------|--------|------|
| **底层** | context, event, idempotent, logging, operationlog, pagination, ratelimit, util | shared/ 内零跨功能域依赖，完全独立 |
| **上层** | dal, generated, mybatis, result, threadpool, web | 依赖底层或其他上层 |
| **特殊** | internal | 空包（无 Java 文件） |

### 现有跨功能域依赖

| 源功能域 | 目标功能域 | 依赖的类 | 依赖原因 |
|----------|-----------|---------|---------|
| dal | context | BizContext | 填充审计字段 |
| generated | dal | BaseDO | 代码生成父类 |
| mybatis | dal | InstantTypeHandler | 类型处理器注册 |
| result | pagination | PageResult | 分页结果包装 |
| threadpool | context | BizContext | 异步任务上下文传播 |
| web | context | BizContext | Filter 设置请求上下文 |
| web | result | BaseResult | 统一返回格式 |

**结论**：无循环依赖，依赖方向合理（底层→底层 禁止，底层→上层 禁止，上层→底层 允许，上层→上层 允许）。

## 设计方案

### S-02：shared/ 功能域分层隔离

- **来源**：新规则
- **强度**：⛔ MUST
- **检测方式**：ArchUnit API
- **扫描范围**：`org.smm.archetype.shared` 下的直接子包

#### 规则定义

1. **底层包之间零互相依赖** — 底层 8 个包互不 import 对方的类
2. **底层包不得依赖上层包** — 底层不能"向上看"
3. **上层包可依赖任意底层包** — 合法的分层引用
4. **上层包之间允许单向依赖** — 如 generated→dal, web→result 是合理的组合关系，但不得形成循环
5. **未分类功能域检测** — shared/ 下的直接子包数（排除 internal/）必须等于 BOTTOM + TOP 集合大小，防止新增功能域被静默忽略

#### 底层包白名单

```
org.smm.archetype.shared.context
org.smm.archetype.shared.event
org.smm.archetype.shared.idempotent
org.smm.archetype.shared.logging
org.smm.archetype.shared.operationlog
org.smm.archetype.shared.pagination
org.smm.archetype.shared.ratelimit
org.smm.archetype.shared.util
```

#### 实现策略

**选定方案：ArchUnit `noClasses()` 依赖规则组合**

为每个底层包编写规则：该包不得依赖同层其他底层包、不得依赖任何上层包。这确保了底层包的完全独立性。使用 ArchUnit 的 `noClasses().that().resideInAPackage(..).should().dependOnClassesThat().resideInAnyPackage(..)` 链式 API。

```java
// 伪代码示意（非可编译代码，展示核心逻辑）
private static final String SHARED = "..shared.";
private static final Set<String> BOTTOM = Set.of(
    "context", "event", "idempotent", "logging",
    "operationlog", "pagination", "ratelimit", "util"
);
private static final Set<String> TOP = Set.of(
    "dal", "generated", "mybatis", "result", "threadpool", "web"
);

// 规则 1: 底层包不依赖其他底层包
// 对每个底层包 P：
noClasses()
    .that().resideInAPackage(SHARED + P + "..")
    .should().dependOnClassesThat()
    .resideInAnyPackage(/* 其他所有底层包的 FQN */)
    .check(importedClasses);

// 规则 2: 底层包不依赖上层包
// 对每个底层包 P：
noClasses()
    .that().resideInAPackage(SHARED + P + "..")
    .should().dependOnClassesThat()
    .resideInAnyPackage(/* 所有上层包的 FQN */)
    .check(importedClasses);

// 规则 5: 未分类功能域检测
// 断言 shared/ 直接子包数 == BOTTOM.size + TOP.size（排除 internal）
// 防止新功能域未在集合中登记
```

> 注：ArchUnit 无 `noReverseDependency` API，上述使用的是 `noClasses().should().dependOnClassesThat()` 链式调用。

#### 测试文件

新增 `SharedIsolationUTest.java`，位于 `app/src/test/java/org/smm/archetype/support/basic/`。

包含 3 个 `@ArchTest` 字段：
- `shared_bottom_packages_should_not_depend_on_each_other` — 规则 1
- `shared_bottom_packages_should_not_depend_on_top_packages` — 规则 2
- `all_shared_subpackages_should_be_classified` — 规则 5（未分类检测）

### 实现清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `SharedIsolationUTest.java` | 新增 | S-02 规则测试类 |
| `docs/conventions/java-conventions.md` | 更新 | 新增 S-02 规则说明 |
| `docs/architecture/module-structure.md` | 更新 | ArchUnit 规则列表 44→45 |

### 排除项

- `internal/` 空包（含空子目录 `aspect/operationlog/`，无 Java 文件）：测试中跳过（ArchUnit 不会扫描到类）；若未来添加 Java 文件，须在 BOTTOM/TOP 集合中登记，否则被规则 5 拦截
- `generated/` 中的 `MybatisPlusGenerator.java`：跨模块代码生成器（工具类而非运行时组件），已依赖 shared.dal.BaseDO 作为模板基类，属于合理的上层→上层关系，不需要排除
- `shared/CentralMapperConfig.java`（根目录文件）：MapStruct 全局配置，不属于任何功能域子包，ArchUnit 的子包匹配模式（`..shared.xxx..`）自然不覆盖到它

> **子包语义**：S-02 规则以功能域（直接子包）为最小粒度，功能域内部的子包（如 `shared.dal.entity`、`shared.ratelimit` 内部类）视为同一功能域。`resideInAPackage("..shared.context..")` 会匹配 `shared.context` 及其所有子包。

## 验证标准

- `mvn test -pl app` 通过，测试数 586→589（+3：规则 1 底层间零依赖 + 规则 2 底层不依赖上层 + 规则 5 未分类检测）
- 当前代码零违规（所有断言基于现有代码结构）
- 手动破坏测试场景：
  - ① 在底层包中添加对另一底层包的 import → 规则 1 失败
  - ② 在底层包中添加对上层包的 import → 规则 2 失败
  - ③ 在 shared/ 下新增未登记的子包并添加 Java 文件 → 规则 5 失败

## 不做什么

- 不重构现有跨包依赖（已确认合理）
- 不引入三层或更细粒度分层（二层足够）
- 不检查 shared/ 对外部模块的依赖（已有 Modulith 和 M-10 守护）
