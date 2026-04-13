package org.smm.archetype.exception;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    SUCCESS(1000, "success"),
    FAIL(2000, "操作失败"),
    ILLEGAL_ARGUMENT(2001, "参数校验失败"),
    RPC_EXCEPTION(2002, "外部服务调用失败"),
    SYS_ERROR(5000, "系统异常"),
    UNKNOWN_ERROR(9999, "未知异常"),

    // Client 中间件错误码
    CACHE_OPERATION_FAILED(6001, "缓存操作失败"),
    OSS_OPERATION_FAILED(6101, "对象存储操作失败"),
    OSS_UPLOAD_FAILED(6102, "文件上传失败"),
    EMAIL_SEND_FAILED(6201, "邮件发送失败"),
    SMS_SEND_FAILED(6301, "短信发送失败"),
    SEARCH_OPERATION_FAILED(6401, "搜索操作失败"),

    // 限流错误码
    RATE_LIMIT_EXCEEDED(6501, "请求过于频繁，请稍后再试"),

    // 认证错误码
    AUTH_UNAUTHORIZED(6601, "未登录或登录已过期"),
    AUTH_BAD_CREDENTIALS(6602, "用户名或密码错误"),
    AUTH_USER_NOT_FOUND(6603, "用户不存在");

    private final int code;
    private final String message;

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
