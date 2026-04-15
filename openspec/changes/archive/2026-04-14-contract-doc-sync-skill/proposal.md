## Why

项目采用三轨文档体系（Contract/Constraint/Intent），其中 Contract 轨（docs/modules/ + docs/architecture/）要求 100% 反映代码现状。学术研究表明，代码-文档不一致导致 Bug 引入率增加 1.5 倍，25%+ 热门项目存在过时文档引用（Tan 2023, Radmanesh 2024）。现有 `doc-sync-all` skill 是通用的，不感知本项目的三轨体系、四层架构和文档模板结构。此外，LLM 自我验证不可靠（Accuracy-Correction Paradox, Li 2026），需要多 Agent 共识验证机制保证同步质量。需要一个项目自适应的通用文档同步 skill，支持多级同步深度和多 Agent 交叉验证。

## What Changes

- **新增 `contract-doc-sync` skill**：放置于 `~/.config/opencode/skills/contract-doc-sync/`，支持 L0（漂移检测）、L1（快速同步）、L2（常用同步，默认）、L3（高度同步）四个级别
- **环境感知机制**：Skill 启动时自动探测项目文档体系标准（通过 `**/README.md` glob 匹配和关键文件扫描），项目规则优先于 skill 内置基线
- **共识验证协议**：L2/L3 级别采用多独立子 Agent 串行验证，连续 2 次零问题（含 warning，L3）才视为通过，无轮次上限
- **10 维验证矩阵**：D1-D10 覆盖引用完整性、API 签名、实体/类图、配置项、模板结构、交叉引用、版本号、索引、变更历史、语义一致性
- **自带文档解析工具**：复制 `scripts/md-sections` 到 skill 内，优先使用项目自带版本，skill 版本作为兜底
- **代码级证据优先**：基于学术研究（Zhou 2023），验证时优先使用 grep/AST/编译结果，而非 LLM 主观判断

## Capabilities

### New Capabilities

- `doc-sync-environment-probing`: 环境感知能力——探测项目文档体系标准、模板结构、文档-代码映射规则，构建项目文档模型，项目规则覆盖 skill 基线
- `doc-sync-consensus-verification`: 共识验证能力——多独立子 Agent 对同一维度串行验证，连续 N 次零问题通过，支持 L0/L1/L2/L3 四级同步深度
- `doc-sync-dimension-checks`: 10 维验证能力——定义 D1-D10 各维度的检查步骤、子 Agent prompt 模板、修复权限和通过标准

### Modified Capabilities

（无现有 spec 需要修改）

## Impact

- **新增文件**：`~/.config/opencode/skills/contract-doc-sync/` 目录（SKILL.md + references/ + scripts/）
- **复制文件**：`scripts/md-sections` → skill 内 `scripts/md-sections`（兜底用，不修改项目原文件）
- **无代码变更**：本 skill 纯文档+脚本，不修改任何 Java 源码或项目配置
- **依赖**：OpenCode skill 系统、git、bash、Python 3
- **学术参考**：Tan 2022/2023（漂移检测）、Radmanesh 2024（不一致→Bug）、Li 2026（自验证悖论）、Zhou 2023（代码级验证）、Avizienis 1985（N-Version Programming）
