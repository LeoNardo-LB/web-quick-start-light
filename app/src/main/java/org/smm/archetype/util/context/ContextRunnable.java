package org.smm.archetype.util.context;

import org.slf4j.MDC;

import java.lang.ScopedValue;
import java.util.Map;

public class ContextRunnable implements Runnable {

    private final Runnable delegate;
    private final ScopedThreadContext.Context snapshot;
    private final ScopedValue<ScopedThreadContext.Context> scoped;
    private final Map<String, String> mdcContext;

    public ContextRunnable(Runnable delegate) {
        this.delegate = delegate;
        this.snapshot = ScopedThreadContext.getContext();
        this.scoped = ScopedThreadContext.getScoped();
        this.mdcContext = MDC.getCopyOfContextMap();
    }

    @Override
    public void run() {
        if (mdcContext != null) {
            MDC.setContextMap(mdcContext);
        }
        try {
            if (snapshot != null) {
                ScopedValue.where(scoped, snapshot).run(delegate);
            } else {
                delegate.run();
            }
        } finally {
            MDC.clear();
        }
    }
}
