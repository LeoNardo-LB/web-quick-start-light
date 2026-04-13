## Why

项目的文档体系存在三个结构性问题：

1. **文档职责混乱**：AGENTS.md（259行）与 docs/ 大量内容重复，AI 上下文过载（ETH Zurich 研究表明过长的 AGENTS.md 反而降低 AI 性能）；docs/ 按"文档类型"组织（教程/指南/参考）而非按"模块"组织，人类难以一眼看懂代码的运行逻辑与业务逻辑。
2. **缺少 Contract 文档**：OpenSpec 的 spec.md 是设计意图（Intent），归档时冻结不变；项目缺少 100% 反映代码现状的"合同文档"——人类可读、AI 可引用的代码映射文档。
3. **缺少文档模板和系统说明**：没有标准化的文档编写模板，没有说明文档系统三轨体系（Intent/Contract/Constraint）的指引文档，新人文档编写无规范可循。

## What Changes

1. **全面重写 docs/ 目录**：按三大分组重新组织 — `architecture/`（架构文档）、`conventions/`（编码规范）、`modules/`（业务模块混合文档），每个文档按模块或关注点切分，职责高度内聚、边界清晰。
2. **建立文档模板体系**：为三大分组定义标准模板（固定结构 + 半固定内容），并编写模板使用说明（用途、如何使用、注意事项）。
3. **创建文档系统说明文档**（docs/README.md）：阐述三轨文档体系（Intent = OpenSpec、Contract = docs/、Constraint = AGENTS.md + conventions/），为人类和 AI 提供导航。
4. **优化 AGENTS.md**：从"内联全量"转为"约束 + 引用索引"模式，删除从代码可推断的内容（精确版本号、项目结构索引、JVM 内存配置详表、详细文档路径表），添加带引用强度（⛔ MUST/⚠️ SHOULD/💡 MAY）的文档索引，包含 Contract 文档索引和 OpenSpec Intent specs 索引。

## Capabilities

### New Capabilities
- `docs-system-guide`: 文档系统说明文档（docs/README.md），阐述三轨体系、模板说明、文档导航
- `docs-architecture`: 架构文档分组（architecture/），包含 5 个按"读者问题"切分的文档
- `docs-conventions`: 编码规范分组（conventions/），包含 4 个按"关注点"切分的文档
- `docs-modules`: 业务模块分组（modules/），包含 12 个按"模块"切分的混合文档（业务+技术）

### Modified Capabilities
- `agents-md-and-docs`: AGENTS.md 优化为引用索引模式，删除可推断内容，添加带引用强度的文档索引

## Impact

- **docs/ 目录**：全面重写，现有文件全部替换
- **AGENTS.md**：内容结构重构，从内联模式转为引用索引模式
- **ARCHETYPE_README.md**：无需修改（骨架使用说明，独立于项目文档体系）
- **无代码变更**：本次变更仅涉及文档，不影响任何 Java 代码、测试或配置
- **无 API 变更**：不涉及 REST 端点修改
