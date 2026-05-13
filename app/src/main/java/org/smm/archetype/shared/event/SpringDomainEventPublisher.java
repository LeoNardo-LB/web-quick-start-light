package org.smm.archetype.shared.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Spring ApplicationEventPublisher 适配实现。
 */
@Component
@RequiredArgsConstructor
class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher delegate;

    @Override
    public void publish(DomainEvent event) {
        delegate.publishEvent(event);
    }
}
