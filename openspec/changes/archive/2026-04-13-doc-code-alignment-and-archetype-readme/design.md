## Context

项目已完成 7 轮 OpenSpec change，代码功能稳定，115 个测试全部通过。经全面审计发现 9 处文档与代码的偏差，同时骨架使用说明存在 `README_.md` 与 `README.md` 内容重叠的问题。

当前状态：
- `README_.md`：骨架使用说明，存在过时信息（版本 1.0.0、Windows 专用路径 `_output\archetype\`、标题层级混乱）
- `README.md`：项目主文档，但尾部包含了与 `README_.md` 重复的骨架使用章节
- `AGENTS.md`：AI 编码规范入口，项目结构索引不完整，LoggingProperties 前缀描述与代码不一致
- 代码中存在 3 处死代码：`MethodExecutionLog`、`ResultCode`、`BaseResult.requestId`

## Goals / Non-Goals

**Goals:**

- 重命名 `README_.md` → `ARCHETYPE_README.md` 并重写为 AI 友好的骨架使用文档
- 消除 README.md 与骨架文档的内容重叠（单一信源原则）
- 修正所有已确认的文档-代码偏差（9 处）
- 清理 3 处死代码
- 补全 AGENTS.md 项目结构索引

**Non-Goals:**

- 不重构现有代码架构（仅删除死代码）
- 不修改任何公开 API 或接口
- 不新增功能或依赖
- 不修改 client-log 的 Properties 前缀（保持 `logging`，修正文档即可）
- 不处理 ArchUnit 规则范围扩大的问题（当前规则足够守护核心约束）
- 不修改 docs/backend/ 下的编码规范文档（与代码已对齐）

## Decisions

### D1: ARCHETYPE_README.md 文档结构

**选择**：采用「元数据块 → 生成项目结构 → 安装步骤 → 使用方式 → 故障排查」的结构

**理由**：
- AI 友好：元数据块让 AI 助手无需解析全文即可获得关键技术参数
- 开发者友好：结构化表格替代了原始的错误信息 dump
- 跨平台：macOS/Linux 和 Windows 命令分别给出，不再混用

**替代方案**：保持现有 README_.md 的结构（被否决，因为标题层级混乱、信息过时）

### D2: README.md 骨架部分处理

**选择**：删除 README.md 中的骨架安装/使用/配置章节，替换为一行链接指向 ARCHETYPE_README.md

**理由**：单一信源原则。两份文档维护同一信息会导致同步风险。

### D3: LoggingProperties 前缀对齐方向

**选择**：修正文档，将 `middleware.logging` 改为 `logging`

**理由**：
- `logging.slow-query` 和 `logging.sampling` 更符合 Spring Boot 的日志配置惯例
- 其他 5 个客户端使用 `middleware.*` 是因为它们是自定义中间件，而日志模块是 Spring Boot 日志体系的扩展
- 改动文档比改动代码的风险低（无需修改 yaml 配置和 Properties 类）

### D4: 死代码清理策略

**选择**：直接删除 3 处死代码

| 文件 | 删除理由 |
|------|----------|
| `MethodExecutionLog.java` | LogAspect 不创建也不引用此对象，纯死代码 |
| `ResultCode.java` | 已标注 `@Deprecated`，与 `CommonErrorCode` 功能完全重叠 |
| `BaseResult.requestId` 字段 | 声明但从未赋值，始终为 null |

**理由**：三处死代码均无外部引用，删除后编译和测试不受影响。与其在文档中标注"预留/废弃"，不如直接清理保持代码整洁。

### D5: BaseDO 位置在文档中的描述

**选择**：文档跟随代码实际位置，归入 `repository/` 索引

**理由**：BaseDO 位于 `app/.../repository/BaseDO.java`，虽然语义上是数据对象，但代码实际在 repository 包下。文档应描述代码"是什么"而非"应该是什么"。

### D6: 文档修改范围控制

**选择**：仅修改与偏差直接相关的文档，不进行全量重写

**受影响文档清单**：
- `AGENTS.md` — 索引补充、前缀修正、Hutool 状态、BaseDO 位置
- `docs/architecture/overview.md` — 同步 LoggingProperties 前缀、项目结构
- `docs/architecture/dependency-rules.md` — 无实质变更（ArchUnit 范围不扩大）
- `docs/README.md` — 如有新文档/包需要索引则更新

**不修改的文档**：
- `docs/backend/coding-standards.md` — 已对齐
- `docs/backend/error-handling.md` — 已对齐
- `docs/backend/testing-guide.md` — 已对齐
- `docs/backend/performance-rules.md` — 已对齐
- `docs/workflow/*.md` — 不受影响
- `docs/guides/getting-started.md` — 不受影响

## Risks / Trade-offs

| 风险 | 等级 | 缓解措施 |
|------|------|----------|
| 删除死代码后可能存在间接引用 | 低 | 已确认无编译依赖，`mvn test` 可验证 |
| ARCHETYPE_README.md 中的 Maven archetype 命令未经验证 | 中 | 命令基于 Maven 官方文档和项目实际 pom.xml，使用标准参数 |
| 文档更新遗漏某些偏差 | 低 | 审计已覆盖所有文档和代码，偏差清单完整 |
| README.md 删除骨架部分后用户找不到骨架文档 | 低 | 链接指向 ARCHETYPE_README.md，文件名直观 |
