package org.smm.archetype.util.context;

import org.slf4j.MDC;

import java.lang.ScopedValue;
import java.util.Map;
import java.util.concurrent.Callable;

public class ContextCallable<V> implements Callable<V> {

    private final Callable<V> delegate;
    private final ScopedThreadContext.Context snapshot;
    private final ScopedValue<ScopedThreadContext.Context> scoped;
    private final Map<String, String> mdcContext;

    public ContextCallable(Callable<V> delegate) {
        this.delegate = delegate;
        this.snapshot = ScopedThreadContext.getContext();
        this.scoped = ScopedThreadContext.getScoped();
        this.mdcContext = MDC.getCopyOfContextMap();
    }

    @Override
    public V call() throws Exception {
        if (mdcContext != null) {
            MDC.setContextMap(mdcContext);
        }
        try {
            if (snapshot != null) {
                final V[] result = (V[]) new Object[1];
                final Exception[] exception = new Exception[1];
                ScopedValue.where(scoped, snapshot).run(() -> {
                    try {
                        result[0] = delegate.call();
                    } catch (Exception e) {
                        exception[0] = e;
                    }
                });
                if (exception[0] != null) {
                    throw exception[0];
                }
                return result[0];
            } else {
                return delegate.call();
            }
        } finally {
            MDC.clear();
        }
    }
}
