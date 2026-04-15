package org.smm.archetype.shared.util.context;

public class ScopedThreadContext {

    private static final ScopedValue<Context> SCOPED = ScopedValue.newInstance();

    private ScopedThreadContext() {}

    public static void runWithContext(Runnable runnable, String userId, String traceId) {
        Context context = new Context(userId, traceId);
        ScopedValue.where(SCOPED, context).run(runnable);
    }

    // Package-private: for ContextRunnable/ContextCallable to capture the ScopedValue instance
    static ScopedValue<Context> getScoped() {
        return SCOPED;
    }

    public static String getUserId() {
        if (!SCOPED.isBound()) {
            return null;
        }
        return SCOPED.get().userId();
    }

    public static String getTraceId() {
        if (!SCOPED.isBound()) {
            return null;
        }
        return SCOPED.get().traceId();
    }

    public static Context getContext() {
        if (!SCOPED.isBound()) {
            return null;
        }
        return SCOPED.get();
    }

    public record Context(String userId, String traceId) {}

}
