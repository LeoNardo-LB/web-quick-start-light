## ADDED Requirements

### Requirement: 三级异常类型
系统 SHALL 提供三种异常类型，继承自 BaseException（继承 RuntimeException）：
- BizException：业务逻辑异常（参数校验失败、业务规则违反等）
- ClientException：外部服务调用异常（短信发送超时、邮件发送失败等）
- SysException：系统内部异常（不可预期的系统错误）

每种异常类型 SHALL 持有一个 ErrorCode 实例。

#### Scenario: 抛出业务异常
- **WHEN** Service 层检测到业务规则违反（如配置项不存在）
- **THEN** 抛出 BizException，携带对应的 ErrorCode 和可选的自定义消息

#### Scenario: 抛出外部服务异常
- **WHEN** 调用外部服务失败（如短信接口超时）
- **THEN** 抛出 ClientException，携带对应的 ErrorCode 和原始异常

#### Scenario: 抛出系统异常
- **WHEN** 系统发生不可预期错误（如空指针、IO异常）
- **THEN** 抛出 SysException，携带对应的 ErrorCode 和原始异常

### Requirement: ErrorCode 接口
系统 SHALL 定义 ErrorCode 接口，包含 `code()` 和 `message()` 方法。
系统 SHALL 提供 CommonErrorCode 枚举实现，包含通用错误码（如 SYS-001、BIZ-001、BIZ-002 等）。
各模块 MAY 定义自己的 ErrorCode 枚举实现。

#### Scenario: 使用通用错误码
- **WHEN** 需要抛出一个通用的业务异常
- **THEN** 使用 `CommonErrorCode` 中预定义的错误码创建 BizException

#### Scenario: 扩展模块错误码
- **WHEN** 某个模块需要专用的错误码（如 SMS-TIMEOUT）
- **THEN** 创建新的枚举实现 ErrorCode 接口，定义模块专属错误码

### Requirement: 全局异常处理器增强
WebExceptionAdvise SHALL 处理以下异常类型并返回统一 BaseResult：

**所有异常统一返回 HTTP 200 + BaseResult 封装**（不使用 HTTP 状态码区分错误类型，而是通过 BaseResult 中的 code 字段区分）。这样做的原因是前端可以统一用相同的解析逻辑处理所有响应。

异常处理映射：
- MethodArgumentNotValidException → BaseResult(code=2001, message=所有字段错误信息拼接)
- ConstraintViolationException → BaseResult(code=2001, message=约束违反信息)
- BindException → BaseResult(code=2001, message=绑定错误信息)
- BizException → BaseResult(code=ErrorCode.code, message=ErrorCode.message)
- ClientException → BaseResult(code=ErrorCode.code, message=ErrorCode.message)
- SysException → BaseResult(code=5000, message="系统异常")
- Exception（兜底）→ BaseResult(code=9999, message="未知异常")

#### Scenario: 参数校验异常处理
- **WHEN** Controller 方法参数使用 @Valid 校验失败
- **THEN** 全局异常处理器捕获 MethodArgumentNotValidException，返回 HTTP 200 + BaseResult(code=2001, message=所有字段的错误信息)

#### Scenario: 外部服务异常处理
- **WHEN** Service 层抛出 ClientException
- **THEN** 返回 HTTP 200 + BaseResult(code=ErrorCode.code, message=ErrorCode.message)

#### Scenario: 系统异常处理
- **WHEN** 系统抛出 SysException 或未知 Exception
- **THEN** 返回 HTTP 200 + BaseResult(code=5000 或 9999)，不暴露内部堆栈信息
