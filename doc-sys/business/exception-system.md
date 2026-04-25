# 异常体系

> **职责**: 描述统一异常体系和错误码规范
> **轨道**: Contract
> **维护者**: AI

## 目录

- [概述](#概述)

## 概述

异常体系是项目 `common` 模块的核心输出，通过 `BaseException` 抽象根类建立三级异常分类（业务异常 / 客户端异常 / 系统异常），配合 `ErrorCode` 接口和 `CommonErrorCode` 枚举提供标准化的错误码语义。全局异常处理器 `WebExceptionAdvise` 依据异常类型决定 HTTP 响应状态码和日志级别，并通过 `MessageSource` 实现国际化消息翻译。

## 公共 API 参考

### ErrorCode 接口

```java
package org.smm.archetype.exception;

public interface ErrorCode {
    int code();                          // 错误码数字标识
    String message();                    // 默认中文消息

    default String messageKey() {        // 国际化消息键，格式 "error." + code()
        return "error." + code();
    }
}
```

**契约说明**：所有错误码实现类必须提供 `code()`（唯一数字标识）和 `message()`（中文默认消息）。`messageKey()` 提供默认实现，用于从 `messages.properties` 查找对应语言的翻译；找不到时回退到 `message()` 的默认值。

### BaseException 抽象类

```java
package org.smm.archetype.exception;

@Getter
public abstract class BaseException extends RuntimeException {
    private final ErrorCode errorCode;

    protected BaseException(ErrorCode errorCode);
    protected BaseException(ErrorCode errorCode, String message);
    protected BaseException(ErrorCode errorCode, Throwable cause);
    protected BaseException(ErrorCode errorCode, String message, Throwable cause);
}
```

**契约说明**：所有自定义异常必须继承 `BaseException`，通过 `errorCode` 属性携带结构化错误信息。四种构造模式支持不同场景：

| 构造器 | 适用场景 | `getMessage()` 返回值 |
|--------|---------|---------------------|
| `(ErrorCode)` | 使用默认消息（走 i18n） | `errorCode.message()` |
| `(ErrorCode, String)` | 自定义消息（不走 i18n） | 自定义 `message` |
| `(ErrorCode, Throwable)` | 包装底层异常 | `errorCode.message()` |
| `(ErrorCode, String, Throwable)` | 自定义消息 + 底层异常 | 自定义 `message` |

### BizException — 业务异常

```java
public class BizException extends BaseException {
    public BizException(ErrorCode errorCode);
    public BizException(ErrorCode errorCode, String message);
    public BizException(ErrorCode errorCode, Throwable cause);
    public BizException(ErrorCode errorCode, String message, Throwable cause);
}
```

**语义**：可预期的业务规则违反，如"用户不存在"、"参数校验失败"。全局处理器以 `WARN` 级别记录日志，HTTP 200 + 业务错误码返回。

**使用方**：`app` 模块（Controller / Service / Facade / AOP）、`component-auth`。

### ClientException — 客户端/中间件异常

```java
public class ClientException extends BaseException {
    public ClientException(ErrorCode errorCode);
    public ClientException(ErrorCode errorCode, String message);
    public ClientException(ErrorCode errorCode, Throwable cause);
    public ClientException(ErrorCode errorCode, String message, Throwable cause);
}
```

**语义**：外部依赖失败，如缓存操作失败、邮件发送失败、OSS 上传失败。全局处理器以 `ERROR` 级别记录日志（含堆栈），HTTP 200 + 错误码返回。

**使用方**：`component-cache`、`component-email`、`component-oss`、`component-search`、`component-sms`。

### SysException — 系统级异常

```java
public class SysException extends BaseException {
    public SysException(ErrorCode errorCode);
    public SysException(ErrorCode errorCode, String message);
    public SysException(ErrorCode errorCode, Throwable cause);
    public SysException(ErrorCode errorCode, String message, Throwable cause);
}
```

**语义**：内部不可恢复错误，如系统内部异常。全局处理器以 `ERROR` 级别记录日志（含堆栈），HTTP 200 + 错误码返回。

### CommonErrorCode 枚举

```java
@AllArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    // 1xxx — 成功
    SUCCESS(1000, "success"),

    // 2xxx — 通用业务错误
    FAIL(2000, "操作失败"),
    ILLEGAL_ARGUMENT(2001, "参数校验失败"),
    RPC_EXCEPTION(2002, "外部服务调用失败"),

    // 5xxx — 系统错误
    SYS_ERROR(5000, "系统异常"),

    // 6xxx — 中间件/基础设施错误
    CACHE_OPERATION_FAILED(6001, "缓存操作失败"),
    OSS_OPERATION_FAILED(6101, "对象存储操作失败"),
    OSS_UPLOAD_FAILED(6102, "文件上传失败"),
    EMAIL_SEND_FAILED(6201, "邮件发送失败"),
    SMS_SEND_FAILED(6301, "短信发送失败"),
    SEARCH_OPERATION_FAILED(6401, "搜索操作失败"),
    RATE_LIMIT_EXCEEDED(6501, "请求过于频繁，请稍后再试"),

    // 6xxx — 认证错误
    AUTH_UNAUTHORIZED(6601, "未登录或登录已过期"),
    AUTH_BAD_CREDENTIALS(6602, "用户名或密码错误"),
    AUTH_USER_NOT_FOUND(6603, "用户不存在"),

    // 9xxx — 兜底
    UNKNOWN_ERROR(9999, "未知异常");

    private final int code;
    private final String message;
}
```

## 核心类型定义

### 异常层级结构

```
RuntimeException
  └── BaseException (abstract, 持有 ErrorCode)
        ├── BizException       — 业务逻辑异常（可预期的业务规则违反）
        ├── ClientException    — 客户端/中间件调用异常（外部依赖失败）
        └── SysException       — 系统级异常（内部不可恢复错误）
```

### 错误码分段规范

| 码段范围 | 语义 | 枚举值 | 说明 |
|:--------:|------|--------|------|
| `1xxx` | 成功 | `SUCCESS(1000)` | 唯一的成功码 |
| `2xxx` | 通用业务错误 | `FAIL(2000)`, `ILLEGAL_ARGUMENT(2001)`, `RPC_EXCEPTION(2002)` | 可预期的业务规则违反 |
| `5xxx` | 系统内部错误 | `SYS_ERROR(5000)` | 内部不可恢复错误 |
| `6xxx` | 中间件/基础设施 | `CACHE(6001)` ~ `AUTH(6603)` | 缓存/OSS/邮件/短信/搜索/限流/认证 |
| `9xxx` | 未知兜底 | `UNKNOWN_ERROR(9999)` | 兜底异常处理 |

**扩展约定**：各业务模块可定义自己的 `XxxErrorCode implements ErrorCode`，但应使用不同的码段以避免冲突（如 `3xxx` 用于用户领域、`4xxx` 用于配置领域等）。

### 国际化支持

`ErrorCode.messageKey()` 默认生成格式 `error.{code}`，例如：

- `AUTH_BAD_CREDENTIALS(6602)` → `error.6602`
- `SYS_ERROR(5000)` → `error.5000`

`WebExceptionAdvise` 在处理异常时，通过 `MessageSource.getMessage(key, locale)` 查找翻译。若找不到对应 key 的翻译，回退到 `ErrorCode.message()` 返回的中文默认值。

**消息解析策略**（`WebExceptionAdvise.resolveExceptionMessage()`）：

| 构造方式 | `getMessage()` | 是否走 i18n |
|---------|----------------|:-----------:|
| `new BizException(ErrorCode)` | 等于 `ErrorCode.message()` | ✅ 是 |
| `new BizException(ErrorCode, "自定义")` | 自定义字符串 | ❌ 否，直接返回 |

## 服务流程

### 全局异常处理流程

```
HTTP 请求
    │
    ▼
Controller 方法
    │
    ├── 正常返回 → BaseResult.success(data)
    │
    └── 抛出异常
            │
            ▼
    WebExceptionAdvise (@RestControllerAdvice)
            │
            ├── BizException
            │   → resolveExceptionMessage() (i18n)
            │   → log.warn()
            │   → HTTP 200 + BaseResult.fail(errorCode, message)
            │
            ├── ClientException
            │   → resolveExceptionMessage() (i18n)
            │   → log.error() (含堆栈)
            │   → HTTP 200 + BaseResult.fail(errorCode, message)
            │
            ├── SysException
            │   → resolveExceptionMessage() (i18n)
            │   → log.error() (含堆栈)
            │   → HTTP 200 + BaseResult.fail(errorCode, message)
            │
            ├── NotLoginException (Sa-Token)
            │   → resolveMessage() (i18n)
            │   → log.warn()
            │   → HTTP 401 + BaseResult.fail(AUTH_UNAUTHORIZED)
            │
            ├── MethodArgumentNotValidException / BindException
            │   → 拼接字段错误信息
            │   → log.warn()
            │   → HTTP 200 + BaseResult.fail(ILLEGAL_ARGUMENT, message)
            │
            ├── NoResourceFoundException
            │   → log.warn()
            │   → HTTP 404 + BaseResult.fail(FAIL)
            │
            └── Exception (兜底)
                → log.error() (含堆栈)
                → HTTP 200 + BaseResult.fail(UNKNOWN_ERROR)
```

### 异常类型 → 日志级别 → HTTP 状态码映射

| 异常类型 | 日志级别 | HTTP 状态码 | 说明 |
|---------|:-------:|:---------:|------|
| `BizException` | WARN | 200 | 业务异常，可预期 |
| `ClientException` | ERROR | 200 | 外部依赖失败 |
| `SysException` | ERROR | 200 | 内部系统异常 |
| `NotLoginException` | WARN | 401 | 认证过期 |
| `NoResourceFoundException` | WARN | 404 | 资源不存在 |
| `MethodArgumentNotValidException` | WARN | 200 | 参数校验失败 |
| `BindException` | WARN | 200 | 参数绑定失败 |
| `ConstraintViolationException` | WARN | 200 | 约束违反 |
| `Exception` (兜底) | ERROR | 200 | 未知异常 |

## 依赖关系

### 外部依赖（Maven）

| 依赖 | 范围 | 用途 |
|------|------|------|
| `spring-boot-starter` | compile | Spring Boot 基础（间接依赖） |
| `lombok` | compile (optional) | `@Getter` / `@AllArgsConstructor` 注解驱动 |
| `spring-boot-starter-test` | test | JUnit 5 + AssertJ 测试框架 |

**说明**：核心逻辑零 Spring 耦合（异常类不依赖任何 Spring API），仅用 Lombok 减少样板代码。

### 模块间依赖

| 依赖方模块 | 使用类型 | 典型引用场景 |
|-----------|---------|-------------|
| **app** | `BizException`, `ClientException`, `SysException`, `CommonErrorCode`, `ErrorCode` | `WebExceptionAdvise`、Controller、Service、Facade、AOP 切面、`BaseResult` |
| **component-cache** | `ClientException`, `CommonErrorCode` | 缓存操作失败时抛出 |
| **component-email** | `ClientException`, `CommonErrorCode` | 邮件发送失败时抛出 |
| **component-oss** | `ClientException`, `CommonErrorCode` | 对象存储/上传失败时抛出 |
| **component-search** | `ClientException`, `CommonErrorCode` | 搜索操作失败时抛出 |
| **component-sms** | `ClientException`, `CommonErrorCode` | 短信发送失败时抛出 |
| **component-auth** | `BizException`, `CommonErrorCode` | 认证失败时抛出 |

### 使用模式

- **app 模块**：使用全部异常类型，`WebExceptionAdvise` 是核心消费点
- **components 模块**：基础设施组件统一使用 `ClientException`（表示外部依赖失败），认证组件使用 `BizException`（表示业务逻辑拒绝）

## 相关文档

| 文档 | 关系 | 说明 |
|------|------|------|
| 基础类型 | business/base-types.md | BaseResult 使用 CommonErrorCode |
| REST API 参考 | business/api-reference.md | 全局异常处理决定 API 错误响应格式 |
| 认证组件 | components/component-auth.md | 使用 BizException 处理认证失败 |
| 配置参考 | guides/configuration-reference.md | MessageSource 配置影响 i18n 行为 |

## 变更历史

| 日期 | 版本 | 变更内容 |
|------|------|---------|
| 2026-04-25 | v1.0 | 初始版本：异常体系文档生成 |
