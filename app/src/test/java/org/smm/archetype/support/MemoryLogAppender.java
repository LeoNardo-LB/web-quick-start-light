package org.smm.archetype.support;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MemoryLogAppender extends AppenderBase<ILoggingEvent> {
    private static final List<ILoggingEvent> events = Collections.synchronizedList(new ArrayList<>());

    @Override
    protected void append(ILoggingEvent event) {
        events.add(event);
    }

    public static List<ILoggingEvent> getEvents() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }

    public static void clear() {
        events.clear();
    }
}
