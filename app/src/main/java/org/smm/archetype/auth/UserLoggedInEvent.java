package org.smm.archetype.auth;

import org.smm.archetype.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * 用户登录成功事件。
 * <p>
 * 放在 auth 模块根包（API 包），其他模块可消费。
 */
public record UserLoggedInEvent(
    String eventId,
    Instant occurredAt,
    String username,
    String ip
) implements DomainEvent {

    public UserLoggedInEvent {
        if (eventId == null || eventId.isBlank()) {
            eventId = UUID.randomUUID().toString();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    /**
     * 便捷工厂方法
     */
    public static UserLoggedInEvent of(String username, String ip) {
        return new UserLoggedInEvent(null, null, username, ip);
    }
}
