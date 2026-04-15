## 1. 准备工作

- [ ] 1.1 确认 test-driven-development、dispatching-parallel-agents 技能已加载（未加载则加载，已加载则跳过）
- [ ] 1.2 确认当前分支为干净状态（`git status` 无未提交更改），从 main 创建 feature 分支 `feature/rename-clients-to-components`

## 2. Chunk 1：目录重命名

> 将 `clients/` 及其 6 个子目录重命名为 `components/component-xxx`

- [ ] 2.1 执行目录重命名：`clients/` → `components/`，`clients/client-cache/` → `components/component-cache/`，依此类推（oss、email、sms、search、auth 共 6 个子目录）
- [ ] 2.2 验证目录结构正确：`ls components/` 显示 6 个 `component-*` 子目录

## 3. Chunk 2：Java 包名和类名重命名

> 将 `org.smm.archetype.client` 包重命名为 `org.smm.archetype.component`，所有 `*Client` 类重命名为 `*Component`

### 3.1 component-cache 模块

- [ ] 3.1.1 将 `components/component-cache/` 下所有 Java 文件的物理目录从 `.../client/cache/` 移动到 `.../component/cache/`
- [ ] 3.1.2 替换所有文件中的 `package org.smm.archetype.client.cache` → `package org.smm.archetype.component.cache`
- [ ] 3.1.3 重命名类：`CacheClient.java` → `CacheComponent.java`，`AbstractCacheClient.java` → `AbstractCacheComponent.java`，`CaffeineCacheClient.java` → `CaffeineCacheComponent.java`
- [ ] 3.1.4 更新类内部：类名声明、构造器名、引用了旧类名的所有位置

### 3.2 component-oss 模块

- [ ] 3.2.1 将 `components/component-oss/` 下所有 Java 文件的物理目录从 `.../client/{oss,dto}/` 移动到 `.../component/{oss,dto}/`
- [ ] 3.2.2 替换所有文件中的 `package org.smm.archetype.client.{oss,dto}` → `package org.smm.archetype.component.{oss,dto}`
- [ ] 3.2.3 重命名类：`OssClient.java` → `OssComponent.java`，`AbstractOssClient.java` → `AbstractOssComponent.java`，`LocalOssClient.java` → `LocalOssComponent.java`

### 3.3 component-email 模块

- [ ] 3.3.1 将 `components/component-email/` 下所有 Java 文件的物理目录从 `.../client/{email,dto}/` 移动到 `.../component/{email,dto}/`
- [ ] 3.3.2 替换所有文件中的 `package org.smm.archetype.client.{email,dto}` → `package org.smm.archetype.component.{email,dto}`
- [ ] 3.3.3 重命名类：`EmailClient.java` → `EmailComponent.java`，`AbstractEmailClient.java` → `AbstractEmailComponent.java`，`NoOpEmailClient.java` → `NoOpEmailComponent.java`

### 3.4 component-sms 模块

- [ ] 3.4.1 将 `components/component-sms/` 下所有 Java 文件的物理目录从 `.../client/{sms,dto}/` 移动到 `.../component/{sms,dto}/`
- [ ] 3.4.2 替换所有文件中的 `package org.smm.archetype.client.{sms,dto}` → `package org.smm.archetype.component.{sms,dto}`
- [ ] 3.4.3 重命名类：`SmsClient.java` → `SmsComponent.java`，`AbstractSmsClient.java` → `AbstractSmsComponent.java`，`NoOpSmsClient.java` → `NoOpSmsComponent.java`

### 3.5 component-search 模块

- [ ] 3.5.1 将 `components/component-search/` 下所有 Java 文件的物理目录从 `.../client/{search,dto}/` 移动到 `.../component/{search,dto}/`
- [ ] 3.5.2 替换所有文件中的 `package org.smm.archetype.client.{search,dto}` → `package org.smm.archetype.component.{search,dto}`
- [ ] 3.5.3 重命名类：`SearchClient.java` → `SearchComponent.java`，`AbstractSearchClient.java` → `AbstractSearchComponent.java`，`SimpleSearchClient.java` → `SimpleSearchComponent.java`
- [ ] 3.5.4 更新 `SearchAutoConfigurationUTest.java` 中的全限定名引用（`org.smm.archetype.client.dto.SearchResult` → `org.smm.archetype.component.dto.SearchResult` 等 4 处）

### 3.6 component-auth 模块

- [ ] 3.6.1 将 `components/component-auth/` 下所有 Java 文件的物理目录从 `.../client/auth/` 移动到 `.../component/auth/`
- [ ] 3.6.2 替换所有文件中的 `package org.smm.archetype.client.auth` → `package org.smm.archetype.component.auth`
- [ ] 3.6.3 重命名类：`AuthClient.java` → `AuthComponent.java`，`AbstractAuthClient.java` → `AbstractAuthComponent.java`，`SaTokenAuthClient.java` → `SaTokenAuthComponent.java`，`NoOpAuthClient.java` → `NoOpAuthComponent.java`

### 3.7 app 模块中引用更新

- [ ] 3.7.1 替换 `app/` 下所有 Java 文件的 import：`org.smm.archetype.client.cache.CacheClient` → `org.smm.archetype.component.cache.CacheComponent`（涉及 `IdempotentConfigure.java`、`IdempotentAspect.java`）
- [ ] 3.7.2 替换 `app/` 下所有 Java 文件的 import：`org.smm.archetype.client.auth.AuthClient` → `org.smm.archetype.component.auth.AuthComponent`（涉及 `WebConfigure.java`、`ContextFillFilter.java`、`LoginFacadeImpl.java`）
- [ ] 3.7.3 替换 `app/` 下所有测试文件的 import 和类名引用（`IdempotentConfigureUTest.java`、`IdempotentAspectUTest.java`、`ContextFillFilterUTest.java`、`SaTokenAuthClientITest.java`、`TechClientInterfaceITest.java`）
- [ ] 3.7.4 更新测试注释/断言中的 `client-xxx` 引用：`NoRedundantConfigureUTest.java`（7 处）、`LogAutoConfigurationITest.java`（3 处）、`BusinessLogITest.java`（2 处）、`SaTokenAuthClientITest.java`（1 处）中的模块名

### 3.8 编译验证

- [ ] 3.8.1 执行 `mvn compile -pl components/component-cache,components/component-oss,components/component-email,components/component-sms,components/component-search,components/component-auth,app -am` 确认编译通过
- [ ] 3.8.2 执行 `mvn test -pl components/component-auth,components/component-search` 确认子模块单元测试通过

## 4. Chunk 3：配置前缀替换 + YAML 文件重命名

> 将 `middleware.*` 前缀全部改为 `component.*`，`middleware.object-storage` 简化为 `component.oss`

### 4.1 Properties 类前缀替换

- [ ] 4.1.1 `CacheProperties.java`：`prefix = "middleware.cache"` → `prefix = "component.cache"`
- [ ] 4.1.2 `OssProperties.java`：`prefix = "middleware.object-storage"` → `prefix = "component.oss"`
- [ ] 4.1.3 `EmailProperties.java`：`prefix = "middleware.email"` → `prefix = "component.email"`
- [ ] 4.1.4 `SmsProperties.java`：`prefix = "middleware.sms"` → `prefix = "component.sms"`
- [ ] 4.1.5 `SearchProperties.java`：`prefix = "middleware.search"` → `prefix = "component.search"`
- [ ] 4.1.6 `AuthProperties.java`：`prefix = "middleware.auth"` → `prefix = "component.auth"`
- [ ] 4.1.7 `RateLimitProperties.java`：`prefix = "middleware.ratelimit"` → `prefix = "component.ratelimit"` + 注释更新

### 4.2 AutoConfiguration 类 @ConditionalOnProperty 替换

- [ ] 4.2.1 `CacheAutoConfiguration.java`：`prefix = "middleware.cache"` → `prefix = "component.cache"` + Javadoc 注释
- [ ] 4.2.2 `OssAutoConfiguration.java`：`prefix = "middleware.object-storage"` → `prefix = "component.oss"`
- [ ] 4.2.3 `EmailAutoConfiguration.java`：`prefix = "middleware.email"` → `prefix = "component.email"`
- [ ] 4.2.4 `SmsAutoConfiguration.java`：`prefix = "middleware.sms"` → `prefix = "component.sms"`
- [ ] 4.2.5 `SearchAutoConfiguration.java`：`prefix = "middleware.search"` → `prefix = "component.search"` + Javadoc 注释
- [ ] 4.2.6 `AuthAutoConfiguration.java`：`prefix = "middleware.auth"` → `prefix = "component.auth"` + Javadoc 注释
- [ ] 4.2.7 `AuthInterceptorConfigurer.java`：`prefix = "middleware.auth"` → `prefix = "component.auth"`

### 4.3 测试代码中配置属性替换

- [ ] 4.3.1 `SearchAutoConfigurationUTest.java`：`middleware.search.*` → `component.search.*`（4 处）
- [ ] 4.3.2 `AuthAutoConfigurationUTest.java`：`middleware.auth.*` → `component.auth.*`（3 处）
- [ ] 4.3.3 `RatelimitConfigureUTest.java`：`middleware.ratelimit.*` → `component.ratelimit.*`（3 处）

### 4.4 YAML 配置文件更新

- [ ] 4.4.1 重命名 `application-optional.yaml` → `application-component.yaml`
- [ ] 4.4.2 更新 `application-component.yaml` 内容：`middleware:` → `component:`，`object-storage:` → `oss:`，注释文本同步更新
- [ ] 4.4.3 更新 `application.yaml`：`middleware:` → `component:`（行56），注释"认证中间件" → "认证组件"
- [ ] 4.4.4 更新 `application-test.yaml`：`middleware:` → `component:`（行42），注释"认证中间件" → "认证组件"

### 4.5 编译和测试验证

- [ ] 4.5.1 执行 `mvn compile` 确认全量编译通过
- [ ] 4.5.2 执行 `mvn test -pl app` 确认 app 模块测试通过（含 `TechClientInterfaceITest`、`SaTokenAuthComponentITest` 等集成测试）

## 5. Chunk 4：POM 文件 + AutoConfiguration 注册更新

### 5.1 根 POM

- [ ] 5.1.1 `pom.xml`：`<module>clients</module>` → `<module>components</module>`
- [ ] 5.1.2 `pom.xml`：dependencyManagement 中 7 个 `client-xxx` artifactId → `component-xxx` + `middleware.object-storage` 对应的注释说明
- [ ] 5.1.3 `pom.xml`：注释中 `client-log、client-ratelimit、client-idempotent` → `component-log、component-ratelimit、component-idempotent`

### 5.2 components 父 POM

- [ ] 5.2.1 `components/pom.xml`：`<artifactId>clients</artifactId>` → `<artifactId>components</artifactId>`，`<name>` 和 `<description>` 同步更新
- [ ] 5.2.2 `components/pom.xml`：6 个 `<module>client-xxx</module>` → `<module>component-xxx</module>`

### 5.3 子模块 POM

- [ ] 5.3.1 6 个子模块 POM（component-cache/oss/email/sms/search/auth）：parent artifactId `clients` → `components`，自身 artifactId/name/description `client-xxx` → `component-xxx`

### 5.4 app POM

- [ ] 5.4.1 `app/pom.xml`：6 个 `<artifactId>client-xxx</artifactId>` 依赖 → `component-xxx` + 注释中 `client-xxx` → `component-xxx`（含 `client-cache optional`、`client-ratelimit 依赖提升`、`client-log 依赖提升` 共 4 处注释）

### 5.5 AutoConfiguration 注册文件

- [ ] 5.5.1 `component-auth/.../AutoConfiguration.imports`：2 行全限定类名 `org.smm.archetype.client.auth.*AutoConfiguration/*Configurer` → `org.smm.archetype.component.auth.*AutoConfiguration/*Configurer`
- [ ] 5.5.2 `component-cache/.../AutoConfiguration.imports`：1 行 → `org.smm.archetype.component.cache.CacheAutoConfiguration`
- [ ] 5.5.3 `component-oss/.../AutoConfiguration.imports`：1 行 → `org.smm.archetype.component.oss.OssAutoConfiguration`
- [ ] 5.5.4 `component-email/.../AutoConfiguration.imports`：1 行 → `org.smm.archetype.component.email.EmailAutoConfiguration`
- [ ] 5.5.5 `component-sms/.../AutoConfiguration.imports`：1 行 → `org.smm.archetype.component.sms.SmsAutoConfiguration`
- [ ] 5.5.6 `component-search/.../AutoConfiguration.imports`：1 行 → `org.smm.archetype.component.search.SearchAutoConfiguration`

### 5.6 全量构建验证

- [ ] 5.6.1 执行 `mvn clean compile` 确认全量编译通过
- [ ] 5.6.2 执行 `mvn test` 确认所有测试通过

## 6. Chunk 5：文档更新

> 更新所有活跃 Markdown 文档，同步模块名、路径、类名、配置前缀、中文描述

### 6.1 根目录文档

- [ ] 6.1.1 `AGENTS.md`：目录树 `clients/client-*` → `components/component-*`（~10 处），`application-optional.yaml` → `application-component.yaml`，"技术客户端规范（clients 模块）" → "技术组件规范（components 模块）"，Properties 前缀 `middleware.*` → `component.*`，文档索引表 `client-*.md` → `component-*.md` 链接和描述
- [ ] 6.1.2 `README.md`：目录树 `clients/client-*` → `components/component-*`（~10 处）

### 6.2 架构文档

- [ ] 6.2.1 `docs/architecture/module-structure.md`：目录树、Mermaid 图节点 `CLIENTS/clients` → `COMPONENTS/components`，`client-*` → `component-*`，注释中"客户端模块" → "组件模块"
- [ ] 6.2.2 `docs/architecture/system-overview.md`：Mermaid 图 subgraph 和节点标签 `client-*` → `component-*`，"客户端模块" → "组件模块"，文件链接 `client-*.md` → `component-*.md`
- [ ] 6.2.3 `docs/architecture/design-patterns.md`：表格 `client-*` → `component-*`（12 处），Mermaid 图节点（6 处），`middleware.*` → `component.*`（~14 处），"客户端" → "组件"（~10 处中文描述）

### 6.3 编码规范文档

- [ ] 6.3.1 `docs/conventions/configuration.md`：`middleware.*` → `component.*`（~25 处，含代码示例、表格、反模式示例），`middleware.object-storage` → `component.oss`，`client-*/` → `component-*/` 路径引用（~8 处），"客户端模块" → "组件模块"
- [ ] 6.3.2 `docs/conventions/testing-conventions.md`：`client-email` → `component-email`，`mvn test -pl client-cache` → `component-cache`，"客户端模块" → "组件模块"
- [ ] 6.3.3 `docs/conventions/error-handling.md`：`ClientException` 保持不变，"Client 中间件" → "Component 中间件"，"客户端" → "组件"

### 6.4 模块文档（文件重命名 + 内容更新）

- [ ] 6.4.1 重命名 `docs/modules/client-cache.md` → `component-cache.md`，内容：标题/描述 `缓存客户端（client-cache）` → `缓存组件（component-cache）`，模块坐标 `org.smm.archetype:client-cache` → `:component-cache`，`middleware.cache.*` → `component.cache.*`（~5 处），"缓存客户端" → "缓存组件"
- [ ] 6.4.2 重命名 `docs/modules/client-oss.md` → `component-oss.md`，内容：`对象存储客户端（client-oss）` → `对象存储组件（component-oss）`，模块坐标 `org.smm.archetype:client-oss` → `:component-oss`，`middleware.object-storage.*` → `component.oss.*`（~5 处），"对象存储客户端" → "对象存储组件"
- [ ] 6.4.3 重命名 `docs/modules/client-email.md` → `component-email.md`，内容：同上模式
- [ ] 6.4.4 重命名 `docs/modules/client-sms.md` → `component-sms.md`，内容：同上模式
- [ ] 6.4.5 重命名 `docs/modules/client-search.md` → `component-search.md`，内容：同上模式
- [ ] 6.4.6 重命名 `docs/modules/client-auth.md` → `component-auth.md`，内容：`AuthClient` → `AuthComponent`，包路径 `org.smm.archetype.client.auth` → `org.smm.archetype.component.auth`（~5 处），`middleware.auth.*` → `component.auth.*`（~10 处），"认证客户端" → "认证组件"

### 6.5 其他文档

- [ ] 6.5.1 `docs/modules/README.md`："技术客户端" → "技术组件"，文件链接 `client-*.md` → `component-*.md`（6 处），`middleware.*` 引用
- [ ] 6.5.2 `docs/modules/auth.md`：路径 `clients/client-auth/` → `components/component-auth/`（7 处），`client-auth` → `component-auth`，artifactId、`AuthClient` → `AuthComponent`
- [ ] 6.5.3 `docs/modules/operation-log.md`：`client-log` → `component-log`（1 处变更日志引用），"日志客户端" → "日志组件"
- [ ] 6.5.4 `docs/modules/system-config.md`：`client-email` → `component-email`，`client-oss` → `component-oss`（2 处）
- [ ] 6.5.5 `docs/README.md`：文件链接 `client-*.md` → `component-*.md`（6 处），"技术客户端" → "技术组件"，`client-log/client-ratelimit/client-idempotent` → `component-*`

### 6.6 文档验证

- [ ] 6.6.1 搜索 `docs/` 下所有活跃 .md 文件，确认不存在 `client-`（模块名引用）、`middleware.`（配置前缀引用）残留（`ClientException` 除外）
- [ ] 6.6.2 搜索 `docs/` 下所有活跃 .md 文件，确认"客户端"已全部改为"组件"（`ClientException` 描述除外）

## 7. 最终验证

- [ ] 7.1 执行 `mvn clean package -DskipTests` 确认全量构建通过
- [ ] 7.2 执行 `mvn test` 确认所有测试通过
- [ ] 7.3 全局搜索确认无残留：`grep -r "org.smm.archetype.client" --include="*.java"` 返回 0 结果（排除 `ClientException`）
- [ ] 7.4 全局搜索确认无残留：`grep -r "middleware\." --include="*.java" --include="*.yaml"` 返回 0 结果
- [ ] 7.5 全局搜索确认无残留：`grep -r "client-cache\|client-oss\|client-email\|client-sms\|client-search\|client-auth" --include="*.xml" --include="*.md" --include="*.java"` 返回 0 结果（排除 openspec/ 和 docs/archived/）
- [ ] 7.6 确认 `application-optional.yaml` 不存在，`application-component.yaml` 存在且内容正确

## 8. 一致性检查

- [ ] 8.1 进行 artifact 文档、讨论结果的一致性检查
