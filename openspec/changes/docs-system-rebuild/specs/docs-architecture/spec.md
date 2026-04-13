## ADDED Requirements

### Requirement: system-overview.md 创建能力
- **WHEN** 开发者需要了解系统整体架构
- **THEN** 系统 MUST 在 docs/architecture/ 下创建 system-overview.md，包含 C4 Context 容器图（Mermaid 格式，展示 app 模块与 9 个 client 模块及 common 模块的关系）、技术栈概要表格（语言/框架/ORM/缓存/认证/限流/数据库/测试/构建）和系统设计目标章节

#### Scenario: C4 图展示模块关系
- **WHEN** 开发者阅读 system-overview.md
- **THEN** C4 图 MUST 展示 app（Spring Boot 应用）、common（异常体系）、9 个 client-* 模块（cache/oss/email/sms/search/log/ratelimit/idempotent/auth）以及 SQLite 数据库之间的依赖关系，箭头方向 MUST 反映实际的 Maven 依赖方向

### Requirement: module-structure.md 创建能力
- **WHEN** 开发者需要了解 Maven 多模块结构和四层架构
- **THEN** 系统 MUST 在 docs/architecture/ 下创建 module-structure.md，包含目录树结构（标注每个模块的 packaging 类型）、四层架构说明（Controller → Facade → Service → Repository）、层间依赖规则表（禁止关系和允许关系）和 ArchUnit 规则列表

#### Scenario: 四层架构约束可视化
- **WHEN** 开发者查阅 module-structure.md
- **THEN** 文档 MUST 包含四层架构的 Mermaid 流程图，每层 MUST 标注职责说明，层间 MUST 标注允许的依赖方向和禁止的依赖方向（如 Controller → Repository 标注为红色禁止线）

### Requirement: request-lifecycle.md 创建能力
- **WHEN** 开发者需要了解 HTTP 请求的完整处理流程
- **THEN** 系统 MUST 在 docs/architecture/ 下创建 request-lifecycle.md，包含请求流转时序图（Mermaid sequenceDiagram 格式，覆盖从 HTTP 请求进入到响应返回的完整链路）、各环节职责说明和关键组件列表

#### Scenario: 请求时序图覆盖全链路
- **WHEN** 开发者阅读 request-lifecycle.md
- **THEN** 时序图 MUST 按顺序展示以下参与者：Client → ContextFillFilter → Controller → Facade → Service → Repository(Mapper) → SQLite，每个箭头 MUST 标注传递的数据类型（如 Request → Command → Entity → DO）

### Requirement: design-patterns.md 创建能力
- **WHEN** 开发者需要了解项目中使用的设计模式
- **THEN** 系统 MUST 在 docs/architecture/ 下创建 design-patterns.md，包含 Template Method 模式说明（AbstractXxxClient 抽象类结构、final 公开方法 + do* 扩展点）、条件装配模式说明（@AutoConfiguration + @ConditionalOnClass + @ConditionalOnProperty 组合）和每个模式的代码示例

#### Scenario: Template Method 模式文档
- **WHEN** 开发者查阅 design-patterns.md
- **THEN** 文档 MUST 展示 AbstractXxxClient 的类图（Mermaid classDiagram），包含 final 公开方法、do* 保护方法、子类实现关系，且 MUST 列出所有使用此模式的客户端（cache/oss/email/sms/search/ratelimit/idempotent/auth）

### Requirement: thread-context.md 创建能力
- **WHEN** 开发者需要了解线程上下文传递机制
- **THEN** 系统 MUST 在 docs/architecture/ 下创建 thread-context.md，包含 ScopedValue 传递链说明（ScopedThreadContext 使用 Java 25 ScopedValue API）、ContextRunnable/ContextCallable 异步包装说明和传递链的 Mermaid 序列图

#### Scenario: 异步场景上下文传递
- **WHEN** 开发者在异步场景（如 @Async 方法）中使用 ScopedThreadContext
- **THEN** 文档 MUST 展示通过 ContextRunnable/ContextCallable 包装后的上下文传递时序图，说明 ScopedValue 如何跨线程边界传递 userId 和 traceId

### Requirement: 架构文档统一模板结构
- **WHEN** 维护者创建或更新 architecture/ 下的任何文档
- **THEN** 每个文档 MUST 使用固定模板结构：`📋 目录`（锚点链接到所有二级标题）→ 标题 → 概述 → 核心内容（Mermaid 图 + 表格 + 代码示例） → 与其他文档的关系 → 变更历史，且每个文档 MUST 在顶部包含所属轨道标签（Contract 轨）

#### Scenario: 模板结构验证
- **WHEN** 审查 architecture/ 下的任意文档
- **THEN** 文档 MUST 在开头包含 `📋 目录` 章节（列出所有二级标题的锚点链接），MUST 包含 `# <文档标题>` 一级标题、`## 概述` 章节、核心内容章节（至少包含一个 Mermaid 图和一个表格）、`## 相关文档` 章节和 `## 变更历史` 章节
