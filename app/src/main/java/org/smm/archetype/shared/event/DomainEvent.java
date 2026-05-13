package org.smm.archetype.shared.event;

import java.time.Instant;

/**
 * 领域事件接口。
 * <p>
 * 所有领域事件 record 实现此接口，提供 eventId 和 occurredAt。
 * 零 Spring 依赖。
 */
public interface DomainEvent {

    /**
     * 事件唯一标识
     */
    String eventId();

    /**
     * 事件发生时间
     */
    Instant occurredAt();
}
