## MODIFIED Requirements

### Requirement: BaseResult 统一响应封装
BaseResult SHALL 作为所有 API 的统一响应封装，包含以下字段：
- code: Integer（响应码，1000 表示成功）
- success: Boolean（是否成功）
- message: String（响应消息）
- data: T（响应数据，泛型）
- traceId: String（请求追踪 ID，自动从 ScopedThreadContext 获取）
- requestId: String（请求 ID）
- time: Long（**新增**，服务器处理时间戳，毫秒）

BaseResult SHALL 提供静态工厂方法：
- `success(T data)`：成功响应，自动填充 traceId、time
- `fail(ErrorCode errorCode)`：失败响应，自动填充 traceId、time
- `fail(ErrorCode errorCode, String message)`：失败响应，自定义消息

#### Scenario: 成功响应包含 time 字段
- **WHEN** Controller 调用 BaseResult.success(data)
- **THEN** 返回的 JSON 包含 `time: 1712836800000`（当前时间戳）

#### Scenario: 失败响应自动填充 traceId
- **WHEN** 全局异常处理器捕获异常并调用 BaseResult.fail()
- **THEN** 返回的 JSON 中 traceId 自动从 ScopedThreadContext 获取

### Requirement: ResultCode 扩展为 ErrorCode 接口
原有的 ResultCode 枚举 SHALL 被重构为迁移过渡态：
- 定义 ErrorCode 接口，包含 `code()` 和 `message()` 方法
- ResultCode 枚举标记为 @Deprecated，内部委托到 CommonErrorCode 实现方法（保持向后兼容，编译通过但 IDE 会提示迁移）
- CommonErrorCode 枚举合并 ResultCode 的所有值并新增通用错误码：
  - SUCCESS(1000, "success")
  - FAIL(2000, "操作失败")
  - ILLEGAL_ARGUMENT(2001, "参数校验失败")
  - RPC_EXCEPTION(2002, "外部服务调用失败") — 从原 ResultCode 迁移
  - SYS_ERROR(5000, "系统异常")
  - UNKNOWN_ERROR(9999, "未知异常")
- 新代码 SHALL 使用 CommonErrorCode，不应使用 ResultCode

#### Scenario: 使用 CommonErrorCode
- **WHEN** 需要返回通用错误
- **THEN** 使用 BaseResult.fail(CommonErrorCode.SYS_ERROR)

#### Scenario: 原有 ResultCode 仍可编译
- **WHEN** 已有代码使用 ResultCode.SUCCESS
- **THEN** 编译通过（ResultCode 委托到 CommonErrorCode.SUCCESS），IDE 标记为 @Deprecated

#### Scenario: RPC_EXCEPTION 迁移
- **WHEN** 需要抛出外部服务调用失败异常
- **THEN** 使用 CommonErrorCode.RPC_EXCEPTION（从原 ResultCode 迁移而来）
