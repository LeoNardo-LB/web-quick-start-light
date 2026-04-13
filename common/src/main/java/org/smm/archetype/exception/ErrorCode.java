package org.smm.archetype.exception;

public interface ErrorCode {
    int code();
    String message();

    /**
     * 国际化消息键，格式为 "error." + code()
     * <p>
     * 用于从 messages.properties 中查找对应语言的翻译
     */
    default String messageKey() {
        return "error." + code();
    }
}
