package org.smm.archetype.component.dto;

public record SmsResult(
    boolean success,
    String requestId,
    String message
) {
    public static SmsResult success(String requestId) {
        return new SmsResult(true, requestId, "发送成功");
    }

    public static SmsResult fail(String message) {
        return new SmsResult(false, null, message);
    }
}
