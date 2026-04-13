package org.smm.archetype.exception;

public class BizException extends BaseException {
    public BizException(ErrorCode errorCode) {
        super(errorCode);
    }
    public BizException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    public BizException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
    public BizException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
