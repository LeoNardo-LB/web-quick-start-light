## ADDED Requirements

### Requirement: JaCoCo 插件版本统一管理

根 POM 的 `<properties>` 中声明 `jacoco.version` 属性（值 `0.8.12`），根 POM 的 `<pluginManagement>` 中声明 `jacoco-maven-plugin` 使用该版本。

#### Scenario: 版本属性存在
- **WHEN** 读取根 POM 的 properties
- **THEN** 包含 `<jacoco.version>0.8.12</jacoco.version>`

#### Scenario: pluginManagement 声明
- **WHEN** 读取根 POM 的 pluginManagement
- **THEN** 包含 `org.jacoco:jacoco-maven-plugin:${jacoco.version}` 声明

### Requirement: prepare-agent 全模块自动生效

根 POM `<build><plugins>` 中配置 `jacoco-maven-plugin` 的 `prepare-agent` goal，绑定 `initialize` 阶段。所有子模块（common、clients/*、app）自动继承，无需单独配置。

#### Scenario: 子模块自动继承 prepare-agent
- **WHEN** 在任意子模块目录执行 `mvn test`
- **THEN** 构建日志显示 JaCoCo agent 已注入，`target/jacoco.exec` 文件生成

#### Scenario: prepare-agent 不影响测试执行
- **WHEN** 执行 `mvn test`
- **THEN** 115 个测试全部通过，0 failures，0 errors

### Requirement: 各模块独立覆盖率报告

根 POM `<build><plugins>` 中配置 `report` goal，绑定 `verify` 阶段。每个有 `target/jacoco.exec` 的模块在 `target/site/jacoco/` 下生成 HTML 和 XML 报告。

#### Scenario: 模块级报告生成
- **WHEN** 执行 `mvn verify`
- **THEN** 以下目录存在且包含 `index.html`：
  - `app/target/site/jacoco/index.html`
  - `clients/client-search/target/site/jacoco/index.html`
  - `clients/client-log/target/site/jacoco/index.html`

#### Scenario: XML 报告可用于 CI
- **WHEN** 执行 `mvn verify`
- **THEN** `app/target/site/jacoco/jacoco.xml` 文件存在且为合法 XML

### Requirement: 聚合覆盖率报告

app 模块配置 `report-aggregate` goal，聚合所有子模块覆盖率数据。输出路径 `app/target/site/jacoco-aggregate/`。

#### Scenario: 聚合报告包含所有模块
- **WHEN** 执行 `mvn verify -pl app -am`
- **THEN** `app/target/site/jacoco-aggregate/index.html` 存在
- **THEN** 聚合报告中包含 `org.smm.archetype` 包下所有模块的覆盖率数据

#### Scenario: 聚合报告包含 XML 格式
- **WHEN** 执行 `mvn verify -pl app -am`
- **THEN** `app/target/site/jacoco-aggregate/jacoco.xml` 存在

### Requirement: 排除规则

JaCoCo 的 `excludes` 配置排除以下类（不纳入覆盖率统计）：

- `**/*Test*` — 所有测试类
- `**/generated/**` — 代码生成器产物
- `**/*Application*` — Spring Boot 主启动类
- `**/*Properties*` — 配置属性类
- `**/*Configuration*` — Spring 自动配置类

#### Scenario: 测试类被排除
- **WHEN** 查看覆盖率报告
- **THEN** 报告中不包含 `SystemConfigControllerITest`、`ArchitectureComplianceUTest` 等测试类

#### Scenario: 生成代码被排除
- **WHEN** 查看覆盖率报告
- **THEN** 报告中不包含 `org.smm.archetype.generated` 包下的类

#### Scenario: 业务代码被统计
- **WHEN** 查看覆盖率报告
- **THEN** 报告中包含 `org.smm.archetype.controller.system.SystemConfigController`
- **THEN** 报告中包含 `org.smm.archetype.service.system.SystemConfigService`
- **THEN** 报告中包含 `org.smm.archetype.client.log.LogAspect`

### Requirement: 覆盖率阈值检查

app 模块配置 `check` goal，在 `verify` 阶段校验覆盖率是否达标（post-dev-verification 标准）：

- INSTRUCTION 覆盖率 ≥ 95%
- BRANCH 覆盖率 ≥ 90%

当前 `haltOnFailure=false`，未达标时仅输出警告不中断构建。达标后改为 `true`。

#### Scenario: 阈值达标时构建通过
- **WHEN** 执行 `mvn verify` 且覆盖率满足阈值
- **THEN** 构建成功，`[INFO] All coverage checks have been met.`

#### Scenario: 阈值不达标时输出警告但构建通过
- **WHEN** 覆盖率低于阈值且 `haltOnFailure=false`
- **THEN** 构建成功，日志包含 `[WARNING] Rule violated for bundle app: instructions covered ratio is X, but expected minimum is 0.95`

#### Scenario: 阈值不达标且 haltOnFailure=true 时构建失败
- **WHEN** 覆盖率低于阈值且 `haltOnFailure=true`
- **THEN** 构建失败，日志包含覆盖率不满足的详细信息

### Requirement: AGENTS.md 文档更新

AGENTS.md 新增 JaCoCo 相关内容：版本信息和使用命令。

#### Scenario: 文档包含 JaCoCo 版本
- **WHEN** 读取 AGENTS.md 的技术栈表
- **THEN** 包含 JaCoCo 行，版本 0.8.12

#### Scenario: 文档包含使用命令
- **WHEN** 读取 AGENTS.md
- **THEN** 包含 `mvn verify` 命令说明（生成覆盖率报告）
- **THEN** 包含报告路径说明（`app/target/site/jacoco-aggregate/`）
