## ADDED Requirements

### Requirement: 骨架元数据块
ARCHETYPE_README.md 顶部 SHALL 包含结构化元数据块，列出骨架的 groupId (`org.smm.archetype`)、artifactId (`web-quick-start-light`)、version (`0.0.1-SNAPSHOT`)、Java 版本 (25)、Spring Boot 版本 (4.0.2)。

#### Scenario: AI 助手无需解析全文即可获取关键技术参数
- **WHEN** 读取 ARCHETYPE_README.md 的前 20 行
- **THEN** 可直接获取 groupId、artifactId、version、Java 版本、Spring Boot 版本

### Requirement: 骨架生成项目结构预览
ARCHETYPE_README.md SHALL 包含「生成项目结构」章节，以目录树形式展示使用骨架后获得的项目结构，包含 `common/`、`clients/`（6 个子模块）、`app/` 的完整层级，以及各模块的一句话说明。

#### Scenario: 开发者了解骨架会生成什么
- **WHEN** 阅读 ARCHETYPE_README.md 的「生成项目结构」章节
- **THEN** 能看到完整的目录树和每个模块的职责说明

### Requirement: 本地安装骨架步骤
ARCHETYPE_README.md SHALL 包含清晰的 3 步安装指南：引入 maven-archetype-plugin → 执行 create-from-project → 进入 archetype 目录执行 mvn install。命令 SHALL 同时提供 macOS/Linux 和 Windows 两个版本。

#### Scenario: macOS/Linux 用户安装骨架
- **WHEN** 按照安装步骤在 macOS/Linux 终端执行命令
- **THEN** 骨架成功安装到本地 Maven 仓库

#### Scenario: Windows 用户安装骨架
- **WHEN** 按照安装步骤在 Windows 终端执行命令
- **THEN** 骨架成功安装到本地 Maven 仓库（使用 `^` 续行符且 `^` 后无空格）

### Requirement: 使用骨架创建新项目
ARCHETYPE_README.md SHALL 提供 IDEA 界面和命令行两种创建方式，命令行方式为推荐。命令行示例 SHALL 包含完整参数（archetypeCatalog=local、interactiveMode=false）。

#### Scenario: 命令行创建新项目
- **WHEN** 执行 `mvn archetype:generate` 命令并传入正确的骨架坐标和新项目参数
- **THEN** 生成完整的多模块 Maven 项目

### Requirement: 骨架排除文件说明
ARCHETYPE_README.md SHALL 说明骨架的排除规则（`excludePatterns`），列出被排除的文件/目录（.idea/、target/、logs/、*.iml、README.md、data_h2/）。

#### Scenario: 开发者了解哪些文件不会被骨架包含
- **WHEN** 阅读 ARCHETYPE_README.md 的排除文件章节
- **THEN** 明确知道 .idea/、target/、logs/、README.md 等文件不会出现在生成的项目中

### Requirement: 故障排查表
ARCHETYPE_README.md SHALL 包含故障排查章节，以表格形式列出常见错误、原因和解决方案。至少包含 ResourceManager 找不到 pom.xml 和 settings.xml 不存在两个问题。

#### Scenario: 开发者遇到 archetype 安装错误时自助解决
- **WHEN** 遇到 `ResourceManager: unable to find resource` 错误
- **THEN** 从故障排查表中找到原因（archetype-metadata.xml 中 dir 路径错误）和解决方案

### Requirement: AI 友好性
ARCHETYPE_README.md SHALL 设计为 AI 助手友好的文档格式：使用结构化表格而非纯文本 dump、代码块标注语言类型、文件路径使用完整相对路径、关键信息前置。

#### Scenario: AI 助手快速理解骨架能力
- **WHEN** AI 助手读取 ARCHETYPE_README.md
- **THEN** 能在 30 秒内回答"如何使用此骨架创建新项目"和"骨架包含哪些模块"两个问题
