## Why

经过多轮架构增强（103 个任务）和测试真实性转换后，项目文档严重滞后于代码实际状态。3 个文档描述了"三层架构"（实际已为四层）、1 个文档声称"5 个技术客户端"（实际 9 个）、error-handling 文档缺少 i18n 机制和 AUTH 错误码、testing-guide 引用了已删除的测试文件。同时 ARCHETYPE_README.md 混淆了"骨架安装教程"和"功能展示"两个角色，需要精简为纯安装指南。

## What Changes

- **ARCHETYPE_README.md 精简**：删除"内置功能"表格和详细项目结构树，只保留骨架元数据、安装步骤、创建项目、排除文件、故障排查
- **docs/README.md 修复**：修正"5 个客户端"→9 个、"三层架构"→四层，补全缺失模块
- **docs/architecture/dependency-rules.md 重写**：三层→四层架构、补 Facade 层规则、补全 9 个 client 依赖图、更新 ArchUnit 规则列表
- **docs/backend/error-handling.md 更新**：补 ErrorCode.messageKey()、补 4 个新增错误码（6501/6601/6602/6603）、补 i18n 机制说明（Accept-Language + MessageSource）
- **docs/backend/testing-guide.md 更新**：删除已不存在的测试文件引用、Facade 测试模式从 Mockito 示例改为 ITest 示例、补 Facade 集成测试模式章节

## Capabilities

### New Capabilities

（无新增能力）

### Modified Capabilities

- `agents-md-and-docs`：更新 AGENTS.md 和所有 docs/ 下文档，使其与当前代码架构对齐
- `archetype-readme`：精简为纯骨架安装教程，删除功能展示内容

## Impact

- **文档文件**：ARCHETYPE_README.md + docs/ 下 5 个文件需要修改
- **无代码变更**：纯文档修改，不影响构建和测试
- **无 API 变更**：不影响任何接口
