## Context

项目经过 103 个架构增强任务 + 测试真实性转换后，代码与文档严重脱节。核心矛盾：
- 代码已为四层架构（Controller → Facade → Service → Repository），但 3 个文档仍说"三层"
- 代码有 9 个 client 模块，但 docs/README.md 说"5 个"
- error-handling 文档缺少 i18n + AUTH 错误码
- testing-guide 引用已删除的测试文件
- ARCHETYPE_README.md 混杂了安装教程和功能展示

## Goals / Non-Goals

**Goals:**
- 所有 docs/ 文档与当前代码完全对齐
- ARCHETYPE_README.md 精简为纯骨架安装教程
- 每个文档描述的四层架构、模块数量、错误码、测试模式均反映当前代码实际状态

**Non-Goals:**
- 不修改任何代码（纯文档变更）
- 不新增功能或重构
- 不修改 openspec/specs/ 下的主 spec 文件（仅创建变更内的 delta specs）
- 不修改 AGENTS.md（它已是最新，由上轮变更维护）

## Decisions

1. **ARCHETYPE_README.md 精简策略**：删除"内置功能"表格（第 19-32 行）和详细项目结构树（第 33-56 行），保留骨架元数据 + 安装步骤 + 创建项目 + 排除文件 + 故障排查
2. **dependency-rules.md 重写策略**：将"三层架构"全部改为"四层架构"，补全 Facade 层规则，更新依赖图包含全部 9 个 client，更新 ArchUnit 规则列表
3. **testing-guide.md 更新策略**：将 Facade 测试示例从 Mockito mock Service 改为 ITest（@Autowired 真实 Facade + 真实 SQLite），删除已不存在的文件引用
4. **error-handling.md 补充策略**：补 ErrorCode.messageKey() + 4 个新增错误码 + i18n 机制（Accept-Language + MessageSource）+ 更新全局异常处理器代码示例

## Risks / Trade-offs

- **风险低**：纯文档变更，无编译/运行时影响
- **唯一注意**：确保文档中的代码示例与实际代码一致，避免误导开发者
