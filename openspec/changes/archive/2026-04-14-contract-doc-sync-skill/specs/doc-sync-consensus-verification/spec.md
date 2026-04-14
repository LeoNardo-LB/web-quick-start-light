## ADDED Requirements

### Requirement: 四级同步深度
Skill SHALL 支持 4 个同步级别，用户通过触发词选择。未指定级别时默认使用 L2。

#### Scenario: L0 漂移检测
- **WHEN** 用户触发词为 "检查漂移" 或 "L0"
- **THEN** Skill SHALL 仅执行 Phase 0（环境感知）和 Phase 1（发现变更），输出漂移报告，不修复任何文件，不启动任何子 Agent

#### Scenario: L1 快速同步
- **WHEN** 用户触发词为 "快速同步" 或 "L1"
- **THEN** Skill SHALL 执行 Phase 0-3，每个验证维度启动 1 个子 Agent 快扫，只检查 Error 级别问题，忽略 Warning

#### Scenario: L2 常用同步（默认）
- **WHEN** 用户触发词为 "同步文档"、"L2" 或未指定级别
- **THEN** Skill SHALL 执行 Phase 0-3，采用共识验证协议（连续 2 个子 Agent 零问题通过），忽略 Warning，无轮次上限

#### Scenario: L3 高度同步
- **WHEN** 用户触发词为 "深度同步" 或 "L3"
- **THEN** Skill SHALL 执行 Phase 0-4：共识验证跑两轮，每轮连续 2 次通过；含 Warning 也计为问题（归零计数）；最后执行全局仔细扫描

### Requirement: 共识验证协议
每个验证维度 SHALL 采用多独立子 Agent 串行验证机制。子 Agent 必须是独立会话（不传 task_id），每个子 Agent 的 prompt 完全自包含。

#### Scenario: 连续通过机制
- **WHEN** 连续 2 个子 Agent 对同一维度报告 0 个问题（L2/L3 含 Warning 检查时为 0 Warning + 0 Error）
- **THEN** 该维度 SHALL 被标记为"通过"

#### Scenario: 发现问题归零
- **WHEN** 某个子 Agent 发现 1 个或多个问题并完成修复
- **THEN** 连续通过计数 SHALL 归零，下一个子 Agent 从 0 开始重新计数

#### Scenario: 无轮次上限
- **WHEN** 某维度反复发现问题
- **THEN** Skill SHALL 持续启动新的子 Agent 进行验证，不设最大轮次限制

### Requirement: 子 Agent 隔离性
每个验证子 Agent SHALL 是完全独立的会话，不共享上下文，不知道前序子 Agent 的存在和工作结果。

#### Scenario: 独立会话创建
- **WHEN** 主 Agent 为某维度启动一个验证子 Agent
- **THEN** SHALL 使用 Task 工具且不传 task_id 参数，确保创建全新会话

#### Scenario: 自包含 prompt
- **WHEN** 子 Agent 的验证 prompt 生成
- **THEN** prompt SHALL 包含完整的验证维度定义、检查步骤、输出格式、修复权限，不依赖任何外部上下文

### Requirement: 子 Agent 修复权限
验证子 Agent SHALL 只能修改 docs/ 目录下的文档文件，禁止修改源代码和配置文件。

#### Scenario: 允许修改 docs/ 文档
- **WHEN** 子 Agent 发现 docs/ 下文档与代码不一致
- **THEN** 子 Agent SHALL 可直接修复（通过 md-sections 精准操作）

#### Scenario: 禁止修改源码
- **WHEN** 子 Agent 发现源码与文档不一致
- **THEN** 子 Agent SHALL 不修改任何 .java / pom.xml 文件，仅修复文档侧

#### Scenario: 禁止修改 AGENTS.md
- **WHEN** 验证维度涉及 AGENTS.md 索引
- **THEN** 子 Agent SHALL 将索引问题报告给主 Agent，由主 Agent 统一处理

### Requirement: 维护级别映射
Skill SHALL 将同步操作按确定性程度分为三级，对应不同的处理策略。

#### Scenario: 确定性维护
- **WHEN** 同步内容可从代码直接推断（方法签名、配置项、版本号）
- **THEN** 子 Agent SHALL 自动修复，无需人类确认

#### Scenario: 半确定性维护
- **WHEN** 同步内容需要理解业务上下文（业务场景描述、设计考量）
- **THEN** 子 Agent SHALL 提议修改并标记为"待确认"，在报告中列出

#### Scenario: 创造性维护
- **WHEN** 同步内容需要原创性思考（架构级变更、新增设计模式）
- **THEN** Skill SHALL 跳过该维护，在报告中提醒人类处理

### Requirement: 最终报告输出
同步完成后 SHALL 输出结构化报告，包含所有维度的验证矩阵、修复记录和建议。

#### Scenario: 报告内容
- **WHEN** 同步流程完成
- **THEN** 报告 SHALL 包含：每个维度的通过状态、每轮验证的子 Agent 结果摘要、🤖/🤖👤/👤 分级修复记录、建议后续操作
