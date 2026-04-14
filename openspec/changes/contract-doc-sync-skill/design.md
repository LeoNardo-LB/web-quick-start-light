## Context

项目采用三轨文档体系（Contract/Constraint/Intent），Contract 轨文档（docs/modules/ + docs/architecture/）要求 100% 反映代码现状。当前使用通用的 `doc-sync-all` skill，不感知项目特有的三轨体系、四层架构和文档模板结构。学术研究表明 LLM 自我验证不可靠（Li 2026），需要多 Agent 独立验证机制。本设计创建一个通用 but 项目自适应的 `contract-doc-sync` skill。

## Goals / Non-Goals

**Goals:**
- 创建通用文档同步 skill，通过环境感知适配不同项目的文档体系
- 支持 4 级同步深度：L0（漂移检测）、L1（快速同步）、L2（常用同步，默认）、L3（高度同步）
- 实现 10 维验证矩阵（D1-D10）覆盖文档一致性的各个层面
- 实现多 Agent 共识验证协议：独立子 Agent 串行验证，连续 2 次零问题通过
- 自带 `md-sections` 工具作为兜底，优先使用项目自带版本
- 代码级证据优先于 LLM 主观判断

**Non-Goals:**
- 不修改任何 Java 源码或项目配置文件
- 不处理 Intent 轨（冻结不动）和 Constraint 轨（另一个 skill 的事）
- 不实现 CI/CD 集成（未来可扩展）
- 不实现 Mermaid 类图的完全自动生成（仅检测+提议修复）
- 不实现加权共识机制（当前用简单的连续通过计数，加权共识为未来扩展）

## Decisions

### D1: 环境感知优先于硬编码

**选择**：Skill 启动时通过 `**/README.md` glob 探测项目文档标准，构建项目文档模型，项目规则覆盖 skill 基线。

**替代方案**：硬编码本项目三轨体系规则 → 拒绝，因为不可复用。

**实现**：探测脚本扫描 docs/README.md（体系说明）、docs/modules/README.md（模板定义）等，提取文档轨道体系、模板章节结构、维护职责分工等。Skill 内置通用基线规则，项目探测结果覆盖/增强基线。

### D2: md-sections 双轨策略

**选择**：直接复制 bash 版 `md-sections` 到 skill 的 `scripts/` 目录。执行时优先使用项目自带的 `scripts/md-sections`，skill 自带版本作为兜底。

**替代方案**：用 Python 重写 → 拒绝，因为现有 bash 版本已足够健壮（338行，无外部依赖，处理了代码块/HTML注释等边界）。

### D3: 四级同步深度

**选择**：
- L0（漂移检测）：Phase 0 + Phase 1，只报告不修复，0 子Agent
- L1（快速同步）：+ Phase 2 修复 + Phase 3 快扫验证（每维1Agent，忽略Warning）
- L2（常用同步，默认）：+ Phase 3 共识验证（连续2次通过，忽略Warning，无上限）
- L3（高度同步）：+ Phase 3 共识验证×2轮 + 全局仔细扫描，含Warning

**替代方案**：三级（无L0）→ 拒绝，用户需要零成本快速了解漂移情况。

### D4: 10 维验证矩阵

**选择**：D1-D10 覆盖引用完整性、API签名、实体/类图、配置项、模板结构、交叉引用、版本号、AGENTS.md索引、变更历史、语义一致性。D10（语义一致性）仅 L3 触发。

**依据**：D1-D9 用代码级证据（grep/AST），D10 用 LLM 语义比对。映射到学术研究：Tan 2023（引用）、Zhou 2023（代码验证）、Dau 2023（语义）。

### D5: 共识验证协议——连续2次通过，无轮次上限

**选择**：每个维度启动独立子 Agent 串行验证，连续 2 个子 Agent 报告 0 问题才通过。发现问题则修复并归零计数。无最大轮次限制。

**替代方案**：设10轮上限 → 拒绝，用户明确要求无上限。

**子 Agent 隔离**：每次 Task 调用不传 task_id，确保独立会话。子 Agent prompt 完全自包含。

### D6: Skill 文件结构

**选择**：
```
contract-doc-sync/
├── SKILL.md                        # 核心入口（<500行）
├── references/
│   ├── environment-probing.md      # 环境感知探测规则
│   ├── change-mapping.md           # 代码→文档映射（通用基线）
│   ├── verification-dimensions.md  # 10维定义 + 子Agent prompt模板
│   └── sync-procedures.md          # 同步操作步骤
└── scripts/
    ├── detect-changes.py           # Git diff 预处理
    └── md-sections                 # 自带文档解析工具（bash）
```

**依据**：skill-creator 规范——SKILL.md < 500行，详细规则放 references/，可执行脚本放 scripts/。

### D7: 维护级别与 Agent 权限

**选择**：
- 🤖 确定性维护（方法签名、配置项、版本号）：子 Agent 可直接修复
- 🤖👤 半确定性维护（业务场景、设计考量）：子 Agent 提议，标记待确认
- 👤 创造性维护（架构级变更）：跳过，报告中提醒

**子 Agent 修复权限**：可修改 docs/ 下文档（通过 md-sections），禁止修改 .java / pom.xml / AGENTS.md（AGENTS.md 由主 Agent 统一管理）。

## Risks / Trade-offs

| 风险 | 严重度 | 缓解方案 |
|------|--------|---------|
| Mermaid 类图难以精确自动更新 | 中 | D3 标记为 🤖👤 模式，AI 提议修改人类确认 |
| 环境感知可能误判项目文档体系 | 中 | 无法识别时退化为通用基线模式 |
| references/ 中的规则与项目 docs/README.md 漂移 | 中 | SKILL.md 声明 docs/README.md 为权威来源 |
| L3 级别可能消耗大量 token（40-80 子Agent） | 低 | 用户主动选择 L3，默认为 L2 |
| Java 泛型/注解导致方法签名解析不准 | 低 | 优先用 grep 精确匹配，复杂情况让 AI 读源文件 |
