## MODIFIED Requirements

### Requirement: AGENTS.md 多模块结构索引
AGENTS.md 的模块结构描述 SHALL 更新为四层架构（Controller → Facade → Service → Repository）。模块索引 SHALL 新增 client-auth、client-ratelimit、client-idempotent 模块。技术栈表格 SHALL 新增 Sa-Token、Bucket4j 依赖。

#### Scenario: AGENTS.md 反映四层架构
- **WHEN** 开发者查阅 AGENTS.md
- **THEN** 文档 SHALL 清晰描述 Controller → Facade → Service → Repository 四层架构及各层职责

#### Scenario: AGENTS.md 包含新增客户端模块
- **WHEN** 开发者查阅 AGENTS.md
- **THEN** 文档 SHALL 在模块索引中列出 client-auth、client-ratelimit、client-idempotent

### Requirement: 文档全面对齐
所有改造完成后，系统 SHALL 全面对齐代码与文档。包括 AGENTS.md、ARCHETYPE_README.md、docs/ 目录下所有文档 SHALL 反映最终代码状态。

#### Scenario: 文档与代码完全一致
- **WHEN** 所有改造完成并运行文档对齐检查
- **THEN** AGENTS.md 的模块索引、技术栈、编码规则 SHALL 与实际代码完全一致
