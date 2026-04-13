## ADDED Requirements

### Requirement: Bean Validation 请求参数校验
系统 SHALL 支持 JSR 380 Bean Validation 请求参数校验。

Controller 方法参数 SHALL 使用 `@Valid` 或 `@Validated` 注解触发校验。
请求 DTO SHALL 使用标准约束注解（@NotBlank、@NotNull、@Size、@Pattern、@Email 等）定义校验规则。

#### Scenario: 校验请求体字段
- **WHEN** 客户端发送 POST 请求，body 中缺少 @NotBlank 标注的字段
- **THEN** 框架抛出 MethodArgumentNotValidException，全局异常处理器返回包含所有字段错误信息的 BaseResult

#### Scenario: 校验路径变量和请求参数
- **WHEN** 客户端发送请求，@RequestParam 或 @PathVariable 的约束不满足
- **THEN** 框架抛出 ConstraintViolationException，全局异常处理器返回错误信息

### Requirement: BaseRequest 校验规则
BaseRequest SHALL 定义基础校验规则：
- requestId 可选，若提供需满足一定格式
- BasePageRequest 的 pageNo 默认为 1，pageSize 默认为 20，pageSize 最大不超过 100

#### Scenario: 分页参数默认值
- **WHEN** 客户端不传 pageNo 和 pageSize
- **THEN** 系统使用默认值 pageNo=1, pageSize=20

#### Scenario: 分页参数超限
- **WHEN** 客户端传入 pageSize=500
- **THEN** 校验失败，返回错误提示 "pageSize 不能超过 100"

### Requirement: 校验异常统一响应格式
校验异常 SHALL 通过全局异常处理器返回统一 BaseResult 格式，code 为 2001（ILLEGAL_ARGUMENT），message 包含所有校验错误信息。

#### Scenario: 多个字段校验失败
- **WHEN** 请求体中有 3 个字段校验失败
- **THEN** 返回 BaseResult 的 message 包含所有 3 个字段的错误描述，用分号分隔
