# 整洁架构改造总纲

> **文档性质**：Contract 轨 — 架构改造的契约文档，各阶段实施以此为总参照。
> **创建日期**：2026-05-11
> **状态**：待实施

---

## 一、改造背景

### 1.1 动机

当前项目采用技术分包（controller/facade/service/repository/entity），存在以下问题：

1. **业务逻辑散落在 Service 层**：Model（原 Entity）是贫血模型，所有行为通过 Service 的事务脚本实现
2. **ORM 框架泄漏**：MyBatis-Plus 的 `IPage` 渗透到 Repository 接口、Service 层甚至 `BasePageResult`
3. **技术分包导致功能碎片化**：理解一个业务功能需跨越 5+ 个包，类文件分散
4. **模块边界无强制约束**：模块间依赖关系仅靠人工纪律
5. **有数据库替换需求**：SQLite → MySQL，但 ORM 类型泄漏导致改动面大

### 1.2 已确认的关键决策

| 决策项 | 结论 |
|--------|------|
| 业务逻辑归属 | 内聚到 Model，Service/Facade 作为编排者 |
| 依赖倒置 | Repository 接口去除 `IPage`，返回自有 `PageResult<T>` |
| 分包策略 | 从技术分包改为业务分包（Spring Modulith） |
| Spring Modulith | 与分层改造同步引入 |
| 命名变更 | Entity → Model 同步进行 |
| 代码生成器 | 需要适配新架构 |
| 组件插件化 | 保持现状（Spring AutoConfiguration 已是 SPI 机制） |
| ArchUnit | 与 Spring Modulith 互补，Modulith 管模块边界，ArchUnit 管编码规范 |

### 1.3 不做的事

- 不引入 JDK SPI（Spring AutoConfiguration 已足够）
- 不引入 Use Case 类（CRUD 场景不需要每个操作一个类）
- 不将 Service 层改为纯 POJO（`@Transactional` 是合理的 Spring 耦合）
- 不追求 100% 框架独立（`@Service`、`@Transactional` 注解保留在 Service 上）
- 不将贫血模型全面改为 DDD 富领域模型（只在有业务行为需求时逐步添加）

---

## 二、目标架构

### 2.1 整体模块结构

```
org.smm.archetype/
├── systemconfig/                    ← @ApplicationModule(id = "systemconfig")
│   ├── SystemConfigFacade.java      ← 公开 API（唯一对外接口）
│   └── internal/                    ← 以下全部模块内部
│       ├── SystemConfigFacadeImpl.java
│       ├── SystemConfigService.java     ← 业务编排
│       ├── SystemConfig.java            ← Model（含业务行为）
│       ├── ConfigKey.java               ← 值对象 (record)
│       ├── ConfigValue.java             ← 值对象 (record)
│       ├── ConfigGroup.java             ← 枚举
│       ├── ValueType.java               ← 枚举
│       ├── InputType.java               ← 枚举
│       ├── DisplayName.java             ← 值对象 (record)
│       ├── SystemConfigRepository.java  ← 仓库接口（零框架类型）
│       ├── SystemConfigRepositoryImpl.java
│       ├── SystemConfigConverter.java   ← DO ↔ Model 转换
│       ├── SystemConfigDO.java          ← 持久化对象
│       ├── SystemConfigMapper.java      ← Mapper
│       ├── SystemConfigController.java  ← HTTP 入口
│       ├── SystemConfigVO.java          ← VO (record)
│       ├── ConfigGroupVO.java
│       ├── UpdateConfigRequest.java     ← Request (record)
│       ├── UpdateConfigCommand.java     ← Command (record)
│       └── SystemConfigPageQuery.java   ← 分页查询 (record)
│
├── auth/                            ← @ApplicationModule(id = "auth")
│   ├── AuthFacade.java              ← 公开 API
│   └── internal/
│       ├── AuthFacadeImpl.java
│       ├── LoginService.java
│       ├── User.java
│       ├── UserRepository.java
│       ├── UserRepositoryImpl.java
│       ├── UserConverter.java
│       ├── UserDO.java
│       ├── UserMapper.java
│       ├── LoginController.java
│       ├── LoginRequest.java
│       └── LoginVO.java
│
├── operationlog/                    ← @ApplicationModule(id = "operationlog")
│   ├── OperationLogFacade.java      ← 公开 API
│   └── internal/
│       ├── OperationLogFacadeImpl.java
│       ├── OperationLogService.java
│       ├── OperationLog.java
│       ├── OperationLogRepository.java
│       ├── OperationLogRepositoryImpl.java
│       ├── OperationLogConverter.java
│       ├── OperationLogDO.java
│       ├── OperationLogMapper.java
│       ├── OperationLogController.java
│       ├── OperationLogVO.java
│       └── OperationLogPageQuery.java
│
├── shared/                          ← @ApplicationModule(type = OPEN)
│   ├── pagination/
│   │   ├── PageQuery.java
│   │   └── PageResult.java
│   ├── result/
│   │   ├── BaseResult.java
│   │   └── BasePageResult.java
│   ├── context/
│   │   └── BizContext.java
│   ├── aspect/
│   │   ├── ratelimit/
│   │   ├── idempotent/
│   │   ├── operationlog/
│   │   └── ...
│   └── ...
└── test/tools/
    └── MybatisPlusGenerator.java    ← 代码生成器（开发工具）
```

### 2.2 Maven 模块结构（保持不变）

```
web-quick-start-light/              (根 POM)
├── common/                          (异常体系)
├── components/                      (6 个技术组件)
│   ├── component-cache/
│   ├── component-oss/
│   ├── component-email/
│   ├── component-sms/
│   ├── component-search/
│   └── component-auth/
└── app/                             (主应用，业务模块重构在此模块内)
```

Maven 多模块结构不变，改造范围仅限 `app` 模块内部的包结构重组。

### 2.3 调用链与依赖方向

```
Controller (internal/)
    │  调用 Facade 接口
    ▼
Facade 接口 (根包，公开 API)
    │  FacadeImpl (internal/ 实现) 编排 Service + Model→VO 转换
    ▼
Service (internal/)
    │  编排 Model 行为 + 协调 Repository
    │  调用 Model 的业务方法
    ▼
Model (internal/)
    │  承载业务行为和状态
    ▼
Repository 接口 (internal/)
    │  返回 Model / PageResult<Model>
    │  ⚠️ 零 MyBatis-Plus 类型
    ▼
RepositoryImpl (internal/)
    │  DO↔Model 转换 + IPage→PageResult 转换
    ▼
Mapper (internal/)
```

**依赖规则**：
- 模块根包（Facade 接口）是唯一对外暴露的 API
- `internal/` 内所有类型对其他模块不可见
- `internal/` 内的类不依赖 Spring Framework、MyBatis-Plus 的类型（Controller/RepositoryImpl 例外）
- 基础设施细节（IPage、Mapper）封装在 RepositoryImpl 内部

### 2.4 各层职责定位

| 位置 | 职责 | 可以做 | 不可以做 |
|------|------|--------|---------|
| **根包/Facade** | 模块对外 API | 暴露公开方法 | 暴露 internal 类型 |
| **internal/Model** | 承载业务行为 | 校验、状态变更、业务规则计算 | 依赖框架、访问数据库 |
| **internal/Service** | 编排业务流程 | 协调多个 Model、调用 Repository | 直接操作 DO/Mapper/IPage |
| **internal/Repository** | 数据访问抽象接口 | 定义 CRUD 方法 | 返回 MyBatis-Plus 类型 |
| **internal/FacadeImpl** | 编排 + 转换 | Model→VO 转换、调用 Service | 直接操作 Repository/DO |
| **internal/RepositoryImpl** | 框架适配 | 操作 Mapper、DO↔Model、IPage→PageResult | 暴露框架类型到上层 |
| **internal/Controller** | HTTP 入口 | 接收请求、调用根包 Facade 接口 | 直接调用 Service/Repository |

---

## 三、类型命名规范

### 3.1 对象类型对照

| 位置 | 对象类型 | 命名 | 示例 | 文件位置 |
|------|---------|------|------|---------|
| internal/ | 领域模型 | 无后缀 | `SystemConfig` | `模块/internal/` |
| internal/ | 值对象 | 无后缀 (record) | `ConfigKey` | `模块/internal/` |
| internal/ | 枚举 | 无后缀 (enum) | `ConfigGroup` | `模块/internal/` |
| internal/ | 仓库接口 | `*Repository` | `SystemConfigRepository` | `模块/internal/` |
| internal/ | 数据对象 | `*DO` | `SystemConfigDO` | `模块/internal/` (代码生成) |
| internal/ | Mapper | `*Mapper` | `SystemConfigMapper` | `模块/internal/` (代码生成) |
| internal/ | 转换器 | `*Converter` | `SystemConfigConverter` | `模块/internal/` |
| internal/ | 仓库实现 | `*RepositoryImpl` | `SystemConfigRepositoryImpl` | `模块/internal/` |
| internal/ | 业务编排 | `*Service` | `SystemConfigService` | `模块/internal/` |
| 根包 | 编排接口 | `*Facade` | `SystemConfigFacade` | `模块/` (公开 API) |
| internal/ | 编排实现 | `*FacadeImpl` | `SystemConfigFacadeImpl` | `模块/internal/` |
| internal/ | 写命令 | `*Command` (record) | `UpdateConfigCommand` | `模块/internal/` |
| internal/ | 视图对象 | `*VO` (record) | `SystemConfigVO` | `模块/internal/` |
| internal/ | HTTP 请求 | `*Request` (record) | `UpdateConfigRequest` | `模块/internal/` |
| internal/ | 分页查询 | `*PageQuery` (record) | `SystemConfigPageQuery` | `模块/internal/` |
| shared/ | 分页请求 | `PageQuery` (record) | — | `shared/pagination/` |
| shared/ | 分页结果 | `PageResult<T>` (record) | — | `shared/pagination/` |
| shared/ | 统一响应 | `BaseResult<T>` | — | `shared/result/` |
| shared/ | 分页响应 | `BasePageResult<T>` | — | `shared/result/` |

### 3.2 Entity → Model 重命名说明

概念转变：原来叫 "Entity" 的领域对象改称为 "Model"。

- **类名不变**：`SystemConfig` 仍然是 `SystemConfig`，不加 Model 后缀
- **概念转变**：从"贫血 Entity（数据容器）"变为"Model（可承载业务行为）"
- **包名变更**：`entity/system/` → `systemconfig/internal/`（模块内部扁平化）
- **变量/注释**：代码注释和文档中的 "Entity" 统一改为 "Model"
- **Converter 方法名**：`toEntity()` → `toModel()`，`fromEntity()` → `fromModel()`

### 3.3 三层模型流转

```
适配层模型（Controller 用）
  Request record / VO record / BaseResult / BasePageResult
       ↕ Facade 转换
核心层模型（Facade/Service 用）
  Model / Command / PageQuery / PageResult<Model>
       ↕ RepositoryImpl 转换
仓储层模型（Repository 用）
  DO / Mapper
```

**规则**：每层只感知自己的模型类型，不跨层引用。
- Controller 不接触 Model 和 DO
- Service 不接触 VO 和 DO
- RepositoryImpl 负责所有转换

---

## 四、分页设计

### 4.1 核心原则

- **分页是技术关注点，不是领域概念**
- `PageQuery` 和 `PageResult<T>` 放在 `shared/pagination/`
- **`IPage` 仅在 RepositoryImpl 中使用，绝不泄漏到 Repository 接口之上**

### 4.2 类型定义

```java
// shared/pagination/PageQuery.java
public record PageQuery(
    @Min(1) int pageNo,
    @Min(1) @Max(100) int pageSize
) {
    public PageQuery {
        if (pageNo <= 0) pageNo = 1;
        if (pageSize <= 0) pageSize = 10;
        if (pageSize > 100) pageSize = 100;
    }
}

// shared/pagination/PageResult.java
public record PageResult<T>(
    List<T> list,
    long total,
    int pageNo,
    int pageSize,
    int totalPages
) {
    public static <T> PageResult<T> of(List<T> list, long total, int pageNo, int pageSize) {
        return new PageResult<>(list, total, pageNo, pageSize,
            (int) Math.ceil((double) total / pageSize));
    }
}
```

### 4.3 各层分页类型使用规则

| 层 | 输入 | 输出 | 可见的分页类型 |
|----|------|------|--------------|
| Controller | `*PageQuery` record | `BasePageResult<VO>` | PageQuery, PageResult, BaseResult |
| Facade | `*PageQuery` record | `BasePageResult<VO>` | PageQuery, PageResult, BaseResult |
| Service | `*PageQuery` record | `PageResult<Model>` | PageQuery, PageResult |
| **Repository 接口** | `*PageQuery` record | `PageResult<Model>` | PageQuery, PageResult |
| **RepositoryImpl** | 内部创建 `Page<DO>` | 转换为 `PageResult<Model>` | **IPage 仅在此层使用** |

### 4.4 废弃类型

| 废弃类 | 替代品 | 说明 |
|--------|--------|------|
| `BasePageRequest` | `PageQuery` record | record 无法继承 class，设计为独立 record |
| `BasePageResult.fromPage(IPage)` | `PageResult.of()` | `BasePageResult` 不再直接接触 `IPage` |

---

## 五、四阶段实施计划

### 阶段 1：基础设施层 ✅ 已对齐

**目标**：创建新架构的共享类型基础，不触碰现有业务代码。

**已确认决策**：
- BaseResult/BasePageResult 从 `entity/base/` 迁移到 `shared/result/`，去除 `IPage` 依赖
- PageQuery 采用组合方案：各模块 PageQuery 扁平化定义分页字段，紧凑构造器复用 `PageQuery` 校验
- BaseRequest 废弃（traceId 由 OTel 管理，requestId 由 Filter 生成，不放请求体）
- BasePageRequest 废弃（被 PageQuery record 替代）
- 操作日志接口契约层迁入 common 模块
- common/pom.xml 移除多余的 spring-boot-starter 依赖

**详细操作清单**：

| # | 操作 | 文件 | 说明 |
|---|------|------|------|
| 1 | 新增 | `shared/pagination/PageQuery.java` | 分页请求 record（1-based，含校验） |
| 2 | 新增 | `shared/pagination/PageResult.java` | 分页结果 record（泛型，零框架依赖） |
| 3 | 迁移 | `entity/base/BaseResult.java` → `shared/result/BaseResult.java` | 内容不变，仅迁移位置 |
| 4 | 迁移+重写 | `entity/base/BasePageResult.java` → `shared/result/BasePageResult.java` | 去除 `IPage` 依赖，新增 `from(PageResult<T>)` 静态工厂 |
| 5 | 废弃 | `entity/base/BaseRequest.java` | traceId 由 OTel 管理，无需请求基类 |
| 6 | 废弃 | `entity/base/BasePageRequest.java` | 被 PageQuery record 替代 |
| 7 | 迁移 | 4 个文件 → `common/operationlog/` | OperationType/OperationLogRecord/OperationLogWriter/BusinessLog |
| 8 | 优化 | `common/pom.xml` | 移除 spring-boot-starter，common 零 Spring 依赖 |

**关键类型定义**：

```java
// shared/pagination/PageQuery.java
public record PageQuery(
    @Min(1) int pageNo,
    @Min(1) @Max(100) int pageSize
) {
    public PageQuery {
        if (pageNo <= 0) pageNo = 1;
        if (pageSize <= 0) pageSize = 10;
        if (pageSize > 100) pageSize = 100;
    }
}

// shared/pagination/PageResult.java
public record PageResult<T>(
    List<T> list,
    long total,
    int pageNo,
    int pageSize,
    int totalPages
) {
    public static <T> PageResult<T> of(List<T> list, long total, int pageNo, int pageSize) {
        return new PageResult<>(list, total, pageNo, pageSize,
            (int) Math.ceil((double) total / pageSize));
    }
}

// shared/result/BasePageResult.java — 去除 IPage 依赖后的关键方法
public static <T> BasePageResult<T> from(PageResult<T> pageResult) {
    BasePageResult<T> result = new BasePageResult<>();
    result.setData(pageResult.list());
    result.setTotal(pageResult.total());
    result.setPageNo(pageResult.pageNo());
    result.setPageSize(pageResult.pageSize());
    result.setSuccess(true);
    result.setCode(CommonErrorCode.SUCCESS.code());
    result.setMessage(CommonErrorCode.SUCCESS.message());
    result.setTime(Instant.now());
    result.setTraceId(Span.current().getSpanContext().getTraceId());
    return result;
}
```

**业务 PageQuery 模式**（各模块参考）：

```java
// 扁平化 + 紧凑构造器复用 PageQuery 校验
public record SystemConfigPageQuery(
    int pageNo,
    int pageSize,
    String groupCode
) {
    public SystemConfigPageQuery {
        var pq = new PageQuery(pageNo, pageSize);
        pageNo = pq.pageNo();
        pageSize = pq.pageSize();
    }
    public PageQuery toPageQuery() {
        return new PageQuery(pageNo, pageSize);
    }
}
```

**验证**：
- 编译通过
- 所有现有测试通过（新旧类型共存）
- `BasePageResult` 不再依赖 `IPage`
- common 模块零 Spring 依赖
- Spring Modulith 未引入（阶段 2）

**风险**：🟢 低（纯增量，不改现有业务代码）

---

### 阶段 2：试点模块（systemconfig）✅ 已对齐

**目标**：用一个模块完整验证所有新模式。

**已确认决策**：
- 模块内部结构采用 Spring Modulith 约定：**根包 = 公开 API（Facade 接口），`internal/` = 模块内部**
- 模块内部扁平化（本项目模块小，15-20 类，不需要 domain/application/infrastructure/ui 分层）
- Spring Modulith 延迟到阶段 3 引入（避免对未改造模块的干扰），阶段 2 先按目标包结构组织
- DO/Mapper 生成到各模块 `internal/` 下（方案 B：代码生成器多次执行）
- 代码生成器本身放 `test/tools/`
- 测试采用源码镜像方式组织
- PageQuery 放模块 `internal/`（语义属于核心层，Controller 引用它符合依赖方向）

**目标包结构**：

```
org.smm.archetype/
├── systemconfig/                              ← 业务模块根包
│   ├── SystemConfigFacade.java                ← 公开 API（唯一对外接口）
│   │
│   └── internal/                              ← 以下全部模块内部
│       ├── SystemConfigFacadeImpl.java        ← Facade 实现
│       ├── SystemConfigService.java           ← 业务编排
│       ├── SystemConfig.java                  ← Model（含业务行为）
│       ├── ConfigKey.java                     ← 值对象 (record)
│       ├── ConfigValue.java                   ← 值对象 (record)
│       ├── DisplayName.java                   ← 值对象 (record)
│       ├── ConfigGroup.java                   ← 枚举
│       ├── ValueType.java                     ← 枚举
│       ├── InputType.java                     ← 枚举
│       ├── SystemConfigRepository.java        ← 仓库接口（零 IPage）
│       ├── SystemConfigRepositoryImpl.java    ← MyBatis-Plus 实现
│       ├── SystemConfigConverter.java         ← DO ↔ Model 转换
│       ├── SystemConfigDO.java                ← 生成：持久化对象
│       ├── SystemConfigMapper.java            ← 生成：Mapper 接口
│       ├── SystemConfigController.java        ← HTTP 入口
│       ├── SystemConfigVO.java                ← VO (record)
│       ├── ConfigGroupVO.java                 ← VO (record)
│       ├── UpdateConfigRequest.java           ← Request (record)
│       ├── UpdateConfigCommand.java           ← Command (record)
│       └── SystemConfigPageQuery.java         ← 分页查询 (record)
│
├── shared/                                    ← @ApplicationModule(type = OPEN)
│   ├── pagination/
│   │   ├── PageQuery.java
│   │   └── PageResult.java
│   ├── result/
│   │   ├── BaseResult.java
│   │   └── BasePageResult.java
│   ├── context/
│   │   └── BizContext.java
│   └── aspect/
│       └── ...
│
├── auth/                                      ← 其他模块暂不动（保持旧结构）
├── operationlog/                              ← 其他模块暂不动
└── generated/                                 ← 空（DO/Mapper 已移入各模块）
```

**文件迁移映射**：

| # | 旧位置 | 新位置 | 改动 |
|---|--------|--------|------|
| **公开 API** | | | |
| 1 | `facade/system/SystemConfigFacade.java` | `systemconfig/SystemConfigFacade.java` | 返回类型适配 |
| **模块内部** | | | |
| 2 | `facade/system/SystemConfigFacadeImpl.java` | `systemconfig/internal/SystemConfigFacadeImpl.java` | 重写分页转换 |
| 3 | `service/system/SystemConfigService.java` | `systemconfig/internal/SystemConfigService.java` | 返回 `PageResult<Model>` |
| 4 | `entity/system/SystemConfig.java` | `systemconfig/internal/SystemConfig.java` | 补充业务行为 |
| 5 | `entity/system/ConfigKey.java` | `systemconfig/internal/ConfigKey.java` | 仅迁移 |
| 6 | `entity/system/ConfigValue.java` | `systemconfig/internal/ConfigValue.java` | 仅迁移 |
| 7 | `entity/system/DisplayName.java` | `systemconfig/internal/DisplayName.java` | 仅迁移 |
| 8 | `entity/system/ConfigGroup.java` | `systemconfig/internal/ConfigGroup.java` | 仅迁移 |
| 9 | `entity/system/ValueType.java` | `systemconfig/internal/ValueType.java` | 仅迁移 |
| 10 | `entity/system/InputType.java` | `systemconfig/internal/InputType.java` | 仅迁移 |
| 11 | `repository/system/SystemConfigRepository.java` | `systemconfig/internal/SystemConfigRepository.java` | 返回 `PageResult<Model>` |
| 12 | `repository/system/SystemConfigRepositoryImpl.java` | `systemconfig/internal/SystemConfigRepositoryImpl.java` | IPage→PageResult 转换 |
| 13 | `repository/system/SystemConfigConverter.java` | `systemconfig/internal/SystemConfigConverter.java` | toEntity→toModel |
| 14 | `generated/entity/SystemConfigDO.java` | `systemconfig/internal/SystemConfigDO.java` | 代码生成器输出路径变更 |
| 15 | `generated/mapper/SystemConfigMapper.java` | `systemconfig/internal/SystemConfigMapper.java` | 代码生成器输出路径变更 |
| 16 | `controller/system/SystemConfigController.java` | `systemconfig/internal/SystemConfigController.java` | 仅迁移 |
| 17 | `facade/system/SystemConfigVO.java` | `systemconfig/internal/SystemConfigVO.java` | 仅迁移 |
| 18 | `facade/system/ConfigGroupVO.java` | `systemconfig/internal/ConfigGroupVO.java` | 仅迁移 |
| 19 | `controller/system/UpdateConfigRequest.java` | `systemconfig/internal/UpdateConfigRequest.java` | 仅迁移 |
| 20 | `facade/system/UpdateConfigCommand.java` | `systemconfig/internal/UpdateConfigCommand.java` | 仅迁移 |
| 21 | `entity/system/SystemConfigPageQuery.java` | `systemconfig/internal/SystemConfigPageQuery.java` | 适配新 PageQuery |
| **测试（源码镜像）** | | | |
| 22 | `test/controller/system/SystemConfigControllerITest.java` | `test/systemconfig/internal/SystemConfigControllerITest.java` | import 更新 |
| 23 | `test/controller/system/SystemConfigControllerPaginationITest.java` | `test/systemconfig/internal/SystemConfigControllerPaginationITest.java` | import 更新 |
| 24 | `test/controller/system/SystemConfigPaginationBoundaryITest.java` | `test/systemconfig/internal/SystemConfigPaginationBoundaryITest.java` | import 更新 |
| 25 | `test/facade/system/SystemConfigFacadeImplUTest.java` | `test/systemconfig/internal/SystemConfigFacadeImplUTest.java` | import 更新 |
| 26 | `test/facade/system/SystemConfigFacadePaginationITest.java` | `test/systemconfig/internal/SystemConfigFacadePaginationITest.java` | import 更新 |
| 27 | `test/facade/system/SystemConfigFacadeITest.java` | `test/systemconfig/internal/SystemConfigFacadeITest.java` | import 更新 |
| 28 | `test/repository/system/SystemConfigConverterUTest.java` | `test/systemconfig/internal/SystemConfigConverterUTest.java` | import 更新 |
| 29 | `test/repository/system/SystemConfigRepositoryITest.java` | `test/systemconfig/internal/SystemConfigRepositoryITest.java` | import 更新 |
| 30 | `test/repository/system/SystemConfigPaginationITest.java` | `test/systemconfig/internal/SystemConfigPaginationITest.java` | import 更新 |
| 31 | `test/entity/system/ConfigKeyDisplayNameUTest.java` | `test/systemconfig/internal/ConfigKeyDisplayNameUTest.java` | import 更新 |
| **新增** | | | |
| 32 | — | `test/systemconfig/internal/SystemConfigFacadeImplETest.java` | 端到端测试 |
| 33 | — | `test/systemconfig/internal/SystemConfigControllerETest.java` | 端到端测试 |

**Model 业务行为**：

```java
// systemconfig/internal/SystemConfig.java
public class SystemConfig {
    // ... 字段

    /** 更新配置值 */
    public void updateValue(ConfigValue newValue) {
        this.configValue = newValue;
        this.updateTime = Instant.now();
    }

    /** 是否可编辑 */
    public boolean isEditable() {
        return this.inputType != InputType.READONLY;
    }
}
```

**代码生成器改造**：

```java
// test/tools/MybatisPlusGenerator.java
Map<String, String> TABLE_MODULE_MAP = Map.of(
    "system_config", "systemconfig",
    "operation_log", "operationlog",
    "user",          "auth"
);

TABLE_MODULE_MAP.forEach((table, module) -> {
    FastAutoGenerator.create(url, username, password)
        .globalConfig(b -> b.outputDir("app/src/main/java"))
        .packageConfig(b -> b
            .parent("org.smm.archetype." + module + ".internal")
            .entity("")       // → module/internal/SystemConfigDO.java
            .mapper(""))      // → module/internal/SystemConfigMapper.java
        .strategyConfig(b -> b.addInclude(table)
            .entityBuilder().suffix("DO")...)
        .execute();
});
```

**验证**：
- systemconfig 模块所有接口功能正常
- 所有测试通过（UTest + ITest + ETest）
- 其他模块（auth、operationlog）仍正常运行
- Spring Modulith 未引入（阶段 3）

**风险**：🟡 中（涉及 systemconfig 模块的全面重组 + 代码生成器改造）

---

### 阶段 3：全量推广 + Spring Modulith 引入

**目标**：将试点模式推广到所有模块，引入 Spring Modulith。

**Spring Modulith 引入说明**：

- 阶段 1-2 不引入 Spring Modulith 依赖，仅按目标包结构组织
- 阶段 3 正式引入：
  - `spring-modulith-starter-test`（test scope）— `ApplicationModules.verify()` 边界验证
  - `spring-modulith-starter-jdbc`（compile scope）— 事件持久化 + `@ApplicationModuleListener`
- 为每个模块添加 `package-info.java` 声明 `@ApplicationModule`
- 激活 `ApplicationModules.verify()` 验证模块边界

**事件系统说明**：

- `spring-modulith-starter-jdbc` 自动管理 `event_publication` 表，实现 at-least-once 事件投递
- 跨模块通信优先使用 `ApplicationEventPublisher` + `@ApplicationModuleListener`，替代直接 Facade 调用
- 示例：systemconfig 配置变更后发布事件，operationlog 模块监听并记录
- 模块内部仍走直接调用（Service → Repository），事件主要用于跨模块解耦

**模块改造顺序**：

| 顺序 | 模块 | 复杂度 | 注意事项 |
|------|------|--------|---------|
| 1 | auth | 中 | LoginFacade → AuthFacade，从 `service/auth/` 移到 `auth/` 根包 |
| 2 | operationlog | 低 | 纯查询模块，无复杂业务逻辑 |
| 3 | shared 横切关注点 | 中 | AOP 切面归入 `shared/internal/aspect/` |
| 4 | Spring Modulith 声明 | — | 所有模块添加 `package-info.java` |
| 5 | 清理 | — | 删除空的旧包（controller/facade/service/repository/entity/generated） |
| 6 | Entity → Model 全局重命名 | — | **一次性 Rename**：IDE Refactor 重命名 Converter 方法名 + 注释 + 变量名，跨所有模块统一执行 |

**auth 模块特殊处理**：
- `LoginFacade` → `AuthFacade`，移到 `auth/` 根包（公开 API）
- `LoginFacadeImpl` → `AuthFacadeImpl`，移到 `auth/internal/`
- `User` Model 归入 `auth/internal/`
- `UserRepository` 归入 `auth/internal/`，实现归入 `auth/internal/`
- `UserDO`、`UserMapper` 从 `generated/` 移到 `auth/internal/`

**Spring Modulith 模块声明**：

```java
// systemconfig/package-info.java
@ApplicationModule(id = "systemconfig")
package org.smm.archetype.systemconfig;

// auth/package-info.java
@ApplicationModule(id = "auth")
package org.smm.archetype.auth;

// shared/package-info.java
@ApplicationModule(type = Type.OPEN)
package org.smm.archetype.shared;
```

**验证**：
- 所有模块接口功能正常
- 所有测试通过
- `ApplicationModules.verify()` 通过（无循环依赖、无越界访问）
- ArchUnit 所有现有规则通过

**风险**：🟡 中（改动面大，但已有试点模块验证过模式）

---

### 阶段 4：ArchUnit 规则 + 收尾

**目标**：加强架构约束，清理废弃代码。

**新增 ArchUnit 规则**：

| 规则 ID | 检测内容 | 优先级 |
|---------|---------|--------|
| M-05 | 模块 `internal/` 包零 `org.springframework` 依赖（Controller/RepositoryImpl 除外） | MUST |
| M-06 | Repository 接口方法签名不得出现 `IPage`/`Page`（MyBatis-Plus） | MUST |
| M-07 | 模块间不得直接访问其他模块的 `internal/` 包 | MUST |
| M-08 | Facade 接口不得依赖 MyBatis-Plus 类型 | MUST |
| M-09 | 业务模块间通过根包 Facade 接口通信，禁止直接注入其他模块的 internal 类 | MUST |
| M-10 | Model 类不得使用 `@Data` | MUST（已有，范围扩展） |
| T-04 | 含 `@Test` 的文件必须以 UTest/ITest/ETest 结尾 | MUST |
| T-05 | ETest 不得使用 `@Mock` | MUST |

**代码生成器渐进演化**：

| 阶段 | 生成内容 | 说明 |
|------|---------|------|
| 阶段 2~3 | DO + Mapper（2 个文件） | 最小改动，先让模块跑起来 |
| 阶段 4 | DO + Mapper + Model + Repository + RepositoryImpl + Converter + Facade + FacadeImpl + Controller + VO（11 个文件） | 完整骨架，新模块开箱即用 |

**代码生成器最终形态**（阶段 4 达成）：

| 生成内容 | 输出位置 | 说明 |
|---------|---------|------|
| DO 类 | `模块/internal/XxxDO.java` | 持久化对象 |
| Mapper 接口 | `模块/internal/XxxMapper.java` | MyBatis Mapper |
| Model 骨架 | `模块/internal/Xxx.java` | 含基本 getter/setter |
| Repository 接口 | `模块/internal/XxxRepository.java` | 标准 CRUD 方法签名 |
| RepositoryImpl | `模块/internal/XxxRepositoryImpl.java` | 含 DO↔Model + IPage→PageResult 转换 |
| Converter | `模块/internal/XxxConverter.java` | 含 toModel/toDO |
| Service 骨架 | `模块/internal/XxxService.java` | 业务编排骨架 |
| Facade 接口 | `模块/XxxFacade.java` | 公开 API 接口 |
| FacadeImpl | `模块/internal/XxxFacadeImpl.java` | 编排 + Model→VO 转换 |
| Controller | `模块/internal/XxxController.java` | HTTP 入口骨架 |
| VO | `模块/internal/XxxVO.java` | VO record |

**清理废弃代码**：

| 废弃内容 | 说明 |
|---------|------|
| `entity/base/BasePageRequest.java` | 被 `PageQuery` 替代 |
| `entity/base/BaseRequest.java` | traceId 由 OTel 管理 |
| `entity/base/BasePageResult.fromPage(IPage)` 方法 | 不再直接接触 `IPage` |
| 旧包结构下的空目录 | 迁移完成后删除 |
| `generated/` 目录 | DO/Mapper 已移入各模块 |

**验证**：
- 代码生成器可正常生成符合新架构的完整模块骨架
- 所有 ArchUnit 规则通过
- 应用启动正常
- 所有测试通过

**风险**：🟢 低（工具链适配 + 规则收紧，不影响业务逻辑）

---

## 六、测试规范（硬性规则）

### 6.1 测试类型与文件命名

| 测试类型 | 文件后缀 | 覆盖范围 | 硬性要求 |
|---------|---------|---------|---------|
| **单元测试** | `*UTest.java` | 所有方法 | 每个方法至少 1 个 UTest |
| **集成测试** | `*ITest.java` | Facade 层所有 public 方法 | 每个 Facade public 方法至少 1 个 ITest |
| **端到端测试** | `*ETest.java` | Controller 层所有 public 方法 | 每个 Controller public 方法至少 1 个 ETest |

### 6.2 覆盖率要求

| 测试类型 | 行覆盖率 | 分支覆盖率 |
|---------|---------|-----------|
| 单元测试（UTest） | ≥ 95% | ≥ 90% |
| 集成测试（ITest） | ≥ 90% | ≥ 85% |
| 端到端测试（ETest） | 无硬性指标 | 无硬性指标 |

### 6.3 端到端测试（ETest）构建流程

1. **分析代码**：梳理所有存量或增量的业务流程（git diff），分析出每个业务流程下所有的场景与子场景（AI 辅助分析）
2. **编排设计**：设计 Adapter 层调用，确保覆盖此业务流程的所有场景与子场景
3. **启动环境**：启动测试环境应用
4. **执行脚本**：调用编排脚本
5. **分析覆盖**：分析分支覆盖率以及分支 assert 条件，确保场景与子场景都覆盖到

### 6.4 测试包组织

采用**源码镜像**方式：测试文件的包路径与源码完全一致。

```
src/main/java/                          src/test/java/
systemconfig/                           systemconfig/
└── internal/                           └── internal/
    ├── SystemConfigService.java            ├── SystemConfigServiceUTest.java
    ├── SystemConfigFacadeImpl.java         ├── SystemConfigFacadeImplUTest.java
    │                                       ├── SystemConfigFacadeImplITest.java
    │                                       └── SystemConfigFacadeImplETest.java
    ├── SystemConfigController.java         ├── SystemConfigControllerITest.java
    │                                       └── SystemConfigControllerETest.java
    ├── SystemConfigConverter.java          └── SystemConfigConverterUTest.java
    └── SystemConfigRepositoryImpl.java     └── SystemConfigRepositoryImplITest.java
```

---

## 七、风险与缓解

| 风险 | 缓解措施 |
|------|---------|
| Spring Modulith 与 MyBatis-Plus 兼容问题 | 阶段 3 引入；Modulith 不绑定 JPA，底层使用 JDBC |
| 大规模重命名导致 git 历史断裂 | 使用 IDE Refactor（非手动重命名），保留 rename commit |
| 改造期间新旧结构共存导致混乱 | 阶段 2 完成前其他模块不动；每个阶段独立验证 |
| ArchUnit 规则过于严格阻碍开发 | 新规则先设为 SHOULD，稳定后升级为 MUST |
| 代码生成器多次执行性能 | 仅开发时手动运行，不影响 CI |
| `internal/` 包下类过多 | 本项目模块小（15-20 类），可接受；大型模块可按 Petclinic 方式分子包 |

---

## 八、技术依赖

| 技术 | 版本 | 用途 | 引入阶段 |
|------|------|------|---------|
| Spring Boot | 4.x | 框架（已使用） | — |
| Spring Modulith | 2.0.6+ | 模块边界验证 | 阶段 3 |
| MyBatis-Plus | 3.5.x | ORM（已使用） | — |
| ArchUnit | 已使用 | 编码规范验证（增强） | 阶段 4 |
| JaCoCo | 0.8.x | 覆盖率采集（已使用） | — |
| Java | 25 | 语言（已使用） | — |

---

## 九、术语表

| 术语 | 含义 |
|------|------|
| Model | 领域模型，承载业务行为和状态（原 Entity） |
| DO | 数据对象，映射数据库表（Data Object） |
| VO | 视图对象，面向前端展示（View Object） |
| Command | 写操作命令，Facade 层的输入 |
| PageQuery | 分页查询参数（1-based） |
| PageResult | 分页查询结果（框架无关） |
| Facade | 模块公开 API，唯一对外服务入口 |
| `internal/` | 模块内部包，其他模块不可访问 |
| UTest | 单元测试，纯 Mockito 环境 |
| ITest | 集成测试，Spring 上下文 |
| ETest | 端到端测试，业务流程验证 |
