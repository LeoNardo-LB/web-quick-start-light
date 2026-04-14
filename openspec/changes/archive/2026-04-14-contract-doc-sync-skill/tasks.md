## 1. 准备与初始化

- [x] 1.1 确认 test-driven-development、dispatching-parallel-agents 技能已加载（未加载则加载，已加载则跳过）
- [x] 1.2 创建分支 `feature/contract-doc-sync-skill`
- [x] 1.3 使用 skill-creator 的 `init_skill.py` 初始化 skill 骨架：`python3 ~/.config/opencode/skills/skill-creator/scripts/init_skill.py contract-doc-sync --path ~/.config/opencode/skills --resources scripts,references`

## 2. 核心脚本

- [x] 2.1 复制 `scripts/md-sections` 到 `~/.config/opencode/skills/contract-doc-sync/scripts/md-sections`，确认可执行权限
- [x] 2.2 编写 `scripts/detect-changes.py`：解析 git diff 输出，按文件路径分类变更（controller/facade/service/repository/entity/config/client/common），输出 JSON 结构化报告（变更文件列表+变更类型+影响的模块），覆盖场景：新增/修改/删除文件、pom.xml 版本变更、新 Maven 模块

## 3. 环境感知模块（references/environment-probing.md）

- [x] 3.1 编写 `references/environment-probing.md`：定义 README.md glob 探测规则（`**/README.md` 路径分类映射表）、关键文件探测清单（AGENTS.md/CLAUDE.md/docs/README.md/pom.xml/package.json）、ProjectDocProfile 输出格式、项目规则覆盖 skill 基线的合并策略、退化策略（无法识别时使用通用基线）

## 4. 验证维度定义（references/verification-dimensions.md）

- [x] 4.1 编写 `references/verification-dimensions.md`：定义 D1-D10 每个维度的名称、检查目标、真相源、检查步骤、问题判定标准（Error/Warning）、修复权限、子 Agent prompt 模板（完全自包含，含角色定义、检查步骤、输出格式）
- [x] 4.2 重点设计 D3（实体/类图一致性）的 prompt：字段匹配、方法匹配、关系线检查、Mermaid 语法正确性验证——此维度"没得商量"，必须严格
- [x] 4.3 设计 D10（语义一致性，仅L3）的 prompt：LLM 语义比对策略，Warning 在 L3 中计为问题

## 5. 同步操作规程（references/sync-procedures.md）

- [x] 5.1 编写 `references/sync-procedures.md`：定义通用基线的代码→文档映射规则（controller/ → API参考、entity/ → 技术设计、config/ → 配置参考等）、🤖/🤖👤/👤 三级维护策略的具体操作步骤、修复操作的具体方法（使用 md-sections 精准操作）

## 6. SKILL.md 核心

- [x] 6.1 编写 SKILL.md frontmatter：name=contract-doc-sync，description 含 L0/L1/L2/L3 四个级别的触发词
- [x] 6.2 编写 SKILL.md Phase 0（环境感知）：引用 references/environment-probing.md，定义探测流程和项目模型构建
- [x] 6.3 编写 SKILL.md Phase 1（发现变更）：调用 detect-changes.py，输出结构化变更分类
- [x] 6.4 编写 SKILL.md L0 流程：Phase 0 + Phase 1 → 漂移报告，不修复，不启动子 Agent
- [x] 6.5 编写 SKILL.md L1 流程：+ Phase 2 同步修复 + Phase 3 快扫验证（每维 1 Agent，忽略 Warning）
- [x] 6.6 编写 SKILL.md L2 流程（默认）：+ Phase 3 共识验证（连续 2 次通过，忽略 Warning，无上限）
- [x] 6.7 编写 SKILL.md L3 流程：+ Phase 3 共识验证×2 轮 + Phase 4 全局仔细扫描，含 Warning
- [x] 6.8 编写 SKILL.md 共识验证协议：调度规则（维度并行、同维度串行）、子 Agent 隔离规则、连续通过/归零逻辑、报告格式模板
- [x] 6.9 确认 SKILL.md 总行数 < 500 行（ Progressive Disclosure 原则）

## 7. 验证与测试

- [x] 7.1 手动加载 skill 验证：启动 OpenCode，确认 skill 出现在可用列表中，description 正确显示 L0/L1/L2/L3 触发词
- [x] 7.2 L0 冒烟测试：在当前项目执行 L0，验证漂移报告输出正确（检测到文档、识别到代码变更）
- [x] 7.3 L1 冒烟测试：在当前项目执行 L1，验证快扫验证流程（10 维度各 1 子 Agent，修复 Error）
- [x] 7.4 验证 md-sections 兜底：临时重命名项目 scripts/md-sections，确认 skill 使用自带版本成功
- [x] 7.5 验证环境感知：确认探测到本项目的三轨体系、模板结构、md-sections 工具

## 8. 文档同步与收尾

- [x] 8.1 进行 artifact 文档、讨论结果的一致性检查
- [x] 8.2 更新 AGENTS.md 文档索引表（如有新 skill 需要引用）— 全局 skill，无需索引到 AGENTS.md
- [x] 8.3 提交代码到分支，准备合并
