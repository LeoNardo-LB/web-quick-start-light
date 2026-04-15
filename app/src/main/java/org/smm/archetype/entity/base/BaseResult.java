package org.smm.archetype.entity.base;

import lombok.Getter;
import lombok.Setter;
import org.smm.archetype.exception.CommonErrorCode;
import org.smm.archetype.exception.ErrorCode;
import org.smm.archetype.shared.util.context.ScopedThreadContext;

import java.time.Instant;

/**
 * @param <T> 结果类型
 * @author Leonardo
 * @since 2025/7/14
 * 基础结果
 */
@Getter
@Setter
public class BaseResult<T> {

    /**
     * code
     */
    private int code;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 信息
     */
    private String message;

    /**
     * 数据
     */
    private T data;

    /**
     * traceId
     */
    private String traceId;

    /**
     * 时间戳
     */
    private Instant time;

    public static BaseResult<Void> fail() {
        return fail(CommonErrorCode.FAIL);
    }

    public static BaseResult<Void> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.message());
    }

    public static BaseResult<Void> fail(ErrorCode errorCode, String msg) {
        BaseResult<Void> result = new BaseResult<>();
        result.code = errorCode.code();
        result.success = false;
        result.message = msg;
        result.time = Instant.now();
        result.traceId = ScopedThreadContext.getTraceId();
        return result;
    }

    public static <T> BaseResult<T> success(T data) {
        BaseResult<T> result = new BaseResult<>();
        result.code = CommonErrorCode.SUCCESS.code();
        result.message = CommonErrorCode.SUCCESS.message();
        result.success = true;
        result.data = data;
        result.time = Instant.now();
        result.traceId = ScopedThreadContext.getTraceId();
        return result;
    }

}
