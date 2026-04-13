## Tasks

### Chunk 1: ARCHETYPE_README.md 精简

- [x] 1. 删除 ARCHETYPE_README.md 中的"内置功能"表格（当前第 19-32 行）和"生成项目结构"详细树（当前第 33-56 行），仅保留：骨架元数据表、本地安装骨架（3 步骤）、使用骨架创建项目（2 方式）、骨架排除文件表、故障排查表

### Chunk 2: docs/README.md 模块数量与架构修正

- [x] 2. 修正 docs/README.md 第 5 行"5 个技术客户端子模块"为"9 个技术客户端子模块"，修正"三层架构"引用为"四层架构"，确保导航表描述与 docs/architecture/overview.md 一致

### Chunk 3: docs/architecture/dependency-rules.md 四层架构重写

- [x] 3. 重写 dependency-rules.md：(a) 标题和所有"三层架构"→"四层架构" (b) 依赖方向图 `Controller → Facade → Service → Repository` (c) 补 Facade 层规则（Controller 仅注入 Facade、Facade 仅注入 Service、Facade 禁止暴露 Entity） (d) 依赖关系图补全 9 个 client 模块 (e) ArchUnit 规则列表补 Facade 守护规则（controllerShouldOnlyDependOnServiceDirectly、facadeShouldNotDependOnRepository）

### Chunk 4: docs/backend/error-handling.md 补全

- [x] 4. 更新 error-handling.md：(a) ErrorCode 接口定义补 messageKey() 方法 (b) CommonErrorCode 表格补 4 个新增错误码：RATE_LIMIT_EXCEEDED(6501)、AUTH_UNAUTHORIZED(6601)、AUTH_BAD_CREDENTIALS(6602)、AUTH_USER_NOT_FOUND(6603)，总数标为 16 个 (c) 补 i18n 机制章节：ErrorCode.messageKey() 返回 error.{code} 格式键、WebExceptionAdvise 根据 Accept-Language Header 从 MessageSource 解析、messages.properties + messages_en.properties (d) 更新全局异常处理器代码示例展示 i18n 逻辑

### Chunk 5: docs/backend/testing-guide.md 对齐

- [x] 5. 更新 testing-guide.md：(a) 删除测试文件组织中已不存在的 Facade UTest 引用（cases/unittest/facade/system/SystemConfigFacadeUTest、cases/unittest/facade/operationlog/OperationLogFacadeUTest）(b) 将"Facade 单元测试模式"章节改为"Facade 集成测试模式"，示例改为继承 IntegrationTestBase + @Autowired 注入真实 Facade + 使用真实 SQLite 验证 VO 转换 (c) 补充说明：Facade 测试已全部从 Mockito UTest 转为 ITest，使用真实 Spring 上下文 + 真实数据库

### Chunk 6: 验证

- [x] 6. 运行 `mvn clean verify` 确认文档变更未影响构建，所有 219 测试通过
- [x] 7. 进行 artifact 文档与讨论结果的一致性检查
