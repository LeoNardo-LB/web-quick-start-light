## 1. 准备工作

- [x] 1.1 确认 test-driven-development、dispatching-parallel-agents 技能已加载（未加载则加载，已加载则跳过）

## 2. 死代码清理

- [x] 2.1 删除 `clients/client-log/src/main/java/org/smm/archetype/client/log/MethodExecutionLog.java`（LogAspect 未使用）
- [x] 2.2 删除 `app/src/main/java/org/smm/archetype/entity/enums/ResultCode.java`（@Deprecated，与 CommonErrorCode 重叠）
- [x] 2.3 从 `app/src/main/java/org/smm/archetype/entity/api/BaseResult.java` 中删除 `requestId` 字段及其 getter/setter
- [x] 2.4 执行 `mvn test` 确认编译通过、全部测试通过

## 3. 文件重命名与 ARCHETYPE_README.md 重写

- [x] 3.1 使用 `git mv README_.md ARCHETYPE_README.md` 重命名文件
- [x] 3.2 重写 ARCHETYPE_README.md 内容，按 archetype-readme spec 要求包含：元数据块、生成项目结构、安装步骤（macOS/Linux + Windows）、使用方式（IDEA + 命令行）、排除文件说明、故障排查表

## 4. README.md 骨架部分清理

- [x] 4.1 删除 README.md 中「本地安装骨架」「使用骨架」「骨架配置信息」「常见错误」四个章节
- [x] 4.2 在 README.md 适当位置添加链接指向 ARCHETYPE_README.md（如「骨架使用方式详见 [ARCHETYPE_README.md](ARCHETYPE_README.md)」）

## 5. AGENTS.md 文档-代码对齐

- [x] 5.1 修正技术栈表格中 Hutool 的说明：从"暂未使用"改为"已使用（SensitiveLogUtils 使用 StrUtil）"
- [x] 5.2 修正技术客户端规范章节：LoggingProperties 前缀从 `middleware.logging` 改为 `logging`，注明与其他客户端不同的原因
- [x] 5.3 修正项目结构索引：BaseDO 从 `entity/` 行移至 `repository/` 行
- [x] 5.4 补充项目结构索引：添加 `controller/global/`（ContextFillFilter + WebExceptionAdvise）
- [x] 5.5 补充项目结构索引：添加 `controller/test/`（TestController）
- [x] 5.6 补充项目结构索引：添加 `entity/api/`（BaseResult、BasePageResult、BaseRequest、BasePageRequest）
- [x] 5.7 补充项目结构索引：添加 `config/properties/`（AppInfoProperties、ThreadPoolProperties）
- [x] 5.8 补充项目结构索引：添加 `util/dal/`（MyMetaObjectHandler）
- [x] 5.9 删除对 `entity/enums/ResultCode` 的引用（如存在）

## 6. docs/ 同步更新

- [x] 6.1 更新 `docs/architecture/overview.md`：Properties 前缀表从 `middleware.logging` 改为 `logging`
- [x] 6.2 更新 `docs/architecture/overview.md`：app 模块结构树补充 controller/test/、entity/api/、config/properties/、util/dal/
- [x] 6.3 更新 `docs/architecture/overview.md`：删除对 entity/enums/ResultCode 的引用（如存在）
- [x] 6.4 检查 `docs/README.md` 是否需要更新索引条目（如有新包或变更则更新）

## 7. 验证

- [x] 7.1 执行 `mvn clean test` 确认编译通过、全部测试通过
- [x] 7.2 检查所有修改的文档中不存在 `middleware.logging`（应为 `logging`）
- [x] 7.3 检查所有修改的文档中不存在"暂未使用"（Hutool 说明）
- [x] 7.4 确认 README.md 不包含骨架安装/使用/配置章节
- [x] 7.5 确认 ARCHETYPE_README.md 包含元数据块、安装步骤、使用方式、故障排查表

## 8. 一致性检查

- [x] 8.1 进行 artifact 文档、讨论结果的一致性检查
