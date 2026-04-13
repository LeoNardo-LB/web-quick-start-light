## MODIFIED Requirements

### Requirement: LoggingProperties 前缀修正
AGENTS.md 中 client-log 的 Properties 前缀 SHALL 从 `middleware.logging` 修正为 `logging`。文档 SHALL 说明 client-log 使用 `logging` 前缀的原因（对接 Spring Boot 日志配置惯例），与其他 5 个客户端的 `middleware.*` 前缀形成明确区分。

#### Scenario: AGENTS.md LoggingProperties 前缀与代码一致
- **WHEN** 阅读 AGENTS.md 的技术客户端规范章节的 Properties 前缀统一表
- **THEN** client-log 行显示前缀为 `logging`（而非 `middleware.logging`），且注明原因

### Requirement: Hutool 状态更新
AGENTS.md 技术栈表格中 Hutool 的说明 SHALL 从"暂未使用"改为"已使用（SensitiveLogUtils 使用 StrUtil）"。

#### Scenario: AGENTS.md Hutool 状态与代码实际使用一致
- **WHEN** 阅读 AGENTS.md 技术栈表格中的 Hutool 行
- **THEN** 说明中包含"已使用"字样，不再出现"暂未使用"

### Requirement: 项目结构索引补全
AGENTS.md 的项目结构索引表 SHALL 补充以下当前遗漏的包和关键文件：
- `controller/global/` — ContextFillFilter + WebExceptionAdvise
- `controller/test/` — TestController（4 个测试端点）
- `entity/api/` — BaseResult、BasePageResult、BaseRequest、BasePageRequest
- `config/properties/` — AppInfoProperties、ThreadPoolProperties
- `util/dal/` — MyMetaObjectHandler

#### Scenario: AGENTS.md 索引覆盖所有关键包
- **WHEN** 对照代码实际包结构阅读 AGENTS.md 项目结构索引
- **THEN** controller/global/、controller/test/、entity/api/、config/properties/、util/dal/ 均在索引中列出

### Requirement: BaseDO 位置修正
AGENTS.md 项目结构索引中 BaseDO 的位置 SHALL 从 `entity/` 修正为 `repository/`（与代码实际位置 `app/.../repository/BaseDO.java` 一致）。

#### Scenario: AGENTS.md BaseDO 位置与代码一致
- **WHEN** 阅读 AGENTS.md 项目结构索引
- **THEN** BaseDO 出现在 `repository/` 行（而非 `entity/` 行）

### Requirement: 骨架文档单一信源
README.md 中 SHALL 删除「本地安装骨架」「使用骨架」「骨架配置信息」三个章节，替换为指向 ARCHETYPE_README.md 的一行链接。

#### Scenario: README.md 不再包含骨架安装步骤
- **WHEN** 阅读 README.md
- **THEN** 不包含骨架安装/使用/配置章节，包含指向 ARCHETYPE_README.md 的链接

### Requirement: 死代码清理后文档更新
AGENTS.md 和相关文档 SHALL 删除对以下已清理代码的引用：
- `entity/enums/ResultCode`（已删除，使用 CommonErrorCode 代替）

#### Scenario: AGENTS.md 不再引用已删除的 ResultCode
- **WHEN** 阅读 AGENTS.md 的项目结构索引
- **THEN** 不包含 `entity/enums/` 或 `ResultCode` 条目（或标注为已移除）

### Requirement: overview.md 同步更新
docs/architecture/overview.md SHALL 同步以下变更：
- LoggingProperties 前缀表从 `middleware.logging` 改为 `logging`
- app 模块结构树补充 `controller/test/`、`entity/api/`、`config/properties/`、`util/dal/`
- 删除对 `entity/enums/ResultCode` 的引用

#### Scenario: overview.md 与 AGENTS.md 保持一致
- **WHEN** 对比 overview.md 和 AGENTS.md 中的 LoggingProperties 前缀
- **THEN** 两者一致，均显示 `logging`

## REMOVED Requirements

### Requirement: 客户端描述更新（Aliyun SDK / Elasticsearch 实现）
**Reason**：该需求来自早期 change（假设会引入 Aliyun SDK 和 Elasticsearch），但实际代码使用 NoOp 和 Simple 实现。本次变更 SHALL 将现有 spec 中关于 Aliyun SDK 和 Elasticsearch 的描述修正为与代码一致的 NoOp/Simple 实现。
**Migration**：使用 `CommonErrorCode` 中已有的客户端模块描述（NoOpEmailClient、NoOpSmsClient、SimpleSearchClient）。
