## ADDED Requirements

### Requirement: modules 文档补全相关文档章节
docs/modules/ 下所有文档 SHALL 包含"相关文档"章节，链接到相关的 architecture/ 文档、conventions/ 文档、以及有交互关系的其他 modules/ 文档。

#### Scenario: 每个 modules 文档都有相关文档章节
- **WHEN** 对 docs/modules/ 下每个 .md 文件执行 `scripts/md-sections <file> "相关文档"`
- **THEN** 每个文件 SHALL 返回有效内容（而非 `not_found` 错误）

### Requirement: modules 文档补全变更历史章节
docs/modules/ 下所有文档 SHALL 包含"变更历史"章节，记录至少一条初始版本记录（包含日期、版本、描述）。

#### Scenario: 每个 modules 文档都有变更历史章节
- **WHEN** 对 docs/modules/ 下每个 .md 文件执行 `scripts/md-sections <file> "变更历史"`
- **THEN** 每个文件 SHALL 返回有效内容（而非 `not_found` 错误）

### Requirement: modules 文档标题三轨标签统一
docs/modules/ 下所有文档的一级标题 SHALL 追加 `— Contract 轨` 标记。标题下方 SHALL 包含 blockquote 说明：`> 代码变更时必须同步更新本文档`。

#### Scenario: modules 文档标题包含三轨标签
- **WHEN** 对 docs/modules/ 下每个 .md 文件执行 `scripts/md-sections <file>` 获取标题
- **THEN** 每个 `title` 字段 SHALL 以 `— Contract 轨` 或 `— Contract 轨，100% 反映代码现状` 结尾

### Requirement: modules 文档业务场景扩展
docs/modules/ 下所有文档的"业务场景"章节 SHALL 包含至少 3 个用例，每个用例包含场景名称和简短流程描述（而非仅一句话）。

#### Scenario: 业务场景至少 3 个用例且有描述
- **WHEN** 读取 docs/modules/ 下任意文档的"业务场景"章节
- **THEN** SHALL 包含至少 3 个用例，每个用例 SHALL 包含名称和 1-3 句流程描述

### Requirement: modules 文档使用指南扩展
docs/modules/ 下所有文档的"使用指南"章节 SHALL 包含至少 2 个不同场景的完整代码示例（而非仅有"集成步骤"一个场景）。

#### Scenario: 使用指南包含多场景代码示例
- **WHEN** 读取 docs/modules/ 下任意文档的"使用指南"章节
- **THEN** SHALL 包含至少 2 个不同场景的代码示例（如基础用法 + 高级用法，或集成步骤 + 常见操作）

### Requirement: conventions 文档设计考量扩展
docs/conventions/ 下所有文档的"规则"章节中，每条 ⛔ MUST 级别规则 SHALL 包含"设计理由"说明（为什么这样规定而不是其他方案）。

#### Scenario: MUST 规则包含设计理由
- **WHEN** 读取 docs/conventions/ 下任意文档的规则章节
- **THEN** 每个 ⛔ MUST 级别的规则 SHALL 包含"设计理由"或"为什么"说明段落

### Requirement: conventions 文档常见违规场景扩展
docs/conventions/ 下所有文档 SHALL 在规则章节中包含至少 2 个"常见违规场景"描述（实际项目中容易犯的错误和正确做法）。

#### Scenario: conventions 包含常见违规场景
- **WHEN** 读取 docs/conventions/ 下任意文档
- **THEN** SHALL 包含至少 2 个"常见违规场景"或"常见错误"描述

### Requirement: architecture 文档适当扩展
docs/architecture/ 下的文档 SHALL 在适当位置增加"设计考量"段（说明为什么选择这种设计而非其他方案）。system-overview.md SHALL 增加"系统边界"说明。

#### Scenario: system-overview 包含系统边界说明
- **WHEN** 读取 docs/architecture/system-overview.md 的结构
- **THEN** SHALL 包含"系统边界"或类似标题的章节/段落

### Requirement: 内容扩展遵守职责边界
所有文档的内容扩展 SHALL 遵循"需要引用其他文档的内容时，不复制，用链接"原则。扩展内容 SHALL NOT 侵入其他文档的职责范围。

#### Scenario: modules 文档不重复 Template Method 模式说明
- **WHEN** 读取 docs/modules/ 下任意 client-* 文档的"技术设计"章节
- **THEN** Template Method 模式的完整说明 SHALL 出现在 design-patterns.md 中，modules/ 文档只引用（链接）而非完整描述
