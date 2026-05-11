## MODIFIED Requirements

### Requirement: docs/README.md 模块数量与架构层级
- **WHEN** 开发者阅读 docs/README.md
- **THEN** 文档导航描述必须反映 9 个技术客户端子模块和四层架构（Controller → Facade → Service → Repository）

### Requirement: docs/architecture/module-structure.md 四层架构规则
- **WHEN** 开发者查阅依赖规则文档（`docs/architecture/module-structure.md`）
- **THEN** 文档必须描述四层架构（Controller → Facade → Service → Repository），包含 Facade 层规则（Controller 仅注入 Facade、Facade 仅注入 Service），ArchUnit 规则列表包含 Facade 守护

### Requirement: docs/conventions/error-handling.md 错误码完整性
- **WHEN** 开发者查阅错误处理文档（`docs/conventions/error-handling.md`）
- **THEN** ErrorCode 接口必须包含 messageKey() 方法，CommonErrorCode 表格必须包含全部 16 个枚举值（含 RATE_LIMIT_EXCEEDED/6501、AUTH_UNAUTHORIZED/6601、AUTH_BAD_CREDENTIALS/6602、AUTH_USER_NOT_FOUND/6603），全局异常处理器代码示例必须展示 i18n 机制（Accept-Language + MessageSource）

### Requirement: docs/conventions/testing-conventions.md 测试文件与模式对齐
- **WHEN** 开发者查阅测试指南（`docs/conventions/testing-conventions.md`）
- **THEN** 测试文件组织必须反映当前实际文件（不引用已删除的 Facade UTest），Facade 测试模式必须使用 ITest 示例（@Autowired 真实 Facade + 真实 SQLite），不使用 Mockito mock Service 示例
