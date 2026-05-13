package org.smm.archetype.shared.event;

/**
 * 领域事件发布接口。
 * <p>
 * 零 Spring 依赖，由 Spring 适配器实现。
 */
@FunctionalInterface
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
