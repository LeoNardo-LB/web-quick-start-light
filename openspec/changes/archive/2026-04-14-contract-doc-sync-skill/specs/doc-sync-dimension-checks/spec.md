## ADDED Requirements

### Requirement: 10 维验证矩阵
Skill SHALL 定义 10 个独立验证维度，覆盖文档一致性的各个层面。每个维度有独立的检查步骤、真相源、问题判定标准和子 Agent prompt 模板。

#### Scenario: 维度完整性
- **WHEN** Skill 执行验证阶段
- **THEN** SHALL 执行以下 10 个维度：D1(引用完整性)、D2(API签名一致性)、D3(实体/类图一致性)、D4(配置项一致性)、D5(模板结构合规性)、D6(交叉引用完整性)、D7(版本号一致性)、D8(AGENTS.md索引完整性)、D9(变更历史完整性)、D10(语义一致性)

#### Scenario: D10 仅 L3 触发
- **WHEN** 同步级别为 L0、L1 或 L2
- **THEN** D10（语义一致性）SHALL 被跳过，仅报告"未检查"
- **WHEN** 同步级别为 L3
- **THEN** D10 SHALL 被执行

### Requirement: D1 引用完整性
SHALL 检测文档中引用的类名、方法名、文件路径是否在代码库中实际存在。

#### Scenario: 引用不存在
- **WHEN** 文档中引用了类名 `SystemConfigExporter` 或路径 `app/service/system/SystemConfigExporter.java`
- **THEN** Skill SHALL 在代码库中搜索确认其存在，不存在则报告为 Error

### Requirement: D2 API 签名一致性
SHALL 检查文档中 API 参考章节的端点信息与 Controller 源码完全一致。

#### Scenario: 端点不匹配
- **WHEN** 文档记录 `GET /api/system/configs/export` 但 Controller 中实际为 `POST`
- **THEN** SHALL 报告为 Error

#### Scenario: 端点缺失
- **WHEN** Controller 中存在公开端点但文档未记录
- **THEN** SHALL 报告为 Error

### Requirement: D3 实体/类图一致性
SHALL 检查文档中 Mermaid 类图的字段、方法和关系线与 Java Entity/DO/VO/Request 类完全一致。此维度没得商量，必须严格。

#### Scenario: 字段缺失
- **WHEN** Java 类 `SystemConfig` 有字段 `lastModifiedBy` 但类图未反映
- **THEN** SHALL 报告为 Error 并修复

#### Scenario: 关系线缺失
- **WHEN** 两个类之间存在关联但类图中缺少关系线
- **THEN** SHALL 报告为 Error

### Requirement: D4 配置项一致性
SHALL 检查文档中配置参考章节的配置项名称、类型、默认值与 Properties 类完全一致。

#### Scenario: 配置项不匹配
- **WHEN** Properties 类有字段 `corePoolSize`（Integer，默认4）但文档记录为 `pool-size`（String）
- **THEN** SHALL 报告为 Error

### Requirement: D5 模板结构合规性
SHALL 检查文档是否符合其模板定义的固定层+半固定层结构。

#### Scenario: 缺少必要章节
- **WHEN** modules/ 文档缺少"变更历史"章节
- **THEN** SHALL 报告为 Error

### Requirement: D6 交叉引用完整性
SHALL 检查文档间"相关文档"链接的有效性和双向覆盖。

#### Scenario: 死链
- **WHEN** 文档 A 引用了 `docs/modules/xxx.md` 但该文件不存在
- **THEN** SHALL 报告为 Error

### Requirement: D7 版本号一致性
SHALL 检查 architecture 文档技术栈表格中的版本号与 pom.xml 一致。

#### Scenario: 版本号不匹配
- **WHEN** pom.xml 中 Spring Boot 版本为 4.0.2 但文档记录为 4.0.1
- **THEN** SHALL 报告为 Error

### Requirement: D8 AGENTS.md 索引完整性
SHALL 检查 AGENTS.md 的文档索引表是否覆盖所有实际存在的文档。

#### Scenario: 索引缺失
- **WHEN** docs/modules/ 下有 `new-module.md` 但 AGENTS.md 索引表中未列出
- **THEN** SHALL 报告为 Error

### Requirement: D9 变更历史完整性
SHALL 检查被修改的文档是否追加了变更记录。

#### Scenario: 缺少变更记录
- **WHEN** 文档在本次同步中被修改但变更历史表未追加新行
- **THEN** SHALL 报告为 Error

### Requirement: D10 语义一致性
SHALL 使用 LLM 语义比对，检查文档描述与代码实际行为是否语义一致。仅在 L3 级别触发。

#### Scenario: 行为描述不准确
- **WHEN** 文档描述"该方法返回所有配置"但代码实际返回分页结果
- **THEN** SHALL 报告为 Warning（L3 中 Warning 也计为问题）

### Requirement: 子 Agent prompt 模板
每个维度的子 Agent prompt SHALL 完全自包含，包含角色定义、检查范围、检查步骤、问题判定标准、修复权限、输出格式。

#### Scenario: prompt 自包含性
- **WHEN** 为某维度生成子 Agent prompt
- **THEN** prompt SHALL 包含：验证维度名称和描述、检查步骤（含具体工具调用命令）、问题分级标准（Error/Warning）、修复权限说明、结构化输出格式模板

### Requirement: 代码级证据优先
D1-D4 和 D7 的验证 SHALL 优先使用代码级证据（grep/文件存在性检查/AST 解析），不依赖 LLM 主观判断。

#### Scenario: 代码级验证
- **WHEN** 验证 D2（API 签名一致性）
- **THEN** 子 Agent SHALL 直接读取 Controller.java 源码提取方法签名，与文档对比，而非让 LLM 凭记忆判断
