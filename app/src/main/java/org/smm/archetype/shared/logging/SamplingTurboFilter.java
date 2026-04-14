package org.smm.archetype.shared.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import lombok.Setter;
import org.slf4j.Marker;

import java.util.concurrent.atomic.AtomicInteger;

public class SamplingTurboFilter extends TurboFilter {

    private final AtomicInteger counter    = new AtomicInteger(0);

    @Setter
    private       double        sampleRate = 0.1;

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level,
                              String format, Object[] params, Throwable t) {
        if (level.isGreaterOrEqual(Level.ERROR))
            return FilterReply.NEUTRAL;
        if (sampleRate >= 1.0)
            return FilterReply.NEUTRAL;
        if (counter.incrementAndGet() % (int) (1 / sampleRate) == 0)
            return FilterReply.NEUTRAL;
        return FilterReply.DENY;
    }

}
