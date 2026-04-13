## Why

当前脚手架（web-quick-start-light）具备优秀的工程化基础（ArchUnit 架构守护、生产级日志、Template Method 客户端模块、1GB 内存优化），但在"可直接用于生产项目"的能力上存在明显缺口：无认证/授权、无 API 版本控制、无限流/幂等防护、无国际化、无操作日志持久化与展示、无分页查询示例。同时，当前三层架构（Controller → Service → Repository）在业务复杂度上升时 Service 层容易膨胀，需要引入 Facade 薄层解耦。本次改造一次性补齐这些基础设施级能力，使脚手架从"精工细作的教学级模板"升级为"开箱即用的生产级脚手架"。

## What Changes

### Phase 0 — 基础设施（独立能力）

- **API 版本控制**：使用 Spring Boot 4 原生 `spring.mvc.apiversion` 能力，Header 方式（`API-Version`），为所有 API 添加统一版本控制
- **统一 API 前缀**：通过 `WebConfigure` 配置 `/api` 统一前缀，与版本控制协同工作
- **限流/熔断**：引入 Bucket4j + Caffeine，设计 `@RateLimit` 注解，支持在任意方法上标注，参数可配置（桶容量、补充速率、按 Key 策略），具备降级与熔断机制
- **幂等/重复提交防护**：设计 `@Idempotent` 注解，基于内置 Caffeine 缓存，不仅限于 HTTP 接口，支持根据入参的某个字段做幂等，幂等超时时间与字段名称可配置（默认值 + 注解标记）
- **操作日志展示**：扩展 `@BusinessLog` 注解（增加 module/operationType/samplingRate），新增操作日志表 + 异步写入 + 分页查询 API，支持采集率配置

### Phase 1 — 架构改造

- **四层架构（Facade）**：在 Controller 和 Service 之间插入 Facade 薄层，Controller 依赖 Facade，Facade 编排 Service。Facade 层包含接口定义和 DTO（含 VO），Service 层返回 Entity，不再关心 VO 组装
- **认证/授权（Sa-Token）**：引入 `sa-token-spring-boot4-starter:1.45.0`，做成 `client-auth` 模块（Template Method + 条件装配 + NoOp 默认），仅做登录认证（`StpUtil.checkLogin()`），纯内存会话，改造 `ContextFillFilter` 从 Sa-Token 获取真实 userId

### Phase 2 — 业务增强

- **国际化 i18n**：改造 `ErrorCode` 接口和 `WebExceptionAdvise`，引入 `MessageSource`，支持 `Accept-Language` Header，默认中文，Bean Validation 消息国际化
- **分页查询示例**：以操作日志和系统配置为例，展示完整分页查询链路（Controller → Facade → Service → Repository），利用已有 `BasePageResult.fromPage(IPage)`

### Phase 3 — 文档对齐

- **全面文档对齐**：所有改造完成后，系统性地更新 AGENTS.md、ARCHETYPE_README.md、docs/ 目录下所有文档，确保文档与代码完全一致

## Capabilities

### New Capabilities

- `api-versioning`: Spring Boot 4 原生 API 版本控制 + 统一 API 前缀（`/api`）
- `rate-limiting`: 基于 Bucket4j + Caffeine 的 `@RateLimit` 注解式限流，支持按 Key/按方法/按 IP 多维度，可配置降级策略
- `idempotent-protection`: 基于 Caffeine 缓存的 `@Idempotent` 通用幂等注解，支持字段级 Key 提取、可配置超时
- `operation-log`: 操作日志持久化（扩展 @BusinessLog + DB 表 + 异步写入 + 采集率 + 分页查询 API）
- `facade-layer`: 四层架构 Facade 薄层（Controller → Facade → Service → Repository），Facade 包含接口和 DTO/VO
- `auth`: Sa-Token 登录认证（client-auth 模块，Template Method + NoOp 默认，纯内存会话）
- `i18n`: 国际化支持（MessageSource + Accept-Language + ErrorCode 改造 + Bean Validation 国际化）
- `pagination`: 分页查询完整示例（操作日志 + 系统配置分页，BasePageResult 实战）

### Modified Capabilities

- `agents-md-and-docs`: 更新 AGENTS.md 和所有文档以反映四层架构、新增模块、新增依赖
- `system-config`: 系统配置模块迁移到四层架构（增加 Facade 层），增加分页查询端点
- `logging-enhancement`: 扩展 @BusinessLog 注解属性（module/operationType/samplingRate），增加 DB 持久化能力
- `client-modules-replacement`: 新增 client-auth、client-ratelimit、client-idempotent 三个客户端模块

## Impact

### 代码影响

- **BREAKING**: 三层架构改为四层架构，Controller 从直接调用 Service 改为调用 Facade
- **BREAKING**: API 端点统一增加 `/api` 前缀（如 `/api/system/configs`）
- **BREAKING**: `SystemConfigVO`/`ConfigGroupVO`/`UpdateConfigCommand` 从 `service.system` 包移到 `facade.system` 包
- **BREAKING**: `ErrorCode.message()` 语义变更（返回 messageKey 而非中文文案，或新增 messageKey 方法）
- **NEW**: 新增 `clients/client-auth`、`clients/client-ratelimit`、`clients/client-idempotent` 模块
- **NEW**: 新增 `operation_log` 数据库表
- **MODIFY**: `ContextFillFilter` 从 Sa-Token 获取 userId（替代硬编码 "SYSTEM"）
- **MODIFY**: `WebExceptionAdvise` 集成 MessageSource 国际化
- **MODIFY**: `BusinessLog` 注解增加 module/operationType/samplingRate 属性
- **MODIFY**: `LogAspect` 增加异步 DB 写入逻辑
- **MODIFY**: `WebConfigure` 增加 API 统一前缀配置
- **MODIFY**: `SystemConfigController` 改为依赖 Facade
- **MODIFY**: `SystemConfigService` 返回 Entity 而非 VO

### 依赖影响

- **新增**: `sa-token-spring-boot4-starter:1.45.0`（optional，client-auth）
- **新增**: `bucket4j_jdk17-core:8.17.0`（optional，client-ratelimit）
- **新增**: Spring Boot 4 API Versioning（零额外依赖，框架自带）

### API 影响

- 所有端点新增 `/api` 前缀
- 新增 `POST /api/auth/login`、`POST /api/auth/logout` 登录认证端点
- 新增 `GET /api/system/operation-logs` 分页查询操作日志端点
- 新增 `GET /api/system/configs/page` 系统配置分页查询端点
- 所有响应支持国际化（Accept-Language Header）
