## Phase 1: docs/README.md 结构性重构

> 对应 design.md D1（结构重组）、D2（维护职责三级分类）、D3（编写原则 7 维度）
> 对应 capability: docs-governance, docs-writing-principles

- [x] 1.1 确认 test-driven-development、dispatching-parallel-agents 技能已加载（未加载则加载，已加载则跳过）
- [x] 1.2 在 `## 三轨文档体系` 之后新增 `## 文档编写原则` 章节，包含 7 个维度（职责清晰、简要精准、层次分明、逻辑自洽、科学有据、双范式写作、索引具体规则泛化），每个维度含面向 AI 和面向人类的对比说明（双范式写作用表格呈现）
- [x] 1.3 将 `## 文档系统设计` 下的 5 个治理子章节（维护策略、反模式警示、文档与代码对齐机制、可演进性设计、与 AI Agent 的协同设计）从该章节中移除，保持"文档系统设计"仅含设计哲学内容
- [x] 1.4 新增 `## 文档治理` L2 章节，包含 5 个 L3 子章节：① `### 维护职责分工`（🤖/🤖👤/👤 三级分类表格，每级含触发场景和操作描述）② `### 文档与代码对齐机制`（三轨各自的对齐流程、触发场景、一致性漂移检测）③ `### 维护策略`（扩展表格：增加维护者列和验证方式列，覆盖所有常见场景）④ `### 反模式警示`（扩展为 4 类：文档结构反模式、AI 维护反模式、维护遗漏反模式、内容膨胀反模式，每类 2-3 条）⑤ `### 可演进性设计`（每个演进方向增加触发条件和实施步骤，增加演进决策树）
- [x] 1.5 新增 `## 与 AI Agent 协同` L2 章节（从原 L3 提升），扩展内容包含：AGENTS.md 定位说明、docs/ 对 AI 的价值、AI 可自主维护的文档范围（确定性维护清单）、AI 需人类确认的维护范围（半确定性维护清单）
- [x] 1.6 更新 `📋 目录` 章节，反映新增和重组后的章节结构
- [x] 1.7 更新 `## 相关文档` 章节，确保 Constraint 轨包含 `AGENTS.md` + `conventions/README.md`，Contract 轨包含三个子目录 README
- [x] 1.8 验证 docs/README.md 结构：执行 `scripts/md-sections docs/README.md`，确认存在"文档编写原则"、"文档治理"、"与 AI Agent 协同"三个 L2 章节，且"文档系统设计"下不含治理子章节

## Phase 2: AGENTS.md 文档维护职责引用

> 对应 design.md D2（维护职责三级分类在 AGENTS.md 中的体现）
> 对应 capability: agents-md-and-docs

- [x] 2.1 在 AGENTS.md 的 `## 文档索引` 之前新增 `## 文档维护职责` 段落，包含以下章节级锚点引用：文档编写原则（`docs/README.md#文档编写原则`）、维护职责分工（`docs/README.md#维护职责分工`）、对齐机制（`docs/README.md#文档与代码对齐机制`）、维护策略（`docs/README.md#维护策略`）、反模式警示（`docs/README.md#反模式警示`）
- [x] 2.2 验证 AGENTS.md 结构：执行 `scripts/md-sections AGENTS.md`，确认"文档维护职责"章节位于"文档索引"之前

## Phase 3: 模板升级 + 三轨标签统一

> 对应 design.md D4（三轨标签呈现）、D5（modules 模板固定层扩展）
> 对应 capability: docs-template-upgrade

- [x] 3.1 升级 `docs/modules/README.md` 模板固定层：从 6 章节扩展为 8 章节（新增"相关文档"和"变更历史"），模板一级标题格式改为 `# <模块名> — Contract 轨`，标题下方增加 blockquote `> 代码变更时必须同步更新本文档`
- [x] 3.2 更新 `docs/modules/README.md` 的"编写检查清单"：增加"包含相关文档章节"和"包含变更历史章节"和"标题包含 — Contract 轨 标记"三个检查项
- [x] 3.3 升级 `docs/conventions/README.md` 模板半固定层：增加"设计理由"、"常见违规场景"、"关联规则"三个可选扩展内容说明
- [x] 3.4 升级 `docs/architecture/README.md` 模板半固定层：增加"设计考量"和"系统边界"两个可选扩展内容建议
- [x] 3.5 验证模板更新：对三个 README.md 分别执行 `scripts/md-sections` 验证新结构

## Phase 4: 各文档内容扩展

> 对应 design.md D7（纵向深挖+横向关联）
> 对应 capability: docs-content-expansion

### 4A. modules/ 文档补全与扩展（12 个文件）

- [x] 4A.1 为 `docs/modules/auth.md` 补全"相关文档"章节（链接到 client-auth.md、request-lifecycle.md、error-handling.md）、补全"变更历史"章节、标题增加 `— Contract 轨` 标记、扩展业务场景至 3+ 个用例、扩展使用指南至 2+ 个场景代码示例
- [x] 4A.2 为 `docs/modules/system-config.md` 补全"相关文档"章节、补全"变更历史"章节、标题增加 `— Contract 轨` 标记、扩展业务场景和使用指南
- [x] 4A.3 为 `docs/modules/operation-log.md` 补全"相关文档"章节、补全"变更历史"章节、标题增加 `— Contract 轨` 标记、扩展业务场景和使用指南
- [x] 4A.4 为 `docs/modules/client-cache.md` 补全"相关文档"章节、补全"变更历史"章节、标题增加 `— Contract 轨` 标记、扩展技术设计中的"设计考量"（为什么用 Caffeine 而非 Redis）、扩展使用指南多场景
- [x] 4A.5 为 `docs/modules/client-oss.md` 补全"相关文档"章节、补全"变更历史"章节、标题增加 `— Contract 轨` 标记、扩展业务场景和使用指南
- [x] 4A.6 为 `docs/modules/client-email.md` 补全"相关文档"章节、补全"变更历史"章节、标题增加 `— Contract 轨` 标记、扩展业务场景和使用指南
- [x] 4A.7 为 `docs/modules/client-sms.md` 补全"相关文档"章节、补全"变更历史"章节、标题增加 `— Contract 轨` 标记、扩展业务场景和使用指南
- [x] 4A.8 为 `docs/modules/client-search.md` 补全"相关文档"章节、补全"变更历史"章节、标题增加 `— Contract 轨` 标记、扩展业务场景和使用指南
- [x] 4A.9 为 `docs/modules/client-log.md` 补全"相关文档"章节、补全"变更历史"章节、标题增加 `— Contract 轨` 标记、扩展业务场景和使用指南
- [x] 4A.10 为 `docs/modules/client-ratelimit.md` 补全"相关文档"章节、补全"变更历史"章节、标题增加 `— Contract 轨` 标记、扩展业务场景和使用指南
- [x] 4A.11 为 `docs/modules/client-idempotent.md` 补全"相关文档"章节、补全"变更历史"章节、标题增加 `— Contract 轨` 标记、扩展业务场景和使用指南
- [x] 4A.12 为 `docs/modules/client-auth.md` 补全"相关文档"章节、补全"变更历史"章节、标题增加 `— Contract 轨` 标记、扩展业务场景和使用指南
- [x] 4A.13 验证 modules/ 全部 12 个文件：执行批量 `scripts/md-sections <file> "相关文档"` 和 `scripts/md-sections <file> "变更历史"` 和 `scripts/md-sections <file>`（检查标题包含 — Contract 轨）

### 4B. conventions/ 文档内容扩展（4 个文件）

- [x] 4B.1 为 `docs/conventions/java-conventions.md` 扩展：每条 ⛔ MUST 规则增加"设计理由"段落、增加 2+ 个"常见违规场景"段落、规则间交叉引用
- [x] 4B.2 为 `docs/conventions/testing-conventions.md` 扩展：每条 ⛔ MUST 规则增加"设计理由"段落、增加 2+ 个"常见违规场景"段落
- [x] 4B.3 为 `docs/conventions/error-handling.md` 扩展：每条 ⛔ MUST 规则增加"设计理由"段落、增加 2+ 个"常见违规场景"段落
- [x] 4B.4 为 `docs/conventions/configuration.md` 扩展：每条 ⛔ MUST 规则增加"设计理由"段落、增加 2+ 个"常见违规场景"段落

### 4C. architecture/ 文档内容扩展（6 个文件）

- [x] 4C.1 为 `docs/architecture/system-overview.md` 增加"系统边界"说明段（说明系统覆盖和不覆盖的范围）
- [x] 4C.2 为 `docs/architecture/request-lifecycle.md` 增加过滤器链完整列表细节
- [x] 4C.3 为 `docs/architecture/design-patterns.md` 增加"设计考量"段（为什么选择 Template Method 而非 Strategy 模式等）
- [x] 4C.4 为 `docs/architecture/module-structure.md` 适当扩展设计考量
- [x] 4C.5 为 `docs/architecture/thread-context.md` 适当扩展设计考量
- [x] 4C.6 为 `docs/architecture/archetype-usage.md` 适当扩展故障排查场景

## Phase 5: 一致性校验

- [x] 5.1 全量结构验证：对所有 28 个文档执行 `scripts/md-sections <file>` 检查结构完整性（标题格式、📋 目录章节、相关文档/变更历史章节的存在性）
- [x] 5.2 交叉引用验证：检查所有"相关文档"章节中的链接是否指向真实存在的文件和章节
- [x] 5.3 标题三轨标签验证：对所有 modules/ 和 architecture/ 文件执行 `scripts/md-sections <file>` 确认标题包含正确的三轨标签
- [x] 5.4 内容职责边界验证：检查 modules/ client-* 文档的技术设计章节中 Template Method 模式说明仅为引用链接（如"详见 [设计模式](../../architecture/design-patterns.md)"），而非完整描述；检查 conventions/ 文档中无与 modules/ 重复的业务逻辑描述
