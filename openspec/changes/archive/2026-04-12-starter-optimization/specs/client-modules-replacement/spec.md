## MODIFIED Requirements

### Requirement: Client 模块引入即用
每个 client 模块 SHALL 通过自身的 `*AutoConfiguration` + `.imports` 注册文件实现自动装配。app 模块 SHALL NOT 包含与 client AutoConfiguration 重复的 `@Configuration` 类。

#### Scenario: app 模块无冗余 Configure 类
- **WHEN** 查看 app 模块的 `config/` 目录
- **THEN** 不存在 CacheConfigure.java、OssConfigure.java、NotificationConfigure.java、SearchConfigure.java 这 4 个文件

#### Scenario: 引入 client-cache 依赖后自动装配 CacheClient
- **WHEN** app 的 pom.xml 包含 `client-cache` 依赖，且 yaml 中 `middleware.cache.enabled` 为 true 或未配置（默认 true）
- **THEN** `CacheClient` Bean 自动由 `CacheAutoConfiguration` 注册，无需 app 模块任何 Java 配置

#### Scenario: 引入 client-oss 依赖后自动装配 OssClient
- **WHEN** app 的 pom.xml 包含 `client-oss` 依赖，且 yaml 中 `middleware.object-storage.enabled` 为 true 或未配置（默认 true）
- **THEN** `OssClient` Bean 自动由 `OssAutoConfiguration` 注册，无需 app 模块任何 Java 配置

#### Scenario: Email/Sms 条件装配
- **WHEN** yaml 中 `middleware.email.enabled=true` 或 `middleware.sms.enabled=true`
- **THEN** 对应的 `EmailClient`/`SmsClient` Bean 由各自的 AutoConfiguration 注册（默认 NoOp 实现）

#### Scenario: Search 默认启用
- **WHEN** app 的 pom.xml 包含 `client-search` 依赖
- **THEN** `SearchClient` Bean 自动由 `SearchAutoConfiguration` 注册（默认 `matchIfMissing=true`）

### Requirement: Properties 默认值
每个 client 模块的 `*Properties` 类 SHALL 通过字段初始化提供合理默认值，使"引入依赖、零 yaml 配置"即可正常工作。

#### Scenario: CacheProperties 有默认值
- **WHEN** yaml 未配置 `middleware.cache.*`
- **THEN** CacheProperties 的 `initialCapacity=1000`、`maximumSize=10000`、`expireAfterWrite=30d`、`expireAfterAccess=30d` 作为默认值生效

#### Scenario: OssProperties 有默认值
- **WHEN** yaml 未配置 `middleware.object-storage.*`
- **THEN** OssProperties 的 `type="local"`、`localStoragePath="./uploads"` 作为默认值生效

#### Scenario: Email/Sms Properties 保持无连接默认值
- **WHEN** yaml 未配置 `middleware.email.*` 或 `middleware.sms.*`
- **THEN** EmailProperties 和 SmsProperties 的 host/accessKey 等连接字段为 null（因为默认 disabled）

#### Scenario: SearchProperties 有默认值
- **WHEN** yaml 未配置 `middleware.search.*`
- **THEN** SearchProperties 的 `defaultPageSize=10`、`maxPageSize=100` 作为默认值生效

### Requirement: IDE 配置元数据自动生成
每个 client 模块 SHALL 包含 `spring-boot-autoconfigure-processor` 和 `spring-boot-configuration-processor` 依赖，构建时自动生成 `spring-configuration-metadata.json`。

#### Scenario: IDE 自动补全 middleware 配置项
- **WHEN** 开发者在 IDE 中编辑 yaml 文件，输入 `middleware.`
- **THEN** IDE 自动提示 `cache`、`object-storage`、`email`、`sms`、`search` 及其子属性，显示类型和描述
