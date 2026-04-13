## Why

项目经过 7 轮 OpenSpec change 的迭代开发，代码与文档之间存在 9 处已确认的偏差（Properties 前缀不一致、Hutool 状态声明过时、项目结构索引不完整、死代码未清理等）。同时，骨架使用说明分散在 `README_.md` 和 `README.md` 两处，内容重叠且 `README_.md` 存在过时信息（版本号、Windows 路径、标题层级错误）。在项目稳定后，需要对齐文档与代码的真实状态，并编写 AI 友好的骨架使用文档，确保项目交付物准确可信。

## What Changes

- **重命名** `README_.md` → `ARCHETYPE_README.md`，删除其中过时的骨架文档内容，重新编写为 AI 友好的骨架使用说明
- **README.md 删除骨架部分**，改为链接指向 `ARCHETYPE_README.md`（单一信源原则）
- **修正文档偏差**：LoggingProperties 前缀从 `middleware.logging` 改为 `logging`、Hutool 状态从"暂未使用"改为"已使用"
- **补充索引**：AGENTS.md 项目结构索引补充 `controller/global/`、`controller/test/`、`entity/api/`、`entity/enums/`、`config/properties/`、`util/dal/` 等未记录的包及关键文件
- **修正 BaseDO 位置**：文档索引从 `entity/` 修正为 `repository/`（跟随代码实际位置）
- **清理死代码**：删除 `MethodExecutionLog`（未被 LogAspect 使用）、`ResultCode`（@Deprecated，与 CommonErrorCode 重叠）、`BaseResult.requestId` 字段（声明但从未赋值）
- **同步其他文档**：overview.md、dependency-rules.md、docs/README.md 等根据上述变更进行一致性更新

## Capabilities

### New Capabilities

- `archetype-readme`: AI 友好的 Maven 骨架使用文档（ARCHETYPE_README.md），包含元数据块、生成项目结构预览、安装步骤、使用方式、自定义参数、故障排查表

### Modified Capabilities

- `agents-md-and-docs`: 修正 LoggingProperties 前缀、Hutool 状态、项目结构索引完整性、BaseDO 位置、死代码清理后的文档更新

## Impact

- **文件重命名**：`README_.md` → `ARCHETYPE_README.md`（git mv）
- **文档修改**：`README.md`、`AGENTS.md`、`docs/architecture/overview.md`、`docs/architecture/dependency-rules.md`、`docs/README.md`
- **代码删除**：`MethodExecutionLog.java`（client-log 模块）、`ResultCode.java`（app entity/enums）、`BaseResult.requestId` 字段
- **无 API 变更**：死代码清理不影响任何公开接口
- **无依赖变更**：纯文档和死代码清理
