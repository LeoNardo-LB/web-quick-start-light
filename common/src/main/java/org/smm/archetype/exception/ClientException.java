package org.smm.archetype.exception;

public class ClientException extends BaseException {
    public ClientException(ErrorCode errorCode) {
        super(errorCode);
    }
    public ClientException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    public ClientException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
    public ClientException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
