package org.smm.archetype.client.dto;

public record EmailResult(
    boolean success,
    String messageId,
    String message
) {
    public static EmailResult success(String messageId) {
        return new EmailResult(true, messageId, "发送成功");
    }

    public static EmailResult fail(String message) {
        return new EmailResult(false, null, message);
    }
}
