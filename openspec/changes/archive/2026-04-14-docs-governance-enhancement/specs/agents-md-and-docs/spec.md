## ADDED Requirements

### Requirement: AGENTS.md 文档维护职责引用
AGENTS.md SHALL 在"文档索引"章节之前包含"文档维护职责"段落，该段落 SHALL 直接引用 docs/README.md 的具体治理章节（章节级锚点引用，非文件级引用）。引用范围 SHALL 至少包含：文档编写原则、维护职责分工、文档与代码对齐机制、维护策略、反模式警示。

#### Scenario: AGENTS.md 包含文档维护职责段落
- **WHEN** 执行 `scripts/md-sections AGENTS.md` 查看结构
- **THEN** JSON 树中 SHALL 存在 `title` 包含"维护职责"的章节节点，且该节点位于"文档索引"章节之前

#### Scenario: 引用为章节级锚点
- **WHEN** 读取 AGENTS.md 中"文档维护职责"段落的内容
- **THEN** 每个引用链接 SHALL 使用 `docs/README.md#<章节锚点>` 格式（如 `docs/README.md#维护策略`），而非仅 `docs/README.md` 文件级引用

#### Scenario: 引用至少包含 5 个治理章节
- **WHEN** 读取 AGENTS.md 中"文档维护职责"段落的内容
- **THEN** SHALL 包含以下 5 个章节的引用链接：文档编写原则、维护职责分工、文档与代码对齐机制、维护策略、反模式警示
