## ADDED Requirements

### Requirement: Component 模块目录命名规范

所有原 `clients/client-xxx` 目录 SHALL 重命名为 `components/component-xxx`。

#### Scenario: 目录结构验证

- **WHEN** 查看 Maven 多模块项目根目录
- **THEN** 存在 `components/` 目录（非 `clients/`），其下包含 `component-cache/`、`component-oss/`、`component-email/`、`component-sms/`、`component-search/`、`component-auth/` 六个子目录

### Requirement: Component Java 包命名规范

所有原 `org.smm.archetype.client` 包及其子包 SHALL 重命名为 `org.smm.archetype.component`。

#### Scenario: 包名验证

- **WHEN** 搜索所有 .java 文件的 package 声明
- **THEN** 不存在任何 `org.smm.archetype.client` 的 package 声明，所有原 client 子包已改为 `org.smm.archetype.component.{cache,oss,email,sms,search,auth,dto}`

### Requirement: Component Java 类命名规范

所有以 `Client` 为后缀的技术组件接口和类 SHALL 重命名为 `*Component` 后缀。

#### Scenario: 接口和实现类命名验证

- **WHEN** 搜索 components/ 目录下的所有 .java 文件
- **THEN** 不存在 `*Client.java` 文件（排除 `ClientException`），所有接口/类已使用 `*Component` 命名（如 `CacheComponent`、`AbstractCacheComponent`、`CaffeineCacheComponent`）

#### Scenario: app 模块引用验证

- **WHEN** 搜索 app/ 目录下所有 .java 文件的 import 语句
- **THEN** 所有 `org.smm.archetype.client.*` 的 import 已替换为 `org.smm.archetype.component.*`，所有 `*Client` 类名的引用已替换为 `*Component`

### Requirement: Component 配置前缀命名规范

所有原 `middleware.*` 配置前缀 SHALL 替换为 `component.*`。`middleware.object-storage` SHALL 简化为 `component.oss`。

#### Scenario: Properties 类前缀验证

- **WHEN** 搜索所有 @ConfigurationProperties 注解的 prefix 属性
- **THEN** 不存在 `middleware.` 前缀，所有原 middleware 前缀已改为 `component.{cache,oss,email,sms,search,auth,ratelimit}`，原 `middleware.object-storage` 改为 `component.oss`

#### Scenario: @ConditionalOnProperty 前缀验证

- **WHEN** 搜索所有 @ConditionalOnProperty 注解的 prefix 属性
- **THEN** 不存在 `middleware.` 前缀，所有条件装配属性使用 `component.*` 前缀

### Requirement: YAML 配置文件命名和内容规范

`application-optional.yaml` SHALL 重命名为 `application-component.yaml`，内部配置前缀从 `middleware:` 改为 `component:`。

#### Scenario: YAML 文件名验证

- **WHEN** 查看 app/src/main/resources/ 目录
- **THEN** 存在 `application-component.yaml`（非 `application-optional.yaml`），文件内容使用 `component:` 作为顶级 key

#### Scenario: YAML 配置前缀验证

- **WHEN** 搜索所有 application*.yaml 文件中的配置 key
- **THEN** 不存在 `middleware:` 顶级 key，所有原 middleware 配置已改为 `component:`

### Requirement: Maven POM 命名规范

所有 `clients` 和 `client-xxx` 的 Maven artifactId/module/name SHALL 替换为 `components` 和 `component-xxx`。

#### Scenario: 根 POM module 声明验证

- **WHEN** 查看根 pom.xml 的 modules 节
- **THEN** 存在 `<module>components</module>`（非 `clients`）

#### Scenario: 子模块 POM 验证

- **WHEN** 查看 components/component-xxx/pom.xml
- **THEN** parent artifactId 为 `components`，自身 artifactId 为 `component-xxx`，name 为 `component-xxx`

#### Scenario: app POM 依赖验证

- **WHEN** 查看 app/pom.xml 的依赖声明
- **THEN** 所有 `client-xxx` artifactId 已改为 `component-xxx`

### Requirement: Spring AutoConfiguration 注册更新

所有 AutoConfiguration.imports 文件中的全限定类名 SHALL 更新为新包路径和新类名。

#### Scenario: AutoConfiguration 注册验证

- **WHEN** 搜索 META-INF/spring/ 下的 AutoConfiguration.imports 文件
- **THEN** 所有 `org.smm.archetype.client.*` 引用已改为 `org.smm.archetype.component.*`，所有 `*Client*` 类名已改为 `*Component*`，所有 `*AutoConfiguration*` 类名保持不变

### Requirement: 文档命名和内容规范

所有活跃 Markdown 文档中的模块名、路径、类名、配置前缀、中文描述 SHALL 同步更新。中文"客户端" SHALL 改为"组件"。

#### Scenario: 文档文件名验证

- **WHEN** 查看 docs/modules/ 目录
- **THEN** 存在 `component-cache.md`、`component-oss.md` 等文件（非 `client-*.md`）

#### Scenario: 文档内容验证

- **WHEN** 搜索 AGENTS.md、README.md、docs/ 下所有活跃 .md 文件
- **THEN** 不存在 `client-xxx` 模块名引用、不存在 `middleware.*` 配置前缀引用、中文"客户端"已改为"组件"（`ClientException` 除外）
