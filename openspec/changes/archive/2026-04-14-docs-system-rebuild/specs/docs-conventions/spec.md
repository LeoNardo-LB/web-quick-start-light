## ADDED Requirements

### Requirement: java-conventions.md 创建能力
- **WHEN** 开发者需要了解 Java 编码规范
- **THEN** 系统 MUST 在 docs/conventions/ 下创建 java-conventions.md，包含 Lombok 规范（禁止 @Data，使用 @Builder + @RequiredArgsConstructor）、时间类型规范（统一 Instant，禁止 LocalDateTime/Long 时间戳）、Record 规范（DTO/VO 使用 record，基类保持 class）、命名规范（测试命名 UTest/ITest）和依赖约束（ORM 仅 MyBatis-Plus，工具库优先 Hutool）

#### Scenario: Lombok 规则验证
- **WHEN** 开发者查阅 java-conventions.md
- **THEN** 文档 MUST 列出 Lombok 允许注解白名单（@Builder, @RequiredArgsConstructor, @Getter, @Setter, @Slf4j, @AllArgsConstructor）和禁止注解黑名单（@Data, @EqualsAndHashCode, @ToString 单独使用），每个禁止项 MUST 附带原因说明

### Requirement: testing-conventions.md 创建能力
- **WHEN** 开发者需要了解测试编写规范
- **THEN** 系统 MUST 在 docs/conventions/ 下创建 testing-conventions.md，包含测试分类（UTest 单元测试继承 UnitTestBase、ITest 集成测试继承 IntegrationTestBase）、Mock 规范（UTest 使用 Mockito、ITest 使用真实 Spring 上下文）、覆盖率标准（Instruction ≥ 95%、Branch ≥ 90%）和测试命令参考

#### Scenario: 测试覆盖率标准
- **WHEN** 开发者查阅 testing-conventions.md
- **THEN** 文档 MUST 展示 JaCoCo 覆盖率阈值表（Instruction/Branch 两维度），包含当前阈值和目标阈值，MUST 说明覆盖率报告生成命令（`mvn clean verify`）和报告查看路径

### Requirement: error-handling.md 创建能力
- **WHEN** 开发者需要了解异常处理和错误码规范
- **THEN** 系统 MUST 在 docs/conventions/ 下创建 error-handling.md，包含异常体系类图（ErrorCode 接口 → BaseException → BizException/ClientException/SysException）、CommonErrorCode 完整枚举表、i18n 错误消息机制（messageKey + MessageSource + Accept-Language）和全局异常处理器（WebExceptionAdvise）说明

#### Scenario: 错误码完整性
- **WHEN** 开发者查阅 error-handling.md
- **THEN** CommonErrorCode 表格 MUST 包含全部枚举值（含 RATE_LIMIT_EXCEEDED/6501、AUTH_UNAUTHORIZED/6601、AUTH_BAD_CREDENTIALS/6602、AUTH_USER_NOT_FOUND/6603），每个错误码 MUST 包含 code、message、messageKey 三列

### Requirement: configuration.md 创建能力
- **WHEN** 开发者需要了解配置管理规范
- **THEN** 系统 MUST 在 docs/conventions/ 下创建 configuration.md，包含配置类规范（使用 @ConfigurationProperties，禁止 @Value）、middleware.* 统一前缀说明（列出全部 9 个客户端配置前缀）、多环境配置说明（dev/prod/optional）和配置属性类参考表

#### Scenario: 配置前缀参考表
- **WHEN** 开发者需要添加新的客户端模块配置
- **THEN** configuration.md MUST 提供完整的 middleware.* 配置前缀表，每个条目包含前缀路径（如 middleware.cache）、对应的 Properties 类名和关键配置项说明

### Requirement: conventions 文档统一外壳模板
- **WHEN** 维护者创建或更新 conventions/ 下的任何文档
- **THEN** 每个文档 MUST 使用统一固定外壳模板：`📋 目录`（锚点链接到所有二级标题）→ 标题 → 规范目的（一句话说明此规范存在的理由） → 规则列表（每条规则包含规则描述、正确示例代码、错误示例代码和规则编号） → 检查清单（可用于 Code Review） → 相关文档链接，且每个文档 MUST 在顶部包含所属轨道标签（Constraint 轨）

#### Scenario: 外壳模板结构验证
- **WHEN** 审查 conventions/ 下的任意文档
- **THEN** 文档 MUST 在开头包含 `📋 目录` 章节（列出所有二级标题的锚点链接），MUST 包含 `# <文档标题>` 一级标题、`## 规范目的` 章节（一句话）、`## 规则` 章节（编号列表，每条含代码示例）、`## 检查清单` 章节（checkbox 格式）和 `## 相关文档` 章节
