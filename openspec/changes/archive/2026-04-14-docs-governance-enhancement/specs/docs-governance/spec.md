## ADDED Requirements

### Requirement: 文档治理独立章节
docs/README.md SHALL 包含独立的 `## 文档治理` L2 章节，从原"文档系统设计"子章节中剥离，包含以下 L3 子章节：维护职责分工、文档与代码对齐机制、维护策略、反模式警示、可演进性设计。

#### Scenario: 文档治理章节存在于 docs/README.md 顶级
- **WHEN** 执行 `scripts/md-sections docs/README.md` 查看结构
- **THEN** 输出的 JSON 树中 SHALL 存在 `title: "文档治理"` 且 `level: 2` 的节点

#### Scenario: 文档治理章节包含完整子章节
- **WHEN** 执行 `scripts/md-sections docs/README.md "文档治理"` 获取子章节
- **THEN** SHALL 包含以下 5 个 `level: 3` 子章节：维护职责分工、文档与代码对齐机制、维护策略、反模式警示、可演进性设计

### Requirement: 维护职责三级分类
文档治理章节中的"维护职责分工"SHALL 将所有文档维护操作按确定性程度分为三级：🤖 确定性维护（AI 可自主完成）、🤖👤 半确定性维护（AI 提议人类确认）、👤 创造性维护（人类主导）。每级 SHALL 包含具体的触发场景、操作描述表格。

#### Scenario: 确定性维护操作可被 AI 自主执行
- **WHEN** 代码新增公开方法
- **THEN** 维护职责分工表格 SHALL 指示"更新对应 modules/ 文档的 API 参考章节"为 🤖 确定性维护

#### Scenario: 创造性维护操作由人类主导
- **WHEN** 架构级变更发生（如新增层次、模式变更）
- **THEN** 维护职责分工表格 SHALL 指示"更新对应 architecture/ 文档"为 👤 创造性维护

#### Scenario: 规则泛化不绑死文件名
- **WHEN** 查看维护职责分工表格的所有操作描述
- **THEN** 所有操作描述 SHALL 使用泛化路径（如"对应 modules/ 文档"）而非具体文件名（如"auth.md"）

### Requirement: 文档系统设计章节精简
docs/README.md 的 `## 文档系统设计` L2 章节 SHALL 仅保留设计哲学相关内容，不包含维护策略、反模式警示、对齐机制、可演进性设计、AI Agent 协同等治理内容。

#### Scenario: 文档系统设计不含治理子章节
- **WHEN** 执行 `scripts/md-sections docs/README.md "文档系统设计"` 获取子章节
- **THEN** 输出的子章节列表 SHALL NOT 包含以下标题：维护策略、反模式警示、文档与代码对齐机制、可演进性设计、与 AI Agent 的协同设计

### Requirement: AI Agent 协同独立章节
docs/README.md SHALL 包含独立的 `## 与 AI Agent 协同` L2 章节（从原 `### 与 AI Agent 的协同设计` 提升），内容 SHALL 扩展包括 AGENTS.md 定位、docs/ 对 AI 的价值、AI 可自主维护的文档范围、AI 需要人类确认的维护范围。

#### Scenario: AI Agent 协同为 L2 章节
- **WHEN** 执行 `scripts/md-sections docs/README.md` 查看结构
- **THEN** 输出的 JSON 树中 SHALL 存在 `title` 包含 "AI Agent" 且 `level: 2` 的节点

### Requirement: 维护策略表格扩展
维护策略章节 SHALL 为每行增加"维护者"列（🤖/🤖👤/👤）和"验证方式"列（如何确认维护已完成），并覆盖所有常见维护场景。

#### Scenario: 维护策略表格包含维护者列
- **WHEN** 读取 docs/README.md 的维护策略章节
- **THEN** 表格 SHALL 包含"维护者"列，值为 🤖 或 🤖👤 或 👤 之一

#### Scenario: 维护策略覆盖代码变更场景
- **WHEN** 查看维护策略表格的所有行
- **THEN** SHALL 包含以下触发场景行：代码新增方法、代码新增配置项、架构变更、规则变更、新增模块、版本升级

### Requirement: 反模式警示扩展
反模式警示章节 SHALL 包含以下类别的反模式：文档结构反模式（当前已有）、AI 维护反模式（新增）、维护遗漏反模式（新增）、内容膨胀反模式（新增）。每个反模式 SHALL 包含：反模式名称、为什么不好、本体系的做法。

#### Scenario: 反模式包含 AI 维护相关条目
- **WHEN** 读取反模式警示章节
- **THEN** SHALL 包含至少 2 条 AI 维护相关的反模式（如：AI 一次性重写整个文档、AI 修改 Intent 轨文档等）

### Requirement: 可演进性设计扩展
可演进性设计章节 SHALL 为每个演进方向增加具体的触发条件和实施步骤，并增加"演进决策树"——面对一个变更，应该走哪条演进路径。

#### Scenario: 每个演进方向有触发条件
- **WHEN** 读取可演进性设计章节
- **THEN** 每个演进方向 SHALL 包含明确的触发条件描述

### Requirement: 对齐机制场景化
文档与代码对齐机制章节 SHALL 按三轨分别展开，每轨包含：对齐流程图、具体触发场景列表、一致性漂移检测方法、自动化演进方向。

#### Scenario: 对齐机制包含三轨各自的策略
- **WHEN** 读取对齐机制章节
- **THEN** SHALL 包含 Contract 轨、Constraint 轨、Intent 轨三个子段，每段有具体的触发场景和验证方式
