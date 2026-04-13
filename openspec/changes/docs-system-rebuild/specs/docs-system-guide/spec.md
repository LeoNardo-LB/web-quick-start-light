## ADDED Requirements

### Requirement: docs/README.md 三轨文档体系说明
- **WHEN** 开发者阅读 docs/README.md
- **THEN** 文档 MUST 定义三轨文档体系：Intent 轨（OpenSpec 变更工作流）、Contract 轨（docs/ 目录下的架构/规范/模块文档）、Constraint 轨（AGENTS.md + conventions/ 编码约束），且每个轨道 MUST 包含定位说明和适用场景描述

#### Scenario: 三轨文档体系导航
- **WHEN** 开发者打开 docs/README.md
- **THEN** 页面顶部 MUST 展示三轨文档体系的分类概览，包含每个轨道的名称、定位说明（一句话）和文档数量，并使用锚点链接到各轨道的详细文档列表

### Requirement: 文档导航能力
- **WHEN** 开发者需要查找特定主题的文档
- **THEN** docs/README.md MUST 提供结构化的文档索引表，每个条目包含文档路径、文档标题、一句话摘要和适用场景列，索引表 MUST 按三轨分类组织

#### Scenario: 按场景查找文档
- **WHEN** 开发者需要了解"如何编写单元测试"
- **THEN** 通过文档索引表的"适用场景"列，MUST 能定位到 testing-conventions.md 文档，且索引表条目 MUST 包含指向该文档的相对路径链接

### Requirement: 模板使用说明展示
- **WHEN** 新文档需要被创建或现有文档需要被更新
- **THEN** docs/README.md MUST 包含"文档模板"章节，列出所有可用模板的名称、用途说明和模板结构定义（固定层和半固定层的具体内容），每个模板 MUST 附带使用场景说明和 📋 目录要求（所有 docs/ 文件开头 MUST 包含 `📋 目录` 章节列出所有二级标题的锚点链接）

#### Scenario: 查看可用模板列表
- **WHEN** 维护者需要创建新的模块文档
- **THEN** docs/README.md 的模板章节 MUST 列出 module-doc-template.md 模板，包含其用途说明（"定义单个模块的业务场景、技术设计、API 参考和配置参考"）和模板文件路径

### Requirement: 文档职责内聚验证
- **WHEN** 维护者审查文档系统是否完整
- **THEN** docs/README.md MUST 包含"文档覆盖度"章节，列出每个轨道的预期文档数量、当前文档数量和覆盖率百分比，缺失文档 MUST 以明确标识标注

#### Scenario: 识别缺失文档
- **WHEN** 架构轨道新增了一个模块但未创建对应文档
- **THEN** 文档覆盖度章节 MUST 显示该模块文档为"缺失"状态，覆盖率百分比 MUST 相应下降
