## Why

第一轮架构增强（enhance-architecture-from-backend）已完成 70 个任务，但存在三个遗留问题：

1. **traceId 在日志中不可见**（P0 Bug）：ContextFillFilter 正确设置了 ScopedThreadContext.traceId，但未写入 MDC，logback pattern 也未引用 `%X{traceId}`，导致整个 traceId 传播链在日志中完全不可见——全链路追踪等于白做。
2. **日志系统仅覆盖基本 AOP 日志**：缺少生产环境必需的启动验证、慢查询拦截、JSON 结构化日志，以及高级能力（采样过滤、审计追踪、脱敏工具、日志标记）。参考项目 speccoding-fullstack 拥有完整的 7 组件日志基础设施，本项目仅有 2 个文件（BizLog + BizLogAspect）。
3. **项目无 AI 编码规范体系**：没有 AGENTS.md、没有 docs/ 目录，AI 助手进入项目后缺乏系统性开发规范指引。参考项目拥有 3 个 AGENTS.md + 25 个 docs/ 文件的成熟文档体系。README.md 也存在过时内容（仍提到 H2 数据库、旧的任务描述）。

## What Changes

### T1: 日志系统增强

**P0 — Bug 修复：**
- ContextFillFilter 添加 `MDC.put("traceId", ...)` / `MDC.remove("traceId")`
- logback-spring.xml pattern 添加 `%X{traceId}` 占位符

**P1 — 生产环境刚需：**
- 引入 LoggingProperties（`logging.slow-query.*` / `logging.sampling.*` 可配置开关和阈值）
- 引入 LoggingConfiguration（启动时验证日志目录存在、可写、磁盘空间充足）
- 引入 SlowQueryInterceptor（MyBatis Interceptor，拦截慢 SQL，默认阈值 1000ms）
- logback-spring.xml 添加 JSON Appender（生产环境结构化日志）

**P2 — 高级能力：**
- 引入 SamplingTurboFilter（Logback TurboFilter，ERROR 始终记录，其余按采样率过滤）
- 引入 AuditLogService + AuditEvent（审计日志，使用独立 AUDIT_LOGGER 写入审计文件）
- 引入 SensitiveLogUtils（日志脱敏工具，默认中间 75% 替换为 `*`）
- 引入 LogMarkers（通用日志标记常量，不绑定具体业务，提供 API / BUSINESS / SECURITY / AUDIT / SLOW_QUERY 等通用分类）
- logback-spring.xml 扩展为 7 个 Appender（CONSOLE / FILE / JSON_FILE / ERROR_FILE / AUDIT_FILE / SLOW_QUERY_FILE / CURRENT）
- 引入 LoggingConfigure（Spring @Configuration，条件注册各日志组件）

### T2: AI 规范体系建立

- 创建 AGENTS.md（<200 行入口文件，核心规则 + 引用 docs/）
- 创建 CLAUDE.md 符号链接（兼容 Claude Code）
- 创建 docs/ 目录体系（三层架构适配版）：
  - `docs/README.md`（文档导航索引）
  - `docs/architecture/overview.md`（三层架构总览）
  - `docs/architecture/dependency-rules.md`（依赖规则）
  - `docs/backend/coding-standards.md`（编码规范）
  - `docs/backend/testing-guide.md`（测试规范）
  - `docs/backend/error-handling.md`（错误处理规范）
  - `docs/backend/performance-rules.md`（性能规则）
  - `docs/workflow/tdd-guide.md`（TDD 指南）
  - `docs/workflow/openspec-guide.md`（OpenSpec 工作流）
  - `docs/guides/getting-started.md`（快速开始）

### T3: 文档全面更新

- 更新 README.md（H2→SQLite、技术栈表格、多环境配置命令、项目特性）
- 更新 enhance-architecture-from-backend/tasks.md（追加 Phase 2 精简任务记录）

## Capabilities

### New Capabilities

- `logging-enhancement`: 日志系统增强（MDC traceId + 启动验证 + 慢查询拦截 + 采样过滤 + 审计日志 + 脱敏工具 + 日志标记 + logback 多 Appender）
- `agents-md-and-docs`: AI 编码规范体系（AGENTS.md 入口 + docs/ 目录结构 + 编码/测试/架构/工作流规范文档）

### Modified Capabilities

（无现有 spec 的 requirement 变更，本次变更涉及的日志组件和文档体系均为全新引入）

## Impact

- **现有代码修改**: ContextFillFilter（添加 MDC 操作）、logback-spring.xml（扩展 pattern 和 Appender）
- **新增代码**: ~15 个 Java 类（日志基础设施组件）+ ~11 个文档文件
- **新增配置**: LoggingProperties + LoggingConfigure + logback Appender 配置
- **无 BREAKING 变更**: 所有修改向后兼容，现有功能不受影响
