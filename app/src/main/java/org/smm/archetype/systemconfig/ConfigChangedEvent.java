package org.smm.archetype.systemconfig;

import org.smm.archetype.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * 系统配置变更事件。
 * <p>
 * 放在 systemconfig 模块根包（API 包），其他模块可消费。
 */
public record ConfigChangedEvent(
    String eventId,
    Instant occurredAt,
    String configKey,
    String oldValue,
    String newValue
) implements DomainEvent {

    public ConfigChangedEvent {
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
    public static ConfigChangedEvent of(String configKey, String oldValue, String newValue) {
        return new ConfigChangedEvent(null, null, configKey, oldValue, newValue);
    }
}
