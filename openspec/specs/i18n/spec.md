## ADDED Requirements

### Requirement: ErrorCode 国际化支持
ErrorCode 接口 SHALL 新增 `messageKey()` 默认方法，返回格式为 `error.{code}` 的国际化消息 Key。`message()` 方法保持不变（向后兼容），返回默认中文消息。

#### Scenario: ErrorCode 提供 messageKey
- **WHEN** 调用 CommonErrorCode.FAIL.messageKey()
- **THEN** SHALL 返回 "error.2000"

#### Scenario: message() 保持向后兼容
- **WHEN** 调用 CommonErrorCode.FAIL.message()
- **THEN** SHALL 返回 "操作失败"（不变）

### Requirement: WebExceptionAdvise 国际化消息解析
WebExceptionAdvise SHALL 注入 MessageSource，根据请求的 Accept-Language Header 解析 Locale，使用 messageKey 查询国际化消息。当找不到翻译时 SHALL 回退到默认中文。

#### Scenario: 请求携带 Accept-Language: en 时返回英文错误消息
- **WHEN** 客户端发送请求 Header `Accept-Language: en` 且触发业务异常
- **THEN** 响应中的 message 字段 SHALL 为英文翻译

#### Scenario: 请求不携带 Accept-Language 时返回默认中文
- **WHEN** 客户端发送请求不携带 Accept-Language Header 且触发业务异常
- **THEN** 响应中的 message 字段 SHALL 为中文（默认）

#### Scenario: 找不到翻译时回退到默认消息
- **WHEN** 请求的 Locale 没有对应的翻译文件
- **THEN** 系统 SHALL 回退到 ErrorCode.message() 的默认值

### Requirement: Bean Validation 消息国际化
系统 SHALL 提供国际化 Bean Validation 消息资源文件。`ValidationMessages.properties` 为默认中文，`ValidationMessages_en.properties` 为英文。

#### Scenario: 校验失败时返回对应语言的消息
- **WHEN** 客户端发送请求 Header `Accept-Language: en` 且参数校验失败
- **THEN** 校验错误消息 SHALL 为英文翻译

### Requirement: 国际化资源文件
系统 SHALL 提供 `messages.properties`（默认中文）和 `messages_en.properties`（英文）资源文件，包含所有 ErrorCode 的翻译。

#### Scenario: 资源文件包含所有错误码翻译
- **WHEN** 检查 messages.properties
- **THEN** SHALL 包含 key 格式为 `error.{code}` 的所有 CommonErrorCode 翻译
