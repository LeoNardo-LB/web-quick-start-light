## ADDED Requirements

### Requirement: 技术客户端接口定义
系统 SHALL 在 service/client/ 包下定义以下技术客户端接口：
- CacheClient：缓存操作（put / get / remove / evict）
- EmailClient：邮件发送（sendTemplateEmail / sendSimpleEmail）
- SmsClient：短信发送（sendTemplateSms / sendVerifyCode）
- OssClient：对象存储（upload / download / getSignedUrl / delete）
- SearchClient：全文搜索（search / index / deleteIndex）

每个接口 SHALL 定义对应的 Request/Result DTO。

#### Scenario: 使用缓存客户端
- **WHEN** Service 层需要缓存数据
- **THEN** 注入 CacheClient 接口，调用 put(key, value, expireSeconds) 方法

#### Scenario: 切换缓存实现
- **WHEN** 从 Caffeine 本地缓存切换到 Redis
- **THEN** 仅需在 @Configuration 中注册不同的 CacheClient 实现 Bean，Service 层代码无需修改

### Requirement: 技术客户端默认空实现
系统 SHALL 为每个技术客户端接口提供默认的日志空实现（LoggingXxxClient），在不配置具体中间件时使用，仅记录操作日志不执行实际操作。

#### Scenario: 未配置邮件服务时发送邮件
- **WHEN** 调用 EmailClient.sendTemplateEmail()，且未配置真实邮件服务
- **THEN** LoggingEmailClient 记录日志 "Email sent to {to} with template {template}"，不执行实际发送

### Requirement: 客户端 DTO 定义
技术客户端接口的 Request/Result DTO SHALL 定义在 service/client/dto/ 包下：
- SmsRequest / SmsResult
- EmailRequest / EmailResult
- SearchQuery / SearchResult
- OssUploadRequest / OssUploadResult

DTO SHALL 使用 Java record 类型定义（不可变）。

#### Scenario: 构建短信发送请求
- **WHEN** 需要发送短信验证码
- **THEN** 创建 `new SmsRequest(phoneNumber, templateId, Map.of("code", "123456"))`，通过 SmsClient 发送
