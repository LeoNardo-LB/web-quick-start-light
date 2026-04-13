## ADDED Requirements

### Requirement: EmailClient 接口定义
EmailClient 接口 SHALL 提供 3 个邮件操作方法：`sendEmail(EmailRequest)`、`sendEmail(EmailRequest, String serviceProvider)`、`sendBatchEmail(List<EmailRequest>)`。所有方法声明在 `org.smm.archetype.client.email.EmailClient` 接口中。

#### Scenario: EmailClient 接口方法完整性
- **WHEN** 编译 clients 模块
- **THEN** EmailClient 接口包含 3 个方法签名

### Requirement: AbstractEmailClient 模板方法
AbstractEmailClient SHALL 实现 EmailClient 接口，所有接口方法标记为 `final`。`sendEmail(request)` SHALL 委托给 `doSendEmail` 抽象方法。`sendEmail(request, serviceProvider)` SHALL 根据 serviceProvider 委托给对应的 `doSendEmail`。`sendBatchEmail` SHALL 通过 stream 逐个调用 `sendEmail` 并收集结果。

#### Scenario: 批量发送部分失败
- **WHEN** 调用 `sendBatchEmail` 传入 3 个请求，其中第 2 个发送失败
- **THEN** 返回 3 个 EmailResult，第 2 个的 success=false，其余 success=true

### Requirement: JakartaEmailClient 真实实现
JakartaEmailClient SHALL 继承 AbstractEmailClient，基于 Spring 的 `JavaMailSender`（来自 spring-boot-starter-mail）实现。SHALL 通过 `JavaMailSender.createMimeMessage()` + `MimeMessageHelper` 构建邮件，设置收件人、主题、内容。发送失败 SHALL 抛出 ClientException(ClientErrorCode.EMAIL_SEND_FAILED)。

#### Scenario: 成功发送邮件
- **WHEN** 调用 `sendEmail(EmailRequest)` 且 JavaMailSender 配置正确
- **THEN** 返回 EmailResult(success=true, messageId 非空)

#### Scenario: SMTP 连接失败
- **WHEN** 调用 `sendEmail(EmailRequest)` 但 SMTP 服务器不可达
- **THEN** 抛出 ClientException，错误码为 EMAIL_SEND_FAILED

### Requirement: EmailAutoConfiguration 条件装配
EmailAutoConfiguration SHALL 使用 `@AutoConfiguration` + `@ConditionalOnClass(JavaMailSender.class)` + `@ConditionalOnProperty(prefix="middleware.email", name="enabled", havingValue="true")` + `@EnableConfigurationProperties(EmailProperties.class)`。SHALL 在 `@ConditionalOnMissingBean` 条件下注册 JakartaEmailClient Bean。

#### Scenario: JavaMailSender 在 classpath 且 enabled=true 时注册
- **WHEN** JavaMailSender 类在 classpath 且 `middleware.email.enabled=true`
- **THEN** Spring 容器中存在 JakartaEmailClient 类型的 Bean

#### Scenario: enabled=false 时不注册
- **WHEN** `middleware.email.enabled=false` 或未配置
- **THEN** Spring 容器中不存在 JakartaEmailClient Bean

### Requirement: EmailProperties 配置属性
EmailProperties SHALL 使用 `@ConfigurationProperties("middleware.email")`，包含字段：host(String)、port(int, 默认587)、username(String)、password(String)、sslEnable(boolean, 默认true)、enabled(boolean, 默认false)。SHALL 不使用 @Data 注解。

#### Scenario: 默认配置值
- **WHEN** 不设置任何 middleware.email 配置
- **THEN** EmailProperties 的 port=587, sslEnable=true, enabled=false
