## ADDED Requirements

### Requirement: 环境感知探测
Skill 启动时 SHALL 自动探测项目文档体系标准，构建项目文档模型（ProjectDocProfile），包括文档轨道体系、模板结构、代码-文档映射规则、维护职责分工、可用工具和技术栈。

#### Scenario: 探测三轨体系项目
- **WHEN** Skill 在包含 `docs/README.md` 的项目中启动，且该文件描述了三轨文档体系（Contract/Constraint/Intent）
- **THEN** Skill SHALL 提取三轨定义（轨道类型、位置、真相源、对齐方向），并以此为最高优先级规则

#### Scenario: 探测无文档体系的项目
- **WHEN** Skill 在不包含 `docs/README.md` 或该文件未描述文档体系的项目中启动
- **THEN** Skill SHALL 退化为通用基线模式，使用 skill 内置的默认规则

#### Scenario: README.md glob 匹配
- **WHEN** Skill 执行环境感知
- **THEN** Skill SHALL 使用 `**/README.md` glob 模式扫描所有 README 文件，并按路径分类（docs/README.md → 体系说明、docs/modules/README.md → 模板定义、docs/architecture/README.md → 模板定义、根 README.md → 项目概述）

#### Scenario: 探测文档解析工具
- **WHEN** Skill 检查到项目存在 `scripts/md-sections`
- **THEN** Skill SHALL 优先使用项目自带版本；当项目无此工具时，SHALL 使用 skill 自带的 `scripts/md-sections` 作为兜底

#### Scenario: 项目规则覆盖 skill 基线
- **WHEN** 项目探测结果与 skill 内置基线规则存在冲突
- **THEN** Skill SHALL 以项目规则为准，基线规则中被覆盖的部分 SHALL 被标记为"已覆盖"

### Requirement: 项目文档模型输出
环境感知完成后 SHALL 输出结构化的项目文档模型，包含文档系统类型、轨道定义、模板章节结构、代码→文档映射规则、维护级别定义、章节解析工具路径。

#### Scenario: 模型输出包含必要字段
- **WHEN** 环境感知完成
- **THEN** 输出的 ProjectDocProfile SHALL 至少包含：docSystem（体系类型）、tracks（轨道列表）、templates（模板结构）、codeDocMapping（代码→文档映射）、sectionParser（解析工具路径）

### Requirement: 探测 AGENTS.md / CLAUDE.md 规范
Skill SHALL 检查项目根目录是否存在 AGENTS.md 或 CLAUDE.md，从中提取文档引用规则和约束摘要。

#### Scenario: 发现 AGENTS.md
- **WHEN** 项目根目录存在 AGENTS.md 且包含文档引用规则
- **THEN** Skill SHALL 提取其中的文档加载规则（如 md-sections 使用约定）和约束摘要，纳入项目文档模型
