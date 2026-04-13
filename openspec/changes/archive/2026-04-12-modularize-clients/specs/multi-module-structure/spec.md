## ADDED Requirements

### Requirement: 根 POM 多模块聚合
根 pom.xml SHALL 设置 `packaging=pom`，聚合 2 个子模块：`clients` 和 `app`。根 POM SHALL 继承 `spring-boot-starter-parent:3.5.6`，统一管理所有子模块的版本和依赖。

#### Scenario: Maven Reactor 构建顺序
- **WHEN** 在根目录执行 `mvn compile`
- **THEN** Maven Reactor 按 `clients` → `app` 顺序构建

### Requirement: clients 子模块
clients SHALL 是一个不可执行的 jar 模块（无 spring-boot-maven-plugin repackage）。SHALL 依赖 `spring-boot-starter`（compile）+ `caffeine`（optional）+ `spring-boot-starter-mail`（optional）+ `slf4j-api`（compile）。SHALL 包含所有 Client 接口、抽象基类、真实实现、Properties、AutoConfiguration 和 DTO。SHALL 通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册自动装配类。

#### Scenario: clients 可独立编译
- **WHEN** 在 clients 目录执行 `mvn compile`
- **THEN** 编译成功，无错误

### Requirement: app 子模块
app SHALL 是可执行 jar 模块（spring-boot-maven-plugin repackage），包含 main 启动类。SHALL 依赖 `clients`（compile，必选）。SHALL 包含所有 Controller/Service/Repository/Entity/Config/Util 代码和所有测试。

#### Scenario: app 可启动
- **WHEN** 执行 `mvn spring-boot:run -pl app`
- **THEN** 应用正常启动，所有 API 端点可用

### Requirement: 代码迁移完整性
所有现有 Java 源码 SHALL 从 `src/main/java/` 迁移到 `app/src/main/java/`（除 client 相关代码移入 clients）。所有现有测试 SHALL 从 `src/test/java/` 迁移到 `app/src/test/java/`。所有现有配置文件 SHALL 迁移到 `app/src/main/resources/`。迁移后所有 134 个现有测试 SHALL 全部通过。

#### Scenario: 现有测试全部通过
- **WHEN** 执行 `mvn test`
- **THEN** 134 个测试全部通过，0 失败

### Requirement: AutoConfiguration.imports 注册
每个 client 子模块 SHALL 在各自的 `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 中注册对应的 Auto-Configuration 类：CacheAutoConfiguration（client-cache）、OssAutoConfiguration（client-oss）、EmailAutoConfiguration（client-email）、SmsAutoConfiguration（client-sms）、SearchAutoConfiguration（client-search）。

#### Scenario: AutoConfiguration.imports 文件存在且格式正确
- **WHEN** 编译 clients 模块
- **THEN** jar 中包含 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports 文件，每行一个全限定类名

### Requirement: CommonErrorCode 新增 Client 错误码
CommonErrorCode 枚举 SHALL 新增 6 个 Client 相关错误码：CACHE_OPERATION_FAILED(6001)、OSS_OPERATION_FAILED(6101)、OSS_UPLOAD_FAILED(6102)、EMAIL_SEND_FAILED(6201)、SMS_SEND_FAILED(6301)、SEARCH_OPERATION_FAILED(6401)。

#### Scenario: Client 错误码可引用
- **WHEN** 在 clients 模块中引用 CommonErrorCode.CACHE_OPERATION_FAILED
- **THEN** 编译通过，错误码和消息字符串正确

### Requirement: Properties 前缀迁移
所有 Client 相关 Properties 的前缀 SHALL 从旧的分散前缀（`cache.*`、`email.*`、`sms.*`、`oss.*`）迁移到统一的 `middleware.*` 前缀（`middleware.cache`、`middleware.email`、`middleware.sms`、`middleware.object-storage`）。

#### Scenario: 旧前缀不再生效
- **WHEN** 在 application.yaml 中使用旧前缀 `cache.type=caffeine`
- **THEN** CacheProperties 的 type 字段不被绑定（使用默认值）
