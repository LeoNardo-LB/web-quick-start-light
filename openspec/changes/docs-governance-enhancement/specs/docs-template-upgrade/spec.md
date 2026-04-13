## ADDED Requirements

### Requirement: modules 模板固定层扩展为 8 章节
docs/modules/README.md 的模板固定层 SHALL 从 6 个章节扩展为 8 个章节，新增"相关文档"和"变更历史"为固定层必选章节。章节顺序 SHALL 为：概述 → 业务场景 → 技术设计 → API 参考 → 配置参考 → 使用指南 → 相关文档 → 变更历史。

#### Scenario: modules 模板固定层包含 8 个章节
- **WHEN** 执行 `scripts/md-sections docs/modules/README.md "模板结构" "固定层（不可修改）"`
- **THEN** 输出的模板代码块 SHALL 包含 `## 相关文档` 和 `## 变更历史` 两个章节

#### Scenario: modules 模板检查清单更新
- **WHEN** 执行 `scripts/md-sections docs/modules/README.md "模板结构" "编写检查清单"`
- **THEN** 检查清单 SHALL 包含"包含相关文档章节"和"包含变更历史章节"两个检查项

### Requirement: modules 模板三轨标签约定
docs/modules/README.md 的模板 SHALL 约定一级标题的轨道标签格式：`# <模块名> — Contract 轨`。标题下方 SHALL 包含 blockquote 说明维护方向：`> 代码变更时必须同步更新本文档`。

#### Scenario: modules 模板包含三轨标签格式约定
- **WHEN** 执行 `scripts/md-sections docs/modules/README.md "模板结构" "固定层（不可修改）"`
- **THEN** 模板代码块的一级标题 SHALL 包含 `— Contract 轨` 标记

### Requirement: conventions 模板半固定层扩展指引
docs/conventions/README.md 的模板半固定层 SHALL 增加以下扩展指引：每条规则可包含"设计理由"子段（说明为什么这样规定）、"常见违规场景"子段（实际项目中容易犯的错）、"关联规则"子段（与其他规则的关系）。

#### Scenario: conventions 模板包含扩展指引
- **WHEN** 执行 `scripts/md-sections docs/conventions/README.md "模板结构" "半固定层（内容按实际填充）"`
- **THEN** 半固定层说明 SHALL 提及"设计理由"、"常见违规场景"和"关联规则"作为可选扩展内容

### Requirement: architecture 模板适当优化
docs/architecture/README.md 的模板 SHALL 在半固定层中增加建议：架构文档可包含"设计考量"段（解释为什么选择这种架构而非其他方案）、"系统边界"段（说明系统的范围和不覆盖的内容）。

#### Scenario: architecture 模板包含设计考量建议
- **WHEN** 执行 `scripts/md-sections docs/architecture/README.md "模板结构" "半固定层（内容按实际填充）"`
- **THEN** 半固定层说明 SHALL 提及"设计考量"和"系统边界"作为可选扩展内容
