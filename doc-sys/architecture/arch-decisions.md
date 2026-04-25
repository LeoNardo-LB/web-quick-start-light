# 架构决策记录

> **职责**: 记录 web-quick-start-light 项目中的关键架构决策（ADR），包括决策背景、方案选择、后果与状态
> **轨道**: Intent
> **维护者**: Human

## 目录

- [概述](#概述)
- [设计背景](#设计背景)
- [技术方案](#技术方案)
  - [ADR-001: Template Method 统一组件骨架](#adr-001-template-method-统一组件骨架)
  - [ADR-002: DDD-lite 范围界定](#adr-002-ddd-lite-范围界定)
  - [ADR-003: ConditionalOnMissingBean 覆盖策略](#adr-003-conditionalonmissingbean-覆盖策略)
  - [ADR-004: ScopedValue 替代 ThreadLocal](#adr-004-scopedvalue-替代-threadlocal)
  - [ADR-005: SQLite 嵌入式选型](#adr-005-sqlite-嵌入式选型)
  - [ADR-006: Serial GC 选型](#adr-006-serial-gc-选型)
  - [ADR-007: 三层异常分类](#adr-007-三层异常分类)
  - [ADR-008: 四层分层架构](#adr-008-四层分层架构)
  - [ADR-009: AOP 切面内聚到 shared-aspect](#adr-009-aop-切面内聚到-shared-aspect)
  - [ADR-010: Repository + Converter 数据访问模式](#adr-010-repository--converter-数据访问模式)
- [关键设计决策](#关键设计决策)
- [相关文档](#相关文档)
- [变更历史](#变更历史)

## 概述

本文档以 ADR（Architecture Decision Record）格式记录项目中的关键架构决策。每个决策包含：决策编号与标题、状态、背景与上下文、决策内容、后果与权衡。ADR 是**不可变的决策记录**——一旦采纳，后续变更应通过新 ADR 记录（supersede），而非修改原记录。当前项目共 10 个 ADR，涵盖组件骨架设计、分层策略、基础设施选型、上下文传播等核心主题。

---

## 设计背景

### 为什么需要 ADR？

在脚手架项目中，架构决策的影响力远超普通业务项目——每个决策都会被所有基于此脚手架的新项目继承。因此，每个决策必须：

1. **有据可查** — 新团队成员能理解"为什么这样做"而非仅看到"这样做"
2. **可追溯** — 当技术栈演进时，能回溯当初的权衡条件是否仍然成立
3. **不可变** — 决策记录本身不应被修改，变更通过新 ADR supersede
4. **可讨论** — 为未来的架构评审提供基础材料

### ADR 状态定义

| 状态 | 含义 |
|------|------|
| **已采纳 (Accepted)** | 决策已实施，当前生效 |
| **已废弃 (Deprecated)** | 决策曾采纳，但因条件变化不再适用 |
| **已替代 (Superseded)** | 被更新的 ADR 替代，附替代者编号 |

### 决策分类

| 分类 | ADR 编号 | 主题 |
|------|---------|------|
| **组件设计** | ADR-001, ADR-003 | Template Method、条件装配 |
| **架构分层** | ADR-002, ADR-008, ADR-010 | DDD-lite、四层架构、Repository 模式 |
| **基础设施** | ADR-004, ADR-005, ADR-006 | 上下文传播、数据库、GC |
| **横切关注点** | ADR-007, ADR-009 | 异常分类、AOP 切面 |

---

## 技术方案

### ADR-001: Template Method 统一组件骨架

| 字段 | 内容 |
|------|------|
| **状态** | 已采纳 |
| **决策者** | 架构师 |
| **影响范围** | 6 个 component 模块 |

#### 背景

项目需要 6 个基础设施组件（auth/cache/email/oss/search/sms），每个组件需要：
- 统一的参数校验逻辑
- 统一的日志记录（入口/出口/异常）
- 统一的异常转换（底层异常 → `ClientException`）
- 可替换的实现策略（Sa-Token / Caffeine / NoOp 等）

如果每个组件独立实现，会导致大量重复代码和一致性问题。

#### 决策

采用 **Template Method + Strategy** 设计模式：
1. 定义组件接口（如 `AuthComponent`）
2. 创建抽象基类（如 `AbstractAuthComponent`），公开方法标记 `final`
3. 基类 `final` 方法封装：参数校验 → 日志 → `do*()` 调用 → 异常转换
4. 子类通过 `protected abstract do*()` 实现核心逻辑
5. 通过 `@ConditionalOnMissingBean` 实现策略替换

#### 后果

**正面**：
- 6 个组件共享统一的骨架结构，横切逻辑零重复
- 扩展点数量和签名一目了然（auth 4 个、cache 8 个、search 14 个等）
- 新增组件只需按模板实现，降低认知负担
- 异常契约统一，上层代码无需区分组件来源

**负面**：
- 抽象层级增加，调试时需跨多个类
- 如果某个组件需要非常规的横切逻辑（如搜索组件需要三级参数校验），基类可能需要扩展
- 模板方法限制了公开方法的可覆盖性（`final` 关键字）

---

### ADR-002: DDD-lite 范围界定

| 字段 | 内容 |
|------|------|
| **状态** | 已采纳 |
| **决策者** | 架构师 |
| **影响范围** | entity/system 包 |

#### 背景

DDD（领域驱动设计）有多种实践深度：
- **DDD-Full**：聚合根、领域事件、领域服务、CQRS、Event Sourcing
- **DDD-Lite**：聚合根、值对象、领域服务、Repository
- **DDD-Naming**：仅使用 DDD 术语命名包和类

项目需要决定 DDD 的应用深度。

#### 决策

采用 **DDD-Lite** 策略，仅在 `system-config` 领域完整应用：

| DDD 概念 | 应用范围 | 说明 |
|---------|---------|------|
| **值对象 (Value Object)** | 仅 `entity/system` | `ConfigKey`, `ConfigValue`, `DisplayName` — 使用 Java record + 紧凑构造器校验 |
| **聚合根 (Aggregate Root)** | 仅 `SystemConfig` | 聚合 3 个值对象 + 3 个枚举，含 `updateValue` 领域行为 |
| **枚举值对象** | 仅 `entity/system` | `ConfigGroup`（4 分组含 UI 元数据）、`ValueType`（7 种）、`InputType`（7 种） |
| **Repository** | 所有领域 | 统一 Repository 接口 + 实现 + Converter 模式 |
| **领域事件** | 不应用 | 脚手架级别暂不需要 |
| **CQRS** | 不应用 | 读写分离对脚手架过重 |

**User 和 OperationLog 领域**不使用值对象，保持简单 POJO。

#### 后果

**正面**：
- 在最需要的地方（系统配置的复杂类型约束）应用 DDD，不过度设计
- 值对象的不可变性和校验规则通过 record compact constructor 天然表达
- 为基于此脚手架的项目展示了 DDD 的"最低可行实践"

**负面**：
- User 和 OperationLog 领域未使用值对象，与 SystemConfig 不一致
- 未来如果需要为 User 领域添加值对象（如 `Email`、`PhoneNumber`），需要补齐

---

### ADR-003: ConditionalOnMissingBean 覆盖策略

| 字段 | 内容 |
|------|------|
| **状态** | 已采纳 |
| **决策者** | 架构师 |
| **影响范围** | 6 个 AutoConfiguration 类 |

#### 背景

组件需要支持"默认实现 + 自定义覆盖"的能力。常见方案有：
1. **`@Primary`** — 标记默认 Bean，自定义 Bean 不加 `@Primary`
2. **`@ConditionalOnMissingBean`** — 仅在容器中无同名 Bean 时注册默认实现
3. **`@ConditionalOnProperty`** — 通过配置属性决定是否注册
4. **Profile 隔离** — 不同 Profile 注册不同实现

#### 决策

组合使用 `@ConditionalOnProperty`（开关） + `@ConditionalOnMissingBean`（覆盖）：

```java
@Configuration
@ConditionalOnProperty(prefix = "archetype.component.auth", name = "enabled",
                       havingValue = "true", matchIfMissing = true)
public class AuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuthComponent.class)
    public AuthComponent authComponent(AuthProperties properties) {
        // Sa-Token 存在 → SaTokenAuthComponent，否则 → NoOpAuthComponent
    }
}
```

| 组件 | 默认 enabled | 默认实现 | matchIfMissing |
|------|:----------:|---------|:-------------:|
| component-auth | `true` | Sa-Token → `SaTokenAuthComponent`，否则 → `NoOpAuthComponent` | `true` |
| component-cache | `true` | `CaffeineCacheComponent` | `true` |
| component-email | `false` | `NoOpEmailComponent` | `false` |
| component-oss | `true` | `LocalOssComponent` | `true` |
| component-search | `true` | `SimpleSearchComponent` | `true` |
| component-sms | `false` | `NoOpSmsComponent` | `false` |

#### 后果

**正面**：
- 上层模块只需定义一个 `@Bean XxxComponent` 即可完全替换默认实现
- `matchIfMissing=true` 确保不配置时提供合理默认值
- `enabled=false` 的组件（email/sms）不会加载，减少启动时间

**负面**：
- `@ConditionalOnMissingBean` 的匹配基于 Bean 类型，如果存在多个候选 Bean 可能产生歧义
- 需要注意 Bean 注册顺序（`AutoConfiguration` 的 `@AutoConfigureBefore/After`）

---

### ADR-004: ScopedValue 替代 ThreadLocal

| 字段 | 内容 |
|------|------|
| **状态** | 已采纳 |
| **决策者** | 架构师 |
| **影响范围** | `BizContext`、`ContextFillFilter`、`ThreadPoolConfigure` |

#### 背景

业务上下文传播（用户 ID、租户 ID 等）传统上使用 `ThreadLocal` 实现。但 Java 21+ 引入虚拟线程后，`ThreadLocal` 存在严重问题：
- 虚拟线程池会缓存线程，`ThreadLocal` 值不会自动清除
- 大量虚拟线程 × `ThreadLocal` = 内存泄漏风险
- `ThreadLocal` 的可变性在并发场景下容易产生 bug

#### 决策

使用 Java 25 的 `ScopedValue` API 实现 `BizContext`：

```java
private static final ScopedValue<Holder> SCOPED = ScopedValue.newInstance();

public static void runWithContext(Runnable action, EnumMap<Key, String> context) {
    Baggage baggage = buildBaggage(context);  // OTel Baggage 同步
    Holder holder = new Holder(context, false);
    if (baggage != null) {
        try (Scope _ = baggage.makeCurrent()) {
            ScopedValue.where(SCOPED, holder).run(action);
        }
    } else {
        ScopedValue.where(SCOPED, holder).run(action);
    }
}
```

关键设计点：
- **不可变快照** — 请求线程创建 Holder，子线程通过 `copyAsReplica()` 创建副本
- **副本隔离** — 子线程的 `set()` 仅修改自己的 EnumMap，不写入 OTel Baggage（并打印 WARN）
- **OTel 联动** — `propagated=true` 的 Key 自动同步到 OTel Baggage，实现跨服务传播
- **三合一传播** — BizContext + OTel Context + MDC 通过 `ContextPropagatingTaskDecorator` 统一处理

#### 后果

**正面**：
- 虚拟线程安全，无内存泄漏风险
- 作用域生命周期明确（`ScopedValue.where().run()` 块结束即清除）
- 不可变性消除了并发修改的 bug 类别
- 与 OTel Baggage 的联动实现跨服务上下文传播

**负面**：
- `ScopedValue` 是 Java 25 preview API 的正式版，要求 JDK 25+
- 无法在 `ScopedValue` 绑定后修改值（需通过副本模式间接实现可变性）
- 子线程修改不回传父线程（这是设计意图，但可能让使用者困惑）

---

### ADR-005: SQLite 嵌入式选型

| 字段 | 内容 |
|------|------|
| **状态** | 已采纳 |
| **决策者** | 架构师 |
| **影响范围** | `pom.xml`、`schema.sql`、`init.sql`、`InstantTypeHandler` |

#### 背景

作为脚手架项目，数据库选型需要在"开箱即用"和"生产可用"之间平衡：

| 方案 | 优势 | 劣势 |
|------|------|------|
| **H2** | 纯 Java、SQL 标准兼容好 | 与 MySQL 语法差异、无生产级持久化 |
| **SQLite** | 零安装、单文件、广泛使用 | 并发写入限制、SQL 方言差异 |
| **MySQL/PostgreSQL** | 生产级、功能完整 | 需要安装配置、增加脚手架启动成本 |
| **HSQLDB** | 纯 Java、内存模式快 | 社区活跃度低 |

#### 决策

选择 **SQLite** 作为默认数据库：

- **零运维** — 无需安装任何数据库服务，JDBC URL `jdbc:sqlite:./data/app.db` 即可
- **单文件持久化** — 数据存储在单个 `.db` 文件中，便于备份和迁移
- **广泛验证** — Android、iOS、浏览器、嵌入式设备均使用 SQLite，可靠性经过验证
- **MyBatis-Plus 兼容** — 通过 `InstantTypeHandler` 解决时间类型映射问题

同时提供 MySQL DDL 模板（`schema-template.sql`）和代码生成器（`MybatisPlusGenerator`），支持切换。

#### 后果

**正面**：
- 脚手架 `mvn spring-boot:run` 即可运行，无需任何外部依赖
- 集成测试使用内存 SQLite（`jdbc:sqlite::memory:`），测试隔离性好
- 适合演示、PoC、低并发内部工具等场景

**负面**：
- SQLite 不支持并发写入（WAL 模式下允许一个写者 + 多个读者）
- 无原生 `DATETIME` 类型，需要 `InstantTypeHandler` 做格式转换（ISO-8601 / datetime / epoch）
- 部分 SQL 语法与 MySQL/PostgreSQL 不兼容（如 `AUTO_INCREMENT` vs `AUTOINCREMENT`）
- 不支持存储过程、视图等高级特性

---

### ADR-006: Serial GC 选型

| 字段 | 内容 |
|------|------|
| **状态** | 已采纳 |
| **决策者** | 架构师 |
| **影响范围** | `scripts/start.sh` |

#### 背景

脚手架项目面向小内存部署场景（总内存 1G），JVM 堆大小 512MB。GC 选择需考虑：
- **吞吐量** — 脚手架类应用对延迟不敏感，吞吐量优先
- **内存开销** — GC 自身的数据结构不应占用过多内存
- **暂停时间** — 512MB 堆下暂停应可接受

| GC | 512MB 堆吞吐量 | 额外内存开销 | 适用场景 |
|----|:----------:|:--------:|---------|
| **Serial GC** | 最高 | ~0 | ≤1G 堆 |
| **Parallel GC** | 高 | 低 | 多核 + 中等堆 |
| **G1 GC** | 中 | 中（~10%） | 大堆 + 低延迟 |
| **ZGC / Shenandoah** | 中低 | 高（~20%） | 超大堆 + 超低延迟 |

#### 决策

使用 **Serial GC**（`-XX:+UseSerialGC`），内存分配方案：

| 区域 | 大小 | 占比 |
|------|:----:|:----:|
| 堆内存 (Heap) | 512MB | 50% |
| 元空间 (Metaspace) | 128MB | 13% |
| 代码缓存 (CodeCache) | 64MB | 6% |
| 直接内存 (DirectMem) | 64MB | 6% |
| 线程栈 (ThreadStack) | ~100MB | 10% |
| GC + JVM 内部 | ~90MB | 9% |
| **合计** | **~958MB** | **≈1G** |

#### 后果

**正面**：
- Serial GC 零额外内存开销（无 remembered sets / region tables / barriers）
- 512MB 堆下 Full GC 暂停通常 <100ms
- 吞吐量最高（无并发协调开销）
- Oracle 官方推荐小数据集使用 Serial GC

**负面**：
- Full GC 会暂停所有应用线程（Stop-The-World）
- 不适合大堆（>2G）或低延迟要求的场景
- 多核 CPU 无法利用并行回收（但 512MB 堆的回收本身很快）

---

### ADR-007: 三层异常分类

| 字段 | 内容 |
|------|------|
| **状态** | 已采纳 |
| **决策者** | 架构师 |
| **影响范围** | `common` 模块、`WebExceptionAdvise` |

#### 背景

异常处理是架构中最容易被忽视但又最影响用户体验的部分。常见问题：
- 所有异常都用 `RuntimeException`，无法区分业务异常和系统异常
- 错误码缺乏规范，前端无法针对性处理
- 异常信息直接暴露给用户，存在安全风险

#### 决策

设计三级异常层级：

```
BaseException (abstract)
├── BizException        — 业务异常（用户操作错误，如参数不合法）
├── ClientException     — 客户端/外部依赖异常（如第三方服务超时）
└── SysException        — 系统内部异常（如 NPE、数据库连接失败）
```

错误码分段规范：

| 段 | 范围 | 含义 | HTTP 状态码 |
|----|------|------|:---------:|
| 1xxx | 1000-1999 | 成功/通用 | 200 |
| 2xxx | 2000-2999 | 业务异常 | 400/409 |
| 5xxx | 5000-5999 | 系统异常 | 500 |
| 6xxx | 6000-6999 | 中间件/认证 | 401/403 |
| 9xxx | 9000-9999 | 兜底 | 500 |

`CommonErrorCode` 定义了 16 个标准错误码，覆盖参数校验、认证、数据库、缓存、文件等常见场景。

#### 后果

**正面**：
- `WebExceptionAdvise` 根据异常类型自动映射 HTTP 状态码，Controller 无需处理异常
- 错误码分段规范便于前端针对性处理（如 6xxx 跳转登录页）
- i18n 支持（`MessageSource` + `messages.properties`）
- 组件层异常统一转换为 `ClientException`，上层无需关心底层差异

**负面**：
- 三级分类在某些边界场景下可能模糊（如"用户不存在"算 Biz 还是 Client？）
- 错误码维护需要持续投入，新增错误码需遵循分段规范

---

### ADR-008: 四层分层架构

| 字段 | 内容 |
|------|------|
| **状态** | 已采纳 |
| **决策者** | 架构师 |
| **影响范围** | 全部 app 模块代码 |

#### 背景

常见 Java Web 分层方案：
- **两层**：Controller → Service（过于简单）
- **三层**：Controller → Service → DAO（最常见，但 Service 职责过重）
- **四层**：Controller → Facade → Service → Repository（更清晰的职责分离）
- **六边形**：端口 + 适配器（适合复杂领域，但脚手架过重）

#### 决策

采用 **四层分层架构**，由 ArchUnit 测试守护分层边界：

| 规则 | ArchUnit 测试方法 |
|------|-----------------|
| Controller 不能直接依赖 Service | `controllerShouldNotDependOnServiceDirectly` |
| Controller 不能依赖 Repository | `controllerShouldOnlyDependOnServiceLayer` |
| Service 不能依赖 Controller | `serviceShouldNotDependOnControllerLayer` |
| Repository 不能依赖 Service 或 Controller | `repositoryShouldNotDependOnServiceOrControllerLayer` |
| Entity 不能依赖 Spring Framework | `entityShouldNotDependOnSpringFramework` |
| Facade 不能依赖 Repository | `facadeShouldNotDependOnRepository` |

> **例外**：`LoginController` 被允许直接依赖 Service 层（因为 Login 场景没有 Facade 层，直接调用 `LoginFacadeImpl`）。

#### 后果

**正面**：
- 分层边界由测试守护，重构时自动检测违规
- Facade 层提供用例级别的编排入口，一个 API 端点对应一个 Facade 方法
- Service 层可被多个 Facade 复用，避免代码重复

**负面**：
- 增加了类的数量（每个领域至少 5 个类：Entity + Repository 接口 + Repository 实现 + Converter + Service）
- `SystemConfigService → ConfigGroupVO` 的已知违规说明四层规则在实践中需要灵活性

---

### ADR-009: AOP 切面内聚到 shared-aspect

| 字段 | 内容 |
|------|------|
| **状态** | 已采纳 |
| **决策者** | 架构师 |
| **影响范围** | `shared-aspect` 包、3 个 AOP 切面 |

#### 背景

幂等、日志、限流三个横切关注点最初可能分散在不同组件中。需要决定它们的组织方式：
1. **独立组件模块** — 每个切面一个 Maven 模块（如 `component-idempotent`）
2. **内聚到 shared 包** — 放在 `app/shared/aspect/` 下
3. **Spring Boot Starter** — 打包为独立 Starter

#### 决策

将 AOP 切面内聚到 `shared-aspect` 包下，与 `shared-util` 并列：

| 切面 | 注解 | 依赖组件 |
|------|------|---------|
| `IdempotentAspect` | `@Idempotent` | `CacheComponent`（来自 component-cache） |
| `LogAspect` | `@BusinessLog` | `OperationLogWriter`（策略接口） |
| `RateLimitAspect` | `@RateLimit` | Bucket4j（内置） |

#### 后果

**正面**：
- 三个切面共享相同的设计结构（注解 + Aspect + 策略接口），内聚后便于统一维护
- `shared-aspect` 依赖 `component-cache`（仅 IdempotentAspect 使用），依赖方向清晰
- 通过 Config 层的 `IdempotentConfigure`/`LoggingConfigure`/`RatelimitConfigure` 实例化 Bean

**负面**：
- `shared-aspect` 不是独立 Maven 模块，与 `shared-util` 混在同一个 Maven 模块中
- 如果需要单独复用某个切面，需要引入整个 shared 模块

---

### ADR-010: Repository + Converter 数据访问模式

| 字段 | 内容 |
|------|------|
| **状态** | 已采纳 |
| **决策者** | 架构师 |
| **影响范围** | `repository.*` 包 |

#### 背景

数据访问层的对象映射有几种常见模式：
1. **DO 直出** — Mapper 直接返回 DO，Service 层使用 DO
2. **Repository + 内联转换** — Repository 方法内用 `toEntity()` 转换
3. **Repository + 独立 Converter** — 抽取 Converter 类，遵循单一职责

#### 决策

采用 **Repository + Converter** 模式，将 DO（MyBatis-Plus 数据对象）与 Entity（领域对象）严格分离：

```
接口 (XxxRepository) → 实现 (XxxRepositoryImpl) → 转换器 (XxxConverter) → Mapper (generated)
```

| 领域 | Repository 接口 | Repository 实现 | Converter | Mapper |
|------|----------------|----------------|-----------|--------|
| user | `UserRepository` | `UserRepositoryImpl` | **内联 `toEntity()`** | `UserMapper` |
| operation-log | `OperationLogRepository` | `OperationLogRepositoryImpl` | `OperationLogConverter` | `OperationLogMapper` |
| system-config | `SystemConfigRepository` | `SystemConfigRepositoryImpl` | `SystemConfigConverter` | `SystemConfigMapper` |

> **已知不一致**：User 模块使用内联 `toEntity()` 而非独立 Converter，应补齐。

#### 后果

**正面**：
- Entity 与 DO 解耦，领域层不依赖 ORM 框架
- Converter 可独立测试和复用
- MapStruct 支持自动生成 Converter 实现

**负面**：
- User 模块的 Converter 未抽取，与其他领域不一致
- 增加了类的数量（每个领域至少 3 个类：Repository 接口 + 实现 + Converter）

---

## 关键设计决策

### 决策总览

| ADR | 决策 | 核心权衡 | 影响范围 |
|:---:|------|---------|---------|
| 001 | Template Method 统一组件骨架 | 一致性 vs 灵活性 | 6 个 component 模块 |
| 002 | DDD-lite 范围界定 | 实用性 vs 完整性 | entity/system 包 |
| 003 | ConditionalOnMissingBean 覆盖策略 | 默认值 vs 可定制 | 6 个 AutoConfiguration |
| 004 | ScopedValue 替代 ThreadLocal | 虚拟线程安全 vs JDK 版本要求 | BizContext 全链路 |
| 005 | SQLite 嵌入式选型 | 零运维 vs 生产级能力 | 数据库层 |
| 006 | Serial GC 选型 | 吞吐量 vs 暂停时间 | JVM 启动参数 |
| 007 | 三层异常分类 | 可处理性 vs 简洁性 | 全局异常处理 |
| 008 | 四层分层架构 | 职责清晰 vs 类数量 | 全部 app 模块 |
| 009 | AOP 切面内聚到 shared-aspect | 内聚性 vs 模块化 | shared-aspect 包 |
| 010 | Repository + Converter 模式 | 解耦 vs 复杂度 | repository.* 包 |

### 待决策项

以下主题尚未形成正式 ADR，属于未来的架构演进方向：

1. **事件驱动** — 是否引入领域事件（Spring ApplicationEvent 或消息队列）
2. **CQRS** — 读写分离是否必要（当前只有查询场景）
3. **多租户** — 租户隔离策略（数据库级 / Schema 级 / 行级）
4. **缓存策略** — 本地缓存（Caffeine）到分布式缓存（Redis）的切换路径
5. **API 版本化** — URL 版本（`/api/v1/`）vs Header 版本

---

## 相关文档

| 文档 | 关系 | 说明 |
|------|------|------|
| [架构总览](arch-overview.md) | 交叉 | 分层架构、模块拓扑、技术栈全景 |
| [组件设计模式](../components/component-pattern.md) | 下游 | ADR-001 和 ADR-003 的具体实现 |
| [异常体系](../business/exception-system.md) | 下游 | ADR-007 的错误码定义与 i18n |
| [上下文传播机制](../infrastructure/context-propagation.md) | 下游 | ADR-004 的 BizContext 完整设计 |
| [编码规范](../guides/coding-standards.md) | 下游 | ADR-008 的 ArchUnit 守护规则 |
| [部署与运维](../guides/deployment.md) | 下游 | ADR-005 和 ADR-006 的运行时配置 |
