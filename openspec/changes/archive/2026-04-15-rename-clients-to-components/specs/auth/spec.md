## MODIFIED Requirements

### Requirement: 认证组件接口和实现

原规范定义了 `AuthClient` 接口及其实现。现 SHALL 更新为：接口名为 `AuthComponent`，实现类为 `SaTokenAuthComponent`（原 `SaTokenAuthClient`）和 `NoOpAuthComponent`（原 `NoOpAuthClient`），包路径为 `org.smm.archetype.component.auth`。

#### Scenario: 认证组件接口验证

- **WHEN** 查看 `components/component-auth/` 下的 Java 源码
- **THEN** 存在 `AuthComponent` 接口（非 `AuthClient`），包名为 `org.smm.archetype.component.auth`（非 `org.smm.archetype.client.auth`）

#### Scenario: 认证组件实现验证

- **WHEN** 查看认证模块的实现类
- **THEN** 存在 `SaTokenAuthComponent`（原 `SaTokenAuthClient`）和 `NoOpAuthComponent`（原 `NoOpAuthClient`），均实现 `AuthComponent` 接口

#### Scenario: 认证配置前缀验证

- **WHEN** 查看 `AuthProperties` 类
- **THEN** `@ConfigurationProperties(prefix = "component.auth")`（非 `middleware.auth`）

#### Scenario: 认证条件装配验证

- **WHEN** 查看 `AuthAutoConfiguration` 类
- **THEN** `@ConditionalOnProperty(prefix = "component.auth", ...)`（非 `middleware.auth`）
