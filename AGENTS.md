# Web Quick Start Light — 项目指南

> **职责**: 为 AI Agent 和人类开发者提供项目导航、编码规则和文档索引
>
> 本文件是进入项目的第一入口。AI Agent 在修改代码前应先阅读 [编码规则](#编码规则分层披露) 和对应领域的文档；人类开发者可通过 [文档索引](#文档索引) 快速定位所需信息。

## 项目概览

web-quick-start-light（`org.smm.archetype`）是一个基于 **Java 25 + Spring Boot 4.x** 的轻量级 Web 快速启动脚手架，采用 **DDD-lite + Template Method** 架构风格。项目通过严格的四层分层（Controller → Facade → Service → Repository）保证代码职责清晰，通过 6 个独立组件模块（auth/cache/email/oss/search/sms）实现基础设施的可插拔扩展。全项目 196 个源文件、74 个测试文件、420 个测试用例，零循环依赖，由 ArchUnit 测试守护分层边界。默认使用 SQLite 嵌入式数据库，`mvn spring-boot:run` 即可运行，无需安装任何外部依赖。

## 技术栈

| 技术 | 版本 | 用途 |
|------|:----:|------|
| Java | 25 | ScopedValue、虚拟线程、record |
| Spring Boot | 4.0.2 | Web MVC + AutoConfiguration |
| MyBatis-Plus | 3.5.16 | ORM + 代码生成 + 分页 + 逻辑删除 |
| SQLite | 3.51.3 | 嵌入式数据库（零运维） |
| Sa-Token | 1.45.0 | 轻量级认证框架 |
| Caffeine | — | 本地高性能缓存 |
| Bucket4j | 8.17.0 | 令牌桶限流 |
| FastJSON2 | 2.0.61 | JSON 序列化 |
| Kryo | 5.6.2 | 高性能二进制序列化 |
| SpringDoc OpenAPI | 3.0.3 | Swagger UI / API 文档 |
| MapStruct | 1.6.3 | Entity ↔ DO 对象转换 |
| ArchUnit | 1.4.1 | 架构规则自动化守护 |
| JaCoCo | 0.8.14 | 代码覆盖率 |
| Hutool | 5.8.44 | 通用工具库 |
| Serial GC | — | ≤1G 堆最优 GC 策略 |

## 项目结构

```
web-quick-start-light/
├── pom.xml                    # 父 POM（dependencyManagement + 构建插件）
├── common/                    # 公共模块：异常体系 + 错误码 + 基础类型
├── components/                # 基础设施组件（6 个独立 Maven 模块）
│   ├── component-auth/        #   认证组件（Sa-Token / NoOp）
│   ├── component-cache/       #   缓存组件（Caffeine）
│   ├── component-email/       #   邮件组件（NoOp 占位）
│   ├── component-oss/         #   对象存储组件（本地文件系统）
│   ├── component-search/      #   搜索组件（内存实现）
│   └── component-sms/         #   短信组件（NoOp 占位）
├── app/                       # 应用主模块
│   └── src/main/java/.../
│       ├── controller/        #   HTTP 入口层
│       │   └── global/        #     全局过滤器 + 异常处理
│       ├── facade/            #   编排门面层
│       ├── service/           #   业务逻辑层
│       ├── repository/        #   数据访问层
│       ├── entity/            #   领域实体（纯 POJO）
│       ├── config/            #   Spring 配置类
│       ├── shared/            #   共享横切关注点
│       │   ├── aspect/        #     AOP 切面（幂等/日志/限流）
│       │   └── util/          #     工具类 + BizContext
│       └── generated/         #   MyBatis-Plus 代码生成（禁止手动修改）
├── scripts/                   # 运维脚本（start.sh / stop.sh）
├── doc-sys/                   # 文档体系（三轨 22 份文档）
└── openspec/                  # OpenSpec 变更管理
```

## 编码规则（分层披露）

> **最高优先级约束** — 以下规则由 ArchUnit 自动化测试守护，CI 必过。违反任何规则将导致构建失败。

### 核心规则

| # | 规则 | 严重级别 |
|---|------|:-------:|
| 1 | Controller → Facade → Service → Repository → Mapper，**单向依赖** | 🔴 |
| 2 | Controller **不得**直接调用 Service（Login 例外），必须经过 Facade | 🔴 |
| 3 | Controller / Facade **不得**直接使用 Mapper | 🔴 |
| 4 | Entity **不得**依赖 Spring Framework（保持纯 POJO） | 🔴 |
| 5 | 所有异常继承 `BaseException`，使用 `ErrorCode` 枚举 | 🔴 |
| 6 | Entity 使用 `@Getter/@Setter`，**禁止** `@Data` | 🔴 |
| 7 | 配置注入使用 Properties 类 + 构造器注入，**禁止** `@Value` | 🔴 |
| 8 | `generated` 包禁止手动修改，新增 Entity/Mapper 通过生成器 | 🔴 |

### 分层架构规则

本项目采用 **Controller → Facade → Service → Repository** 四层架构：

- **Controller 层** — HTTP 协议适配（参数校验、响应包装），仅依赖 Facade 和 Entity
- **Facade 层** — 用例编排（一个 API 端点对应一个 Facade 方法），不包含业务规则
- **Service 层** — 单一领域的业务逻辑，可被多个 Facade 复用
- **Repository 层** — 数据访问，通过 Converter 实现 Entity ↔ DO 转换

```
允许: Controller → Facade → Service → Repository → Mapper
      各层均可依赖 Entity（纯 POJO）

禁止: 任何反向依赖（如 Service → Controller）
      Entity → Spring Framework
      Controller → Service（Login 例外）
```

### 组件设计规则

所有 6 个组件模块统一采用 **Template Method + Strategy** 模式：
- 公开方法标记 `final`，封装参数校验 + 日志 + 异常转换
- 子类通过 `protected abstract do*()` 实现核心逻辑
- 通过 `@ConditionalOnMissingBean` 支持策略替换
- 组件异常统一转换为 `ClientException(CommonErrorCode.XXX_FAILED)`

### 命名规范

| 类型 | 命名 | 示例 |
|------|------|------|
| Controller | `{Domain}Controller` | `SystemConfigController` |
| Facade | `{Domain}Facade` / `{Domain}FacadeImpl` | `SystemConfigFacadeImpl` |
| Service | `{Domain}Service` | `SystemConfigService` |
| Repository | `{Domain}Repository` / `{Domain}RepositoryImpl` | `UserRepositoryImpl` |
| Entity | `{Domain}` | `SystemConfig` |
| DO（生成） | `{Domain}DO` | `SystemConfigDO` |
| 测试 | `{Target}UTest` / `{Target}ITest` | `SystemConfigFacadeImplUTest` |

## 文档索引

> 全部 **22 份**文档位于 `doc-sys/` 目录下，按三轨体系组织。

### Architecture — Intent 轨道（系统级设计文档）

| 文档 | 路径 | 引用强度 | 说明 |
|------|------|:-------:|------|
| 架构总览 | `doc-sys/architecture/arch-overview.md` | 🔴 必读 | 分层模型、模块拓扑、技术栈、设计模式 |
| 架构决策记录 | `doc-sys/architecture/arch-decisions.md` | 🔴 必读 | 10 个 ADR（ADR-001 ~ ADR-010） |

### Business — Contract 轨道（业务领域文档）

| 文档 | 路径 | 引用强度 | 说明 |
|------|------|:-------:|------|
| 基础类型 | `doc-sys/business/base-types.md` | 🔴 必读 | BaseResult / BasePageResult / BaseRequest / BaseDO |
| 异常体系 | `doc-sys/business/exception-system.md` | 🔴 必读 | 三级异常分类 + 错误码分段 + i18n |
| REST API 参考 | `doc-sys/business/api-reference.md` | 🟡 推荐 | 全部 18 个端点的完整列表 |
| 用户领域 | `doc-sys/business/domain-user.md` | 🟡 推荐 | LoginController → LoginFacade → UserRepository |
| 系统配置领域 | `doc-sys/business/domain-system-config.md` | 🟡 推荐 | DDD 值对象 + 聚合根 + CRUD 6 端点 |
| 操作日志领域 | `doc-sys/business/domain-operation-log.md` | 🟡 推荐 | 分页查询 + `@BusinessLog` AOP 写入 |

### Components — Contract 轨道（组件文档）

| 文档 | 路径 | 引用强度 | 说明 |
|------|------|:-------:|------|
| 组件设计模式 | `doc-sys/components/component-pattern.md` | 🔴 必读 | Template Method 统一骨架规范 |
| 认证组件 | `doc-sys/components/component-auth.md` | 🟡 推荐 | Sa-Token 集成 + WebMvc 拦截器 |
| 缓存组件 | `doc-sys/components/component-cache.md` | 🟡 推荐 | Caffeine + 独立 TTL |
| 对象存储组件 | `doc-sys/components/component-oss.md` | 🟢 参考 | 本地文件系统 + 7 个操作 |
| 搜索组件 | `doc-sys/components/component-search.md` | 🟢 参考 | 内存搜索引擎 + 15 个操作 |
| 邮件与短信组件 | `doc-sys/components/component-messaging.md` | 🟢 参考 | Email + SMS NoOp 实现 |

### Infrastructure — Intent + Contract 轨道（基础设施文档）

| 文档 | 路径 | 引用强度 | 说明 |
|------|------|:-------:|------|
| 上下文传播机制 | `doc-sys/infrastructure/context-propagation.md` | 🔴 必读 | BizContext + ScopedValue + OTel Baggage |
| AOP 切面 | `doc-sys/infrastructure/aop-aspects.md` | 🟡 推荐 | 幂等 / 日志 / 限流三大切面 |
| 日志体系 | `doc-sys/infrastructure/logging-system.md` | 🟡 推荐 | logback-spring.xml + 采样 + 脱敏 |

### Guides — Constraint 轨道（规范与指南）

| 文档 | 路径 | 引用强度 | 说明 |
|------|------|:-------:|------|
| 编码规范 | `doc-sys/guides/coding-standards.md` | 🔴 必读 | 10 条规则 + ArchUnit 守护 + 违规示例 |
| 配置参考 | `doc-sys/guides/configuration-reference.md` | 🟡 推荐 | 全部 YAML 配置项与 Properties |
| 数据库 Schema | `doc-sys/guides/database.md` | 🟡 推荐 | SQLite DDL + 种子数据 + 代码生成器 |
| 测试指南 | `doc-sys/guides/testing-guide.md` | 🟡 推荐 | 测试分层 + 命名 + 标签 + 基类 |
| 部署与运维 | `doc-sys/guides/deployment.md` | 🟢 参考 | start.sh / stop.sh / JVM 参数 |

### 引用强度说明

- 🔴 **必读**：AI Agent 在修改对应领域代码前必须阅读
- 🟡 **推荐**：理解上下文、编写新功能时建议阅读
- 🟢 **参考**：按需查阅，不要求预先阅读

## 维护治理（Maintenance Governance）

| 轨道 | 维护者 | 触发条件 | 验证方式 |
|------|--------|---------|---------|
| **Intent** | Frozen（创建后冻结） | 重大设计变更（需人工审核后更新） | 人工审核 + 新 ADR 记录 |
| **Contract** | AI（跟随代码） | 代码变更自动触发同步 | CI 自动校验（doc-sys-sync L1/L2） |
| **Constraint** | Human（驱动代码） | 规则变更优先更新文档 | ArchUnit 守护测试 |

### 文档与代码的关系

```
Intent（设计意图）──── 设计阶段产出，创建后冻结
       │
       ▼
Constraint（约束规则）── 人类编写，驱动代码生成
       │
       ▼
Contract（代码契约）──── AI 从代码中提取，跟随代码同步
```

## Auto-Generated Code（排除清单）

以下代码由工具自动生成，文档系统**不处理**：

- `app/src/main/java/org/smm/archetype/generated/` — MyBatis-Plus 代码生成器产物（`*DO`、`*Mapper`）
- `doc-sys/.generation/` — 文档生成过程的中间分析文件

## 快速开始

```bash
# 1. 克隆并构建
git clone <repo-url>
cd web-quick-start-light
mvn clean install -DskipTests

# 2. 启动应用（默认 SQLite，零配置）
mvn spring-boot:run -pl app

# 3. 访问 API 文档
open http://localhost:8080/swagger-ui.html

# 4. 运行全部测试（含 ArchUnit 架构守护）
mvn test
```
