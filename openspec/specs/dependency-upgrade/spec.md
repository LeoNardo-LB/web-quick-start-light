## ADDED Requirements

### Requirement: Spring Boot 升级到 4.0.2
根 POM 的 parent SHALL 从 `spring-boot-starter-parent:3.5.6` 升级到 `4.0.2`。所有 Spring Boot BOM 管理的依赖版本 SHALL 自动跟随升级。

#### Scenario: 编译通过
- **WHEN** 执行 `mvn clean compile`
- **THEN** 所有模块编译成功，无错误

#### Scenario: 测试通过
- **WHEN** 执行 `mvn test`
- **THEN** 所有测试通过（BUILD SUCCESS）

### Requirement: 第三方依赖升级到最新稳定版
所有非 BOM 管理的依赖 SHALL 升级到 Maven Central 最新稳定版：MyBatis-Plus 3.5.16、SpringDoc 2.8.17、Guava 33.5.0-jre、commons-lang3 3.20.0、commons-io 2.21.0、commons-collections4 4.5.0、Kryo 5.6.2、commons-pool2 2.13.1、Fastjson2 2.0.61、Hutool-core 5.8.44、SQLite JDBC 3.51.3.0。

#### Scenario: 依赖版本正确
- **WHEN** 执行 `mvn dependency:tree`
- **THEN** 上述依赖版本与目标版本一致

### Requirement: 新增代码生成器依赖
POM SHALL 新增 `mybatis-plus-generator:3.5.16`（代码生成器核心）和 `freemarker:2.3.34`（模板引擎）依赖。

#### Scenario: 代码生成器依赖可用
- **WHEN** 在 app/pom.xml 中声明 mybatis-plus-generator 和 freemarker 依赖
- **THEN** 代码生成器类可正常编译和运行

### Requirement: 新增日志编码器依赖
POM SHALL 新增 `logstash-logback-encoder:9.0` 依赖用于 JSON 日志输出。

#### Scenario: JSON 日志依赖可用
- **WHEN** logback-spring.xml 中引用 LogstashEncoder
- **THEN** 编译和运行时类路径中包含 logstash-logback-encoder

### Requirement: 移除冗余依赖
POM SHALL 移除 `kryo-shaded:4.0.3`（与 kryo 5.x 功能重复）和 `hutool-bom:5.8.40`（改为直接声明 hutool-core 版本）。

#### Scenario: 无冗余依赖
- **WHEN** 执行 `mvn dependency:tree`
- **THEN** 不包含 kryo-shaded 和 hutool-bom

### Requirement: AGENTS.md 版本号更新
AGENTS.md 的技术栈表格 SHALL 更新为实际使用的版本号。

#### Scenario: 版本号一致
- **WHEN** 阅读 AGENTS.md 的技术栈表格
- **THEN** Spring Boot 版本为 4.0.2，MyBatis-Plus 版本为 3.5.16，其他版本号与 POM 一致
