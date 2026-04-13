## 1. 预备工作

- [x] 确认 test-driven-development、dispatching-parallel-agents 技能已加载（未加载则加载，已加载则跳过）

## 2. 多模块骨架搭建

- [x] 2.1 创建 clients 和 app 目录结构（mkdir -p clients/src/main/java/org/smm/archetype/client, app/src/main/java, app/src/test/java）
- [x] 2.2 改造根 POM：packaging=pom，添加 modules 聚合，统一依赖版本管理（spring-boot-dependencies BOM）
- [x] 2.3 创建 clients/pom.xml：parent=根 POM，依赖 spring-boot-starter + caffeine(optional) + spring-boot-starter-mail(optional)，不使用 spring-boot-maven-plugin repackage
- [x] 2.4 创建 app/pom.xml：parent=根 POM，依赖 clients + 所有原有依赖（web/mybatis-plus/mapstruct/springdoc/sqlite/archunit 等），保留 spring-boot-maven-plugin repackage
- [x] 2.5 将现有 src/main/java 全部迁移到 app/src/main/java，将现有 src/test/java 全部迁移到 app/src/test/java，将现有 src/main/resources 全部迁移到 app/src/main/resources
- [x] 2.6 验证迁移：执行 mvn compile 确认两模块编译通过，执行 mvn test 确认 134 个测试全部通过

## 3. Client 公共基础设施（clients 模块）

- [x] 3.1 RED: 编写 CommonErrorCode 新增 Client 错误码的测试 — 验证 CACHE_OPERATION_FAILED/OSS_OPERATION_FAILED/OSS_UPLOAD_FAILED/EMAIL_SEND_FAILED/SMS_SEND_FAILED/SEARCH_OPERATION_FAILED 的 code 和 message 非空
- [x] 3.2 GREEN: 在 CommonErrorCode 枚举中新增 6 个 Client 错误码（6001/6101/6102/6201/6301/6401），使测试通过
- [x] 3.3 将 8 个 DTO（EmailRequest/EmailResult/SmsRequest/SmsResult/OssUploadRequest/OssUploadResult/SearchQuery/SearchResult）从 app 移入 clients 的 client/dto/ 包，调整 import
- [x] 3.4 验证编译通过：mvn compile -pl clients

## 4. Cache 模块实现

- [x] 4.1 RED: 编写 CacheClient 接口定义测试 — 验证接口包含 10 个方法签名（get/getList/put/put默认过期/append/delete/hasKey/expire/getExpire/keys），泛型参数正确
- [x] 4.2 GREEN: 在 clients 创建 CacheClient 接口，声明 10 个方法
- [x] 4.3 RED: 编写 AbstractCacheClient 模板方法测试 — 覆盖：key 为 null 抛 ClientException、key 为空字符串抛 ClientException、正常 key 调用 do* 扩展点、异常包装为 ClientException(CACHE_OPERATION_FAILED)
- [x] 4.4 GREEN: 创建 AbstractCacheClient 抽象类，实现 CacheClient 接口，所有方法标记 final，统一 key 校验/日志/异常包装，声明 do* 抽象方法
- [x] 4.5 RED: 编写 CaffeineCacheClient 实现测试 — 覆盖：put 后 get 返回正确值、过期后 get 返回 null、put 默认过期、getList 返回列表、delete 后 get 返回 null、hasKey 判断、expire 设置过期、getExpire 获取剩余时间、keys 模式匹配、append 追加
- [x] 4.6 GREEN: 创建 CaffeineCacheClient（含内部类 CacheValueWrapper + CaffeineExpiry），继承 AbstractCacheClient，实现所有 do* 方法
- [x] 4.7 RED: 编写 CacheProperties 测试 — 验证默认值（initialCapacity=100/maximumSize=1000/expireAfterWrite=30m/expireAfterAccess=0）
- [x] 4.8 GREEN: 创建 CacheProperties（@ConfigurationProperties("middleware.cache")），不使用 @Data
- [x] 4.9 RED: 编写 CacheAutoConfiguration 条件装配测试 — 覆盖：Caffeine 在 classpath 且 type=caffeine 时注册 Bean、type 非 caffeine 时不注册、用户自定义 Bean 时回退
- [x] 4.10 GREEN: 创建 CacheAutoConfiguration（@AutoConfiguration + @ConditionalOnClass + @ConditionalOnProperty + @EnableConfigurationProperties + @ConditionalOnMissingBean），注册 CaffeineCacheClient Bean
- [x] 4.11 创建 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports 文件，注册 CacheAutoConfiguration
- [x] 4.12 验证 Cache 模块：执行 mvn test -pl clients -Dtest="*Cache*" 确认所有 Cache 相关测试通过

## 5. OSS 模块实现

- [x] 5.1 RED: 编写 OssClient 接口定义测试 — 验证接口包含 7 个方法签名（upload/download/delete/generateUrl/searchFiles/exists/getFileSize）
- [x] 5.2 GREEN: 在 clients 创建 OssClient 接口，声明 7 个方法
- [x] 5.3 RED: 编写 AbstractOssClient 模板方法测试 — 覆盖：fileKey 为 null 抛 ClientException、fileKey 为空字符串抛 ClientException、正常 fileKey 调用 do* 扩展点、upload request 参数校验
- [x] 5.4 GREEN: 创建 AbstractOssClient 抽象类，实现 OssClient 接口，所有方法标记 final，统一 fileKey 校验/日志/异常包装
- [x] 5.5 RED: 编写 LocalOssClient 实现测试 — 覆盖：上传后可下载（内容一致）、日期分层存储路径、删除后 exists 返回 false、generateUrl 返回本地路径、searchFiles 模式匹配、getFileSize 返回正确大小、上传空文件、下载不存在文件抛异常
- [x] 5.6 GREEN: 创建 LocalOssClient（NIO FileChannel.transferTo 零拷贝 + 日期分层 yyyy/MM/ + 内存 Map 元数据索引）
- [x] 5.7 RED: 编写 OssProperties 测试 — 验证默认值（type="local"/localPath="./uploads"）
- [x] 5.8 GREEN: 创建 OssProperties（@ConfigurationProperties("middleware.object-storage")），不使用 @Data
- [x] 5.9 RED: 编写 OssAutoConfiguration 条件装配测试 — 覆盖：type=local 时注册 LocalOssClient、type 非 local 时不注册
- [x] 5.10 GREEN: 创建 OssAutoConfiguration，注册 LocalOssClient Bean
- [x] 5.11 在 AutoConfiguration.imports 中追加注册 OssAutoConfiguration
- [x] 5.12 验证 OSS 模块：执行 mvn test -pl clients -Dtest="*Oss*" 确认所有 OSS 相关测试通过

## 6. Email 模块实现

- [x] 6.1 RED: 编写 EmailClient 接口定义测试 — 验证接口包含 3 个方法签名（sendEmail/sendEmail指定服务商/sendBatchEmail）
- [x] 6.2 GREEN: 在 clients 创建 EmailClient 接口，声明 3 个方法
- [x] 6.3 RED: 编写 AbstractEmailClient 模板方法测试 — 覆盖：sendEmail 委托 doSendEmail、sendBatchEmail 部分失败场景（3 个请求第 2 个失败）、request 参数校验
- [x] 6.4 GREEN: 创建 AbstractEmailClient 抽象类，sendEmail 委托 doSendEmail，sendBatchEmail 通过 stream 逐个调用
- [x] 6.5 RED: 编写 JakartaEmailClient 实现测试 — 覆盖：成功发送返回 EmailResult(success=true)、SMTP 失败抛 ClientException(EMAIL_SEND_FAILED)、使用 Mock JavaMailSender
- [x] 6.6 GREEN: 创建 JakartaEmailClient（基于 JavaMailSender + MimeMessageHelper）
- [x] 6.7 RED: 编写 EmailProperties 测试 — 验证默认值（port=587/sslEnable=true/enabled=false）
- [x] 6.8 GREEN: 创建 EmailProperties（@ConfigurationProperties("middleware.email")），不使用 @Data
- [x] 6.9 RED: 编写 EmailAutoConfiguration 条件装配测试 — 覆盖：JavaMailSender 在 classpath 且 enabled=true 时注册、enabled=false 时不注册
- [x] 6.10 GREEN: 创建 EmailAutoConfiguration（@ConditionalOnClass(JavaMailSender.class) + @ConditionalOnProperty(enabled=true)），注册 JakartaEmailClient
- [x] 6.11 在 AutoConfiguration.imports 中追加注册 EmailAutoConfiguration
- [x] 6.12 验证 Email 模块：执行 mvn test -pl clients -Dtest="*Email*" 确认所有 Email 相关测试通过

## 7. SMS 模块实现

- [x] 7.1 RED: 编写 SmsClient 接口定义测试 — 验证接口包含 2 个方法签名（sendTemplateSms/sendVerifyCode）
- [x] 7.2 GREEN: 在 clients 创建 SmsClient 接口，声明 2 个方法
- [x] 7.3 RED: 编写 AbstractSmsClient 模板方法测试 — 覆盖：phoneNumber 为 null 抛 ClientException、phoneNumber 为空字符串抛 ClientException、正常调用 do* 扩展点
- [x] 7.4 GREEN: 创建 AbstractSmsClient 抽象类，统一 phoneNumber 校验/日志/异常包装
- [x] 7.5 RED: 编写 SmsProperties 测试 — 验证默认值（所有字段 null）
- [x] 7.6 GREEN: 创建 SmsProperties（@ConfigurationProperties("middleware.sms")），不使用 @Data
- [x] 7.7 [已取消：Logging fallback 已移除] 原任务：修改 app 中的 LoggingSmsClient 实现 clients 的 SmsClient 接口（调整 import）
- [x] 7.8 验证 SMS 模块：执行 mvn test -pl clients -Dtest="*Sms*" 确认所有 SMS 相关测试通过

## 8. Search 模块实现

- [x] 8.1 RED: 编写 SearchClient 接口定义测试 — 验证接口包含 3 个方法签名（search/index/deleteIndex）
- [x] 8.2 GREEN: 在 clients 创建 SearchClient 接口，声明 3 个方法
- [x] 8.3 RED: 编写 SimpleSearchClient 实现测试 — 覆盖：索引后可搜索（关键词匹配）、删除后搜索不到、空索引搜索返回空结果、多个 document 搜索过滤、分页参数正确
- [x] 8.4 GREEN: 创建 SimpleSearchClient（基于 ConcurrentHashMap<String, Map<String, Object>>），实现 SearchClient 接口
- [x] 8.5 RED: 编写 SearchProperties 测试 — 验证默认值（enabled=false/type="simple"）
- [x] 8.6 GREEN: 创建 SearchProperties（@ConfigurationProperties("middleware.search")），不使用 @Data
- [x] 8.7 RED: 编写 SearchAutoConfiguration 条件装配测试 — 覆盖：enabled=true 时注册 SimpleSearchClient、enabled=false 时不注册
- [x] 8.8 GREEN: 创建 SearchAutoConfiguration，注册 SimpleSearchClient Bean
- [x] 8.9 在 AutoConfiguration.imports 中追加注册 SearchAutoConfiguration
- [x] 8.10 验证 Search 模块：执行 mvn test -pl clients -Dtest="*Search*" 确认所有 Search 相关测试通过

## 9. 主应用模块适配（app）

- [x] 9.1 [已取消：Logging fallback 已移除，TechClientConfigure 已删除] 原任务：修改 TechClientConfigure：从直接注册 Logging 实现改为 @ConditionalOnMissingBean fallback 模式，所有 LoggingXxxClient 实现 clients 中对应接口
- [x] 9.2 移除 app 中的 CacheProperties/EmailProperties/SmsProperties/OssProperties（已移入 clients），更新 application-dev.yaml/application-test.yaml 配置前缀为 middleware.*
- [x] 9.3 调整 app 中所有测试的 import 路径（client 接口/DTO 从新包路径导入）
- [x] 9.4 调整 ArchUnit 架构测试：更新包扫描路径覆盖 clients 和 app 两个模块

## 10. 集成验证

- [x] 10.1 执行 mvn test 确认所有测试通过（现有 134 + 新增 clients 测试），BUILD SUCCESS
- [x] 10.2 执行 mvn spring-boot:run -pl app 确认应用正常启动
- [x] 10.3 [已取消：Logging fallback 已移除] 原任务：验证条件装配：在不引入 caffeine 依赖时 CacheClient fallback 到 LoggingCacheClient

## 11. 文档更新

- [x] 11.1 更新 AGENTS.md：项目结构索引新增 clients 模块说明，依赖管理新增模块依赖关系
- [x] 11.2 更新 docs/architecture/overview.md：新增模块依赖关系图（Mermaid）
- [x] 11.3 更新 docs/guides/getting-started.md：多模块构建命令说明

## 12. 一致性检查

- [x] 进行 artifact 文档、讨论结果的一致性检查（proposal → specs → tasks → design 三维度交叉比对）
