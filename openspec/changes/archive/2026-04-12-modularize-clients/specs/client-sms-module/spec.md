## ADDED Requirements

### Requirement: SmsClient 接口定义
SmsClient 接口 SHALL 提供 2 个短信操作方法：`sendTemplateSms(SmsRequest)`、`sendVerifyCode(String phoneNumber, String code)`。所有方法声明在 `org.smm.archetype.client.sms.SmsClient` 接口中。

#### Scenario: SmsClient 接口方法完整性
- **WHEN** 编译 clients 模块
- **THEN** SmsClient 接口包含 2 个方法签名

### Requirement: AbstractSmsClient 模板方法
AbstractSmsClient SHALL 实现 SmsClient 接口，所有接口方法标记为 `final`，内部调用 `doSendTemplateSms` 和 `doSendVerifyCode` 抽象扩展点方法。SHALL 在模板方法中统一执行：phoneNumber 空值/格式校验、入/出日志记录、异常包装为 ClientException。

#### Scenario: phoneNumber 为空时抛出异常
- **WHEN** 调用 AbstractSmsClient 的任意方法并传入 null 或空字符串 phoneNumber
- **THEN** 抛出 ClientException，错误码为 SMS_SEND_FAILED

### Requirement: SmsProperties 配置属性
SmsProperties SHALL 使用 `@ConfigurationProperties("middleware.sms")`，包含字段：provider(String)、accessKeyId(String)、accessKeySecret(String)、signName(String)。SHALL 不使用 @Data 注解。本模块不提供真实实现，仅作为配置预留。

#### Scenario: 默认配置值
- **WHEN** 不设置任何 middleware.sms 配置
- **THEN** SmsProperties 的所有字段均为 null

<!-- SmsClient Logging fallback 需求已移除：LoggingSmsClient 和 TechClientConfigure 已删除，项目不再使用 fallback 机制。SMS 无真实实现时，不注入 SmsClient Bean 即可。 -->
