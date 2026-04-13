## Why

项目当前无代码覆盖率工具，无法量化测试覆盖的代码范围。在 L3 后开发验证中，Coverage 指标只能估算，无法提供精确数据。需要添加 JaCoCo Maven 插件以获取多模块聚合覆盖率报告，为后续质量门禁提供数据支撑。

## What Changes

- 在根 POM 的 `pluginManagement` 中声明 `jacoco-maven-plugin`（版本统一管理）
- 在根 POM 的 `build/plugins` 中配置 `prepare-agent`（绑定 `initialize` 阶段，所有子模块继承）
- 在根 POM 的 `build/plugins` 中配置 `report`（绑定 `verify` 阶段，按模块生成报告）
- 在根 POM 的 `properties` 中定义 `jacoco.version` 属性
- 配置排除规则：`*Test*`、`*UTest`、`*ITest`、generated 包、`*Application` 主类、`*Properties`、`*Configuration`（Spring 自动配置类）
- 在 app 模块中配置 `report-aggregate`（聚合所有子模块覆盖率数据）
- 配置覆盖率阈值（`check` goal）：指令覆盖 ≥80%，分支覆盖 ≥70%（初期宽松，后续收紧）
- 更新 `AGENTS.md` 文档，添加 JaCoCo 使用说明

## Capabilities

### New Capabilities
- `jacoco-coverage`: JaCoCo 代码覆盖率配置，包含多模块聚合报告、排除规则、覆盖率阈值校验

### Modified Capabilities

## Impact

- **POM 文件**: 根 POM（`pluginManagement` + `build/plugins`）、app/pom.xml（`report-aggregate`）
- **依赖**: 新增 `org.jacoco:jacoco-maven-plugin`（仅在 build 插件中，不影响运行时依赖）
- **构建**: `mvn verify` 将自动生成覆盖率报告到各模块 `target/site/jacoco/` 目录
- **文档**: AGENTS.md 新增 JaCoCo 使用命令
- **CI**: 报告路径 `app/target/site/jacoco-aggregate/` 可供 CI 流水线收集
