package org.smm.archetype.exception;

public class SysException extends BaseException {
    public SysException(ErrorCode errorCode) {
        super(errorCode);
    }
    public SysException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    public SysException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
    public SysException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
