## MODIFIED Requirements

### Requirement: 客户端模块替换规范

原规范定义了将基础架构代码从 `infrastructure` 包迁移到独立 `client-*` 模块的规则。现 SHALL 更新为：所有技术组件模块使用 `component-xxx` 命名，Java 包名为 `org.smm.archetype.component.*`，类名使用 `*Component` 后缀。

#### Scenario: 模块结构验证

- **WHEN** 查看 Maven 项目结构
- **THEN** 存在 `components/` 父模块，其下包含 `component-cache`、`component-oss`、`component-email`、`component-sms`、`component-search`、`component-auth` 六个独立 Maven 子模块

#### Scenario: 包路径验证

- **WHEN** 查看 components/ 下的 Java 源码
- **THEN** 所有包名为 `org.smm.archetype.component.{cache,oss,email,sms,search,auth,dto}`，不存在 `org.smm.archetype.client` 包

#### Scenario: 配置前缀验证

- **WHEN** 查看各模块的 Properties 类
- **THEN** 配置前缀统一为 `component.{cache,oss,email,sms,search,auth,ratelimit}`，不存在 `middleware.` 前缀
