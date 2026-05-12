# 模块结构

> 🟢 Contract 轨 — 100% 反映代码现状

## 📋 目录

- [概述](#概述)
- [目录树结构](#目录树结构)
- [Maven 依赖关系图](#maven-依赖关系图)
- [四层架构](#四层架构)
- [层间依赖规则](#层间依赖规则)
- [ArchUnit 守护规则](#archunit-守护规则)
- [相关文档](#相关文档)
- [变更历史](#变更历史)

## 概述

Maven 多模块结构和四层架构说明。项目采用 Maven POM 聚合模式，分为根 POM、common（公共模块）、components（组件聚合）、app（主应用）四个层级。app 模块内部遵循 Controller → Facade → Service → Repository 的严格四层架构，层间依赖单向流动，由 ArchUnit 在测试阶段守护约束。

## 目录树结构

```
web-quick-start-light/                     (根 POM, packaging=pom)
├── common/                                (异常体系: ErrorCode, CommonErrorCode, BaseException...)
├── components/                               (parent POM, packaging=pom — 组件接入层)
│   ├── component-cache/                      (Caffeine 本地缓存, 10 方法, Template Method)
│   ├── component-oss/                        (本地对象存储, NIO + 日期分层, 7 方法, Template Method)
│   ├── component-email/                      (Jakarta Mail 邮件, 3 方法, NoOp 默认实现, 条件装配)
│   ├── component-sms/                        (短信, 3 方法, NoOp 默认实现, 条件装配)
│   ├── component-search/                     (内存搜索, ConcurrentHashMap, 15 方法, 条件装配)
│   └── component-auth/                       (认证组件, Sa-Token/NoOp, Template Method, 条件装配)
└── app/                                   (主应用, packaging=jar, 依赖 common + 组件 component-*)
    └── shared/                            (跨层共享基础设施: 限流/幂等/操作日志/日志工具)
```

## Maven 依赖关系图

```mermaid
graph TD
    ROOT["web-quick-start-light<br/>根 POM (pom)"]
    COMMON["common<br/>异常体系"]
    COMPONENTS["components<br/>聚合 POM (pom) — 组件接入层"]
    APP["app<br/>Spring Boot 主应用 (jar)"]

    ROOT --> COMMON
    ROOT --> COMPONENTS
    ROOT --> APP

    COMPONENTS --> CACHE["component-cache"]
    COMPONENTS --> OSS["component-oss"]
    COMPONENTS --> EMAIL["component-email"]
    COMPONENTS --> SMS["component-sms"]
    COMPONENTS --> SEARCH["component-search"]
    COMPONENTS --> AUTH["component-auth"]

    CACHE --> COMMON
    OSS --> COMMON
    EMAIL --> COMMON
    SMS --> COMMON
    SEARCH --> COMMON
    AUTH --> COMMON

    APP --> COMMON
    APP --> CACHE
    APP --> OSS
    APP --> EMAIL
    APP --> SMS
    APP --> SEARCH
    APP --> AUTH
```

> 箭头表示 `depends on`（A → B 表示 A 的 pom.xml 中声明了对 B 的依赖）。所有 component-* 模块仅依赖 common，不互相依赖。components 模块只包含组件接入（Template
> Method 模式），应用层横切关注点（限流/幂等/操作日志/日志基础设施）集成在 app 模块的 `shared/` 包下。

## 四层架构

app 模块内部采用严格的四层架构，依赖方向单向向下流动：

```mermaid
flowchart TD
    subgraph Controller["Controller 层 — REST API 入口"]
        C1["接收 HTTP 请求<br/>参数校验（Bean Validation）"]
        C2["调用 Facade<br/>传递 Request / Command"]
        C3["返回 Response / BaseResult"]
    end

    subgraph Facade["Facade 层 — 门面 / 编排"]
        F1["Entity → VO 转换（手写 Converter）"]
        F2["调用 Service<br/>传递 Command / Query"]
        F3["业务编排与聚合"]
    end

    subgraph Service["Service 层 — 业务逻辑"]
        S1["核心业务规则<br/>事务管理"]
        S2["调用 Repository<br/>传递 Entity / Query"]
        S3["返回 Entity"]
    end

    subgraph Repository["Repository 层 — 数据访问"]
        R1["MyBatis-Plus Mapper 操作"]
        R2["Entity ↔ DO 转换"]
        R3["SQL 执行"]
    end

    DB[("SQLite 数据库")]

    Controller -->|Request / Command| Facade
    Facade -->|Command / Query| Service
    Service -->|Entity / Query| Repository
    Repository -->|DO / SQL| DB

    style Controller fill:#e3f2fd,stroke:#1976d2
    style Facade fill:#f3e5f5,stroke:#7b1fa2
    style Service fill:#e8f5e9,stroke:#388e3c
    style Repository fill:#fff3e0,stroke:#f57c00
```

### 各层职责

| 层级 | 包路径 | 职责 | 数据形态 |
|------|--------|------|---------|
| Controller | `controller` | 接收 HTTP 请求、参数校验、调用 Facade | Request → Command |
| Facade | `facade` | Entity→VO 转换、业务编排、聚合多个 Service | Command → Entity → VO |
| Service | `service` | 核心业务逻辑、事务管理 | Command → Entity |
| Repository | `repository` | 数据访问、MyBatis-Plus Mapper、Entity↔DO 转换 | Entity → DO |

### app 内部包组织

app 模块内部只有业务模块和 shared 两个顶层概念。

```
org.smm.archetype/
│
│  ═══ 业务模块（Spring Modulith 模式：根包 Facade + internal/ 实现）═══
│
├── auth/                        ← 认证模块
│   ├── AuthFacade.java          ← 公开 API（接口）
│   └── internal/
│       ├── AuthFacadeImpl.java
│       ├── User.java            ← 领域模型
│       ├── UserDO.java
│       ├── UserMapper.java
│       ├── UserRepository.java
│       ├── UserRepositoryImpl.java
│       ├── UserConverter.java
│       ├── LoginController.java
│       ├── LoginRequest.java
│       ├── LoginVO.java
│       └── AuthConfigure.java   ← 模块专属 Bean 注册
├── operationlog/                ← 操作日志模块
│   ├── OperationLogFacade.java
│   └── internal/
│       └── ...
├── systemconfig/                ← 系统配置模块
│   ├── SystemConfigFacade.java
│   └── internal/
│       └── ...
│
│  ═══ 公共基础设施（按功能域自包含）═══
│
├── shared/                      ← 公共基础设施
│   ├── ratelimit/               ← 限流（注解 + 切面 + 配置 + 属性）
│   ├── idempotent/              ← 幂等（注解 + 切面 + Key 解析 + 配置）
│   ├── logging/                 ← 日志（配置 + 属性 + 过滤器 + 拦截器 + 工具）
│   ├── operationlog/            ← 操作日志切面（注解在 common 模块）
│   ├── context/                 ← 业务上下文（BizContext）
│   ├── dal/                     ← 数据访问基础设施（BaseDO + TypeHandler + MetaObjectHandler）
│   ├── generated/               ← 代码生成器
│   ├── mybatis/                 ← MyBatis-Plus 配置
│   ├── threadpool/              ← 线程池配置
│   ├── web/                     ← Web 层基础设施（配置 + 过滤器 + 异常处理 + 测试端点）
│   ├── pagination/              ← 分页模型
│   ├── result/                  ← 响应模型
│   └── util/                    ← 序列化 + Spring 工具
│
└── WebStartLightApplication.java
```

#### 分包原则

| 区域     | 组织方式                           | 说明                             |
|--------|--------------------------------|--------------------------------|
| 业务模块   | Spring Modulith 模式（根包 Facade + internal/） | 模块间通过 Facade 接口通信，internal/ 不跨模块访问 |
| 公共基础设施 | 按功能域自包含                        | 每个功能域包含注解+切面+配置+属性，零跨包依赖       |

#### shared 层定位

`shared` 放置被多个业务模块共享使用的横切关注点和基础设施。每个功能域是自包含的独立包：

| 功能域                    | 职责          | 包含内容                          |
|------------------------|-------------|-------------------------------|
| `shared/ratelimit/`    | 限流          | @RateLimit + RateLimitAspect + BucketFactory + SpelKeyResolver + LimitFallback + RatelimitConfigure + RateLimitProperties |
| `shared/idempotent/`   | 幂等          | @Idempotent + IdempotentAspect + IdempotentKeyResolver + IdempotentConfigure |
| `shared/logging/`      | 日志          | LoggingConfigure + LoggingProperties + SamplingTurboFilter + SlowQueryInterceptor + LogMarkers + SensitiveLogUtils |
| `shared/operationlog/` | 操作日志        | LogAspect（@BusinessLog 注解在 common 模块） |
| `shared/context/`      | 上下文传播       | BizContext（基于 ScopedValue） |
| `shared/dal/`          | 数据访问基础设施    | BaseDO + InstantTypeHandler + MyMetaObjectHandler |
| `shared/generated/`    | 代码生成器       | MybatisPlusGenerator（跨模块工具，通过 --module 参数输出） |
| `shared/mybatis/`      | MyBatis-Plus 配置 | MybatisPlusConfigure |
| `shared/threadpool/`   | 线程池配置       | ThreadPoolConfigure + ThreadPoolProperties |
| `shared/web/`          | Web 基础设施     | WebConfigure + ContextFillFilter + WebExceptionAdvise + TestController + AppInfoProperties + IpUtils |
| `shared/pagination/`   | 分页模型        | PageQuery + PageResult |
| `shared/result/`       | 响应模型        | BaseResult + BasePageResult |
| `shared/util/`         | 序列化/Spring 工具 | KryoSerializer + SpringContextUtils |

> **注意**：每个功能域是自包含的——限流的所有组件都在 `shared/ratelimit/` 下，日志的所有组件都在 `shared/logging/` 下。不需要跨功能域包引用。

#### Configure 类规范

| 类型 | 位置 | 示例 |
|------|------|------|
| 模块专属配置 | 各模块 `internal/` 下 | `auth/internal/AuthConfigure.java` |
| 公共基础设施配置 | shared 对应功能域包 | `shared/ratelimit/RatelimitConfigure.java`、`shared/logging/LoggingConfigure.java` |

## 层间依赖规则

### 允许的依赖

| 依赖关系 | 说明 |
|---------|------|
| Controller → Facade | 通过门面层隔离 API 与业务逻辑 |
| Facade → Service | 门面层调用服务层获取业务数据 |
| Service → Repository | 服务层调用仓储层访问数据 |
| Facade 可转换 Entity → VO | 手写 @Component Converter 转换 |
| Repository 可转换 DO → Entity | 数据对象与业务对象转换 |

### 禁止的依赖

| 禁止关系 | 原因 | ArchUnit 规则 |
|---------|------|--------------|
| Controller → Repository | 跳过 Facade/Service 层 | `controllerShouldOnlyDependOnServiceLayer` |
| Controller → Service | 跨层调用（Login 除外） | `controllerShouldNotDependOnServiceDirectly` |
| Facade → Repository | 跳过 Service 层 | `facadeShouldNotDependOnRepository` |
| Service → Controller | 依赖倒置 | `serviceShouldNotDependOnControllerLayer` |
| Repository → Service / Controller | 依赖倒置 | `repositoryShouldNotDependOnServiceOrControllerLayer` |
| Entity → Spring Framework | 领域模型纯净性 | `entityShouldNotDependOnSpringFramework` |

## ArchUnit 守护规则

项目通过 `ArchitectureComplianceUTest`（继承 `UnitTestBase`）在每次构建时自动验证四层架构约束：

| 规则方法 | 守护的约束 | 说明 |
|---------|-----------|------|
| `controllerShouldOnlyDependOnServiceLayer` | Controller 禁止依赖 `..repository.mapper..` | 防止直接操作 Mapper |
| `controllerShouldNotDependOnServiceDirectly` | Controller 禁止依赖 `..service..`（Login 除外） | 强制通过 Facade 中转 |
| `facadeShouldNotDependOnRepository` | Facade 禁止依赖 `..repository..` | 防止跳过 Service 层 |
| `serviceShouldNotDependOnControllerLayer` | Service 禁止依赖 `..controller..` | 防止依赖倒置 |
| `repositoryShouldNotDependOnServiceOrControllerLayer` | Repository 禁止依赖 `..service..` 和 `..controller..` | 保持数据层独立 |
| `entityShouldNotDependOnSpringFramework` | Entity 禁止依赖 `org.springframework..` | 保持领域模型纯净 |

> 测试文件：`app/src/test/java/org/smm/archetype/support/basic/ArchitectureComplianceUTest.java`

## 设计考量

### 为什么选择多模块而非单模块

**驱动力**：骨架项目需要在保持轻量级的同时，提供清晰的模块边界和独立的依赖管理。

**备选方案**：

| 方案 | 优点 | 缺点 |
|------|------|------|
| 单模块 | 结构简单，IDE 导入快 | 所有代码耦合在一起，无法独立复用组件模块；修改一个组件可能导致全量重新编译 |
| 多模块（当前选择） | 模块间物理隔离，组件可独立引用；Maven 依赖传递清晰 | IDE 导入略慢；需要维护 parent POM 依赖版本 |

**选择多模块的理由**：

1. **组件可独立复用**：其他项目可以只引入 `component-cache` 而不引入整个骨架，Maven 依赖传递自动处理
2. **编译隔离**：修改 `component-sms` 不会触发 `app` 模块重新编译，提升开发效率
3. **依赖范围控制**：`common` 模块不依赖 Spring，确保异常体系等基础能力可以在任何环境下使用
4. **团队协作友好**：不同开发者可以独立修改不同组件模块，减少合并冲突

### 为什么 common 模块不依赖 Spring Framework

**驱动力**：`common` 模块承载异常体系（`ErrorCode`、`BaseException`、`BizException` 等），属于纯粹的 Java 领域抽象，不应与特定框架绑定。

**设计理由**：

1. **框架无关性**：异常类在任何 Java 环境下都可用，包括非 Spring 环境（如消息消费者、定时任务脚本）
2. **依赖方向正确**：`common` 是最底层模块，如果它依赖 Spring，会导致所有上层模块被迫接受 Spring 的传递依赖
3. **Entity 纯净性**：ArchUnit 守护 `Entity → Spring Framework` 的禁止依赖规则，确保领域模型不与框架耦合
4. **测试简单**：`common` 模块的单元测试不需要 Spring 上下文，执行速度更快

## 相关文档

| 文档 | 说明 |
|------|------|
| [系统全景](system-overview.md) | C4 架构图与技术栈概要 |
| [请求流转](request-lifecycle.md) | HTTP 请求完整处理链路 |
| [设计模式](design-patterns.md) | Template Method 与条件装配 |
| [Java 编码规范](../conventions/java-conventions.md) | 四层架构约束与编码规范 |

## 变更历史

| 日期         | 变更内容                                                                                                              |
|------------|-------------------------------------------------------------------------------------------------------------------|
| 2026-04-14 | 初始创建                                                                                                              |
| 2026-04-14 | 新增「app 内部包组织」章节；components 模块精简为纯中间件接入层（移除 component-log/component-ratelimit/component-idempotent）；横切关注点纳入 app 模块 `shared/` 包 |
| 2026-05-11 | `entity/api/` → `entity/base/`；`shared/util/context/` 修正为仅含 BizContext；中间件→组件措辞修正；MapStruct→手写 Converter |
| 2026-04-15 | `shared/logging/` 合并到 `shared/util/logging/`，统一 shared 下 aspect/util 两个维度 |
