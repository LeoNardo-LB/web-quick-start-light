## ADDED Requirements

### Requirement: CacheClient 接口扩展
CacheClient 接口 SHALL 扩展为 10 个方法：get、getList（两个重载）、put（两个重载）、append、delete、hasKey、expire、getExpire。AbstractCacheClient 的公开方法 SHALL 为 final。

#### Scenario: CacheClient 方法数正确
- **WHEN** 查看 CacheClient 接口
- **THEN** 包含 10 个方法签名

#### Scenario: AbstractCacheClient 公开方法为 final
- **WHEN** 查看 AbstractCacheClient
- **THEN** 所有 public 方法标注为 `final`，子类通过 `do*` 扩展点实现

### Requirement: CaffeineCacheClientImpl 替换现有实现
client-cache 模块 SHALL 用 speccoding 的 CaffeineCacheClientImpl 替换现有 CaffeineCacheClient，包含 CacheValueWrapper 和 CaffeineExpiry 实现。

#### Scenario: Caffeine 客户端支持每 entry 过期
- **WHEN** put(key, value, duration) 设置带过期时间的缓存
- **THEN** 该 entry 在指定时间后自动过期，不影响其他 entry

### Requirement: OssClient 增强
OssClient 接口 SHALL 扩展为 7 个方法（upload/download/delete/generateUrl/searchFiles/exists/getFileSize）。AbstractOssClient 的公开方法 SHALL 为 final。

#### Scenario: OssClient 方法数正确
- **WHEN** 查看 OssClient 接口
- **THEN** 包含 7 个方法签名

### Requirement: EmailClient 替换为阿里云实现
client-email 模块 SHALL 用 speccoding 的 AliyunEmailClientImpl 替换 JakartaEmailClient。支持 ServiceProvider 枚举和批量发送。新增 aliyun-java-sdk-dm 依赖。

#### Scenario: EmailClient 支持服务提供商
- **WHEN** 调用 sendEmail(request, ServiceProvider.ALIYUN)
- **THEN** 使用阿里云 SDK 发送邮件

### Requirement: SmsClient 新增阿里云实现
client-sms 模块 SHALL 新增 AliyunSmsClientImpl（speccoding 实现）。新增 aliyun-java-sdk-dysmsapi 依赖。

#### Scenario: SmsClient 有真实实现
- **WHEN** 配置 `middleware.sms.enabled=true` 和阿里云 AccessKey
- **THEN** AliyunSmsClientImpl Bean 注册，可发送短信

### Requirement: SearchClient 替换为 Elasticsearch 实现
client-search 模块 SHALL 用 speccoding 的 ElasticsearchClientImpl 替换 SimpleSearchClient（ConcurrentHashMap）。接口扩展为 15+ 个方法，包含索引/搜索/聚合/向量搜索。

#### Scenario: SearchClient 使用 Elasticsearch
- **WHEN** 配置 Elasticsearch 连接信息
- **THEN** ElasticsearchClientImpl Bean 注册，支持索引和搜索操作

### Requirement: 保留独立 Maven 子模块结构
5 个客户端模块 SHALL 保留独立 Maven 子模块结构（client-cache、client-oss、client-email、client-sms、client-search）。

#### Scenario: 模块结构不变
- **WHEN** 查看项目根目录的 clients/ 目录
- **THEN** 包含 5 个子目录：client-cache、client-oss、client-email、client-sms、client-search

### Requirement: 客户端条件装配
所有客户端实现 SHALL 通过条件装配（`@ConditionalOnProperty` 或 `@ConditionalOnBean`）控制 Bean 注册。未配置相关服务时 SHALL NOT 注册客户端 Bean。

#### Scenario: 未配置 ES 时不注册 SearchClient
- **WHEN** 未配置 Elasticsearch 连接信息
- **THEN** SearchClient Bean 不注册，应用正常启动

#### Scenario: 未配置阿里云 AccessKey 时不注册 SmsClient
- **WHEN** 未配置 `middleware.sms.enabled=true`
- **THEN** AliyunSmsClientImpl Bean 不注册
