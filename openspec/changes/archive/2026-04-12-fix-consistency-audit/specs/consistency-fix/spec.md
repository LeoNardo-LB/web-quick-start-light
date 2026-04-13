## ADDED Requirements

### Requirement: 文档端点与实际配置一致
getting-started.md 中的验证端点 SHALL 与 application.yaml 中的实际配置完全一致：端口 9201、context-path /quickstart-light、Swagger UI 路径 /openapi-doc.html。

#### Scenario: 端点信息准确
- **WHEN** 开发者阅读 getting-started.md 的验证端点部分
- **THEN** 所有 URL 使用端口 9201、包含 context-path /quickstart-light、Swagger 路径为 /openapi-doc.html

### Requirement: 多环境配置表使用 SQLite
getting-started.md 多环境配置表中 prod 行 SHALL 显示 SQLite 而非 PostgreSQL。

#### Scenario: prod 数据库类型正确
- **WHEN** 开发者查看 getting-started.md 多环境配置表
- **THEN** prod 行的数据库列为 SQLite

### Requirement: application-optional.yaml 配置键可绑定
application-optional.yaml 中被注释的配置示例 SHALL 使用 `middleware.*` 前缀，字段名 SHALL 与对应 Properties 类的属性名匹配。不存在的依赖配置（如 Redis）SHALL 被删除。

#### Scenario: 取消注释后配置可生效
- **WHEN** 开发者取消 application-optional.yaml 中某段配置的注释并填写值
- **THEN** 配置键能正确绑定到对应 Properties 类的字段

### Requirement: schema.sql 逻辑删除字段与 MyBatis-Plus 配置一致
test 环境 schema.sql 中的逻辑删除字段 SHALL 为 `delete_time BIGINT DEFAULT 0`，与 application-test.yaml 的 `logic-delete-field: delete_time` 一致。

#### Scenario: 逻辑删除查询使用正确字段
- **WHEN** MyBatis-Plus 在 test 环境执行逻辑删除查询
- **THEN** 生成的 SQL 使用 `delete_time` 字段而非 `deleted`

### Requirement: start.sh 日志提示指向实际文件
start.sh 启动完成后的日志查看提示 SHALL 指向 logback-spring.xml 实际生成的日志文件（app.log 或 current.log），而非不存在的 spring.log。

#### Scenario: 日志提示有效
- **WHEN** start.sh 启动完成后打印日志查看提示
- **THEN** 提示的文件名是 logback-spring.xml 中定义的日志文件名之一

### Requirement: logback-spring.xml 无重复 appender 定义
logback-spring.xml SHALL NOT 同时引入 Spring Boot 默认的 CONSOLE appender 和自定义同名 CONSOLE appender。只保留自定义版本。

#### Scenario: 无重复 CONSOLE appender
- **WHEN** 检查 logback-spring.xml 内容
- **THEN** CONSOLE appender 只定义一次

### Requirement: 禁止 @Data 注解
所有 Java 文件 SHALL NOT 使用 Lombok `@Data` 注解。Properties 和 DTO 类 SHALL 使用 `@Getter @Setter`。需要 `@EqualsAndHashCode(callSuper = true)` 的类 SHALL 显式声明。

#### Scenario: 代码中无 @Data 注解
- **WHEN** 搜索项目中所有 Java 文件的 @Data 使用
- **THEN** 不存在任何 @Data 注解（import 语句除外）

### Requirement: 禁止 @Value 注入配置
所有配置注入 SHALL 使用 `@ConfigurationProperties` 而非 `@Value`。WebStartLightApplication 的启动信息属性 SHALL 通过 AppInfoProperties 类注入。

#### Scenario: 配置通过 Properties 类注入
- **WHEN** 检查 WebStartLightApplication 的依赖注入方式
- **THEN** 使用 AppInfoProperties 而非 @Value

### Requirement: client-search 遵循 Template Method 模式
client-search 模块 SHALL 包含 Interface → AbstractSearchClient → SimpleSearchClient 三层结构。AbstractSearchClient 的公开方法 SHALL 为 final，子类实现 do* 扩展点。

#### Scenario: 与其他 client 模式一致
- **WHEN** 检查 client-search 模块的类层次结构
- **THEN** 存在 AbstractSearchClient 抽象基类，SimpleSearchClient 继承它

### Requirement: 生产慢查询阈值文档准确
performance-rules.md 中描述的生产环境慢查询阈值 SHALL 与 application-prod.yaml 中的实际配置值一致。

#### Scenario: 文档与配置阈值一致
- **WHEN** 比较 performance-rules.md 和 application-prod.yaml 的 threshold-ms
- **THEN** 两者值相同

### Requirement: CommonErrorCode 文档 message 文本准确
error-handling.md 中列出的 CommonErrorCode 枚举 message 文本 SHALL 与 CommonErrorCode.java 中的实际值完全一致。

#### Scenario: 文档枚举值与代码一致
- **WHEN** 比较文档和代码中的每个 CommonErrorCode 枚举的 message
- **THEN** 所有 message 文本完全匹配

### Requirement: WebExceptionAdvise 文档示例与代码一致
error-handling.md 中的 WebExceptionAdvise 代码示例 SHALL 反映实际实现：包含所有异常处理器、返回类型为 ResponseEntity。

#### Scenario: 文档代码示例可参考
- **WHEN** 开发者参考 error-handling.md 的异常处理器示例
- **THEN** 示例包含 ConstraintViolationException、BindException、BizException、ClientException、SysException 处理器

### Requirement: AGENTS.md 技术栈表无重复
AGENTS.md 技术栈表格 SHALL 无内容完全重复的行。

#### Scenario: SQLite 只出现一次
- **WHEN** 检查 AGENTS.md 技术栈表格
- **THEN** SQLite 行只出现一次

### Requirement: AGENTS.md client-sms 描述完整
AGENTS.md 中 client-sms 的描述 SHALL 提及 AbstractSmsClient 和 SmsAutoConfiguration 的存在。

#### Scenario: client-sms 描述包含所有类
- **WHEN** 阅读 AGENTS.md 中 client-sms 的描述
- **THEN** 提及 AbstractSmsClient 而非"仅接口+Properties"

### Requirement: overview.md 标注废弃类
overview.md 项目结构树中 SHALL 标注已 @Deprecated 的类（如 ResultCode）为废弃状态。

#### Scenario: 废弃类被标注
- **WHEN** 查看 overview.md 中 ResultCode 的描述
- **THEN** 明确标注为已废弃

### Requirement: Hutool 标注为预置工具库
AGENTS.md 技术栈表格中 Hutool SHALL 被标注为"预置工具库（暂未使用）"。

#### Scenario: Hutool 标注明确
- **WHEN** 阅读 AGENTS.md 技术栈表格
- **THEN** Hutool 有"预置暂未使用"的说明

### Requirement: testing-guide.md 包含两种测试命令
testing-guide.md SHALL 同时列出 `mvn test`（从根目录运行所有模块测试）和 `mvn test -pl app`（仅运行 app 模块测试），并说明区别。

#### Scenario: 两种命令都有文档
- **WHEN** 阅读 testing-guide.md 的测试命令部分
- **THEN** 同时包含有和没有 `-pl app` 的命令

### Requirement: README 包含常用命令
README.md 快速开始部分 SHALL 包含 `mvn test -Dtest="*UTest"`、`mvn test -Dtest="*ITest"`、`scripts/start.sh prod`、`scripts/stop.sh` 命令。

#### Scenario: 常用命令齐全
- **WHEN** 查看 README.md 快速开始部分
- **THEN** 包含单元测试、集成测试、生产启动、停止的命令

### Requirement: POM 版本统一管理
所有第三方依赖版本 SHALL 集中在根 pom.xml 的 properties + dependencyManagement 中管理。子模块 pom.xml SHALL NOT 硬编码依赖版本号。

#### Scenario: 子模块无硬编码版本
- **WHEN** 搜索 app/pom.xml 和 client-*/pom.xml 中的 <version> 标签
- **THEN** 除 ${project.version} 外无硬编码版本号（或版本号已由根 POM dependencyManagement 管理）

### Requirement: clients/pom.xml 无冗余 dependencyManagement
clients/pom.xml SHALL NOT 重复声明根 pom.xml 中已有的 dependencyManagement。

#### Scenario: clients POM 精简
- **WHEN** 检查 clients/pom.xml 的 dependencyManagement
- **THEN** 无与根 POM 重复的依赖声明

### Requirement: application-test.yaml 配置完整
application-test.yaml SHALL 包含 `map-underscore-to-camel-case: true` 和 `id-type: assign_id`，确保测试环境行为与 dev/prod 一致。

#### Scenario: 测试环境字段映射正确
- **WHEN** 在测试环境执行 MyBatis-Plus 查询
- **THEN** 下划线字段名正确映射到驼峰 Java 字段

### Requirement: stop.sh PID 验证安全
stop.sh SHALL 在 kill 之前验证目标 PID 的进程名包含 "java" 或应用名，避免误杀回收后的 PID。

#### Scenario: PID 回收安全
- **WHEN** stop.sh 检查 PID 是否存活
- **THEN** 除了 kill -0 外还验证进程名

### Requirement: OpenSpec 反映实际代码
modularize-clients change 的 tasks.md 中已完成项 SHALL 被标记为 [x]，specs 中的接口方法签名、Properties 字段、AutoConfiguration 条件 SHALL 与实际代码一致。

#### Scenario: tasks checkbox 与代码状态匹配
- **WHEN** 检查 modularize-clients 的 tasks.md
- **THEN** 已实现的功能标记为 [x]

### Requirement: enhance-architecture-from-backend 已归档
enhance-architecture-from-backend change（所有 126 个任务已完成）SHALL 被移动到 openspec/changes/archive/ 目录。

#### Scenario: 归档状态正确
- **WHEN** 检查 openspec/changes/ 目录
- **THEN** enhance-architecture-from-backend 不在活跃 changes 列表中，而在 archive/ 下

### Requirement: OpenSpec 无过时包路径和 H2 引用
OpenSpec artifacts 中 SHALL NOT 包含旧的包路径（如 entity/api/exception/）或已删除的数据库引用（如 H2）。

#### Scenario: 无过时路径
- **WHEN** 搜索 OpenSpec 文件中的 "entity/api/exception" 或 "H2"
- **THEN** 无匹配结果
