package org.smm.archetype.config;

import io.opentelemetry.context.Context;
import org.jspecify.annotations.NonNull;
import org.smm.archetype.config.properties.ThreadPoolProperties;
import org.smm.archetype.shared.util.context.BizContext;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 线程池配置。
 * <p>
 * 所有 Executor Bean 统一使用 {@link ContextPropagatingTaskDecorator}，
 * 在每次异步任务提交时自动传播三种上下文：
 * <ol>
 *   <li>BizContext（ScopedValue 业务上下文：userId 等）</li>
 *   <li>OTel Context（Span + Baggage：traceId、跨服务传播）</li>
 *   <li>MDC（日志上下文：traceId、spanId）</li>
 * </ol>
 */
@Configuration
@EnableConfigurationProperties(ThreadPoolProperties.class)
public class ThreadPoolConfigure {

    /**
     * 三合一上下文传播 TaskDecorator。
     * <p>
     * 在 {@code decorate()} 时（调用线程）捕获三种上下文，
     * 返回的 Runnable 在工作线程中恢复三种上下文。
     */
    static final class ContextPropagatingTaskDecorator implements TaskDecorator {
        @Override
        public @NonNull Runnable decorate(@NonNull Runnable runnable) {
            BizContext.Holder replica = BizContext.copyAsReplica();
            ScopedValue<BizContext.Holder> scoped = BizContext.getScoped();
            Context otelContext = Context.current();
            Map<String, String> mdcContext = MDC.getCopyOfContextMap();

            return () -> {
                Map<String, String> previousMdc = MDC.getCopyOfContextMap();
                if (mdcContext != null) {
                    MDC.setContextMap(mdcContext);
                }
                try {
                    Runnable task = runnable;
                    if (replica != null) {
                        task = () -> ScopedValue.where(scoped, replica).run(runnable);
                    }
                    otelContext.wrap(task).run();
                } finally {
                    if (previousMdc != null) {
                        MDC.setContextMap(previousMdc);
                    } else {
                        MDC.clear();
                    }
                }
            };
        }
    }

    /**
     * Callable 包装：捕获三种上下文，在工作线程中恢复。
     */
    private static <T> Callable<T> wrapCallable(Callable<T> callable) {
        BizContext.Holder replica = BizContext.copyAsReplica();
        ScopedValue<BizContext.Holder> scoped = BizContext.getScoped();
        Context otelContext = Context.current();
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();

        return () -> {
            Map<String, String> previousMdc = MDC.getCopyOfContextMap();
            if (mdcContext != null) MDC.setContextMap(mdcContext);
            try {
                if (replica != null) {
                    final T[] result = (T[]) new Object[1];
                    final Exception[] ex = new Exception[1];
                    ScopedValue.where(scoped, replica).run(() -> {
                        try {
                            result[0] = otelContext.wrap(callable).call();
                        } catch (Exception e) {
                            ex[0] = e;
                        }
                    });
                    if (ex[0] != null) throw ex[0];
                    return result[0];
                }
                return otelContext.wrap(callable).call();
            } finally {
                if (previousMdc != null) MDC.setContextMap(previousMdc);
                else MDC.clear();
            }
        };
    }

    private static final ContextPropagatingTaskDecorator CONTEXT_PROPAGATING_DECORATOR = new ContextPropagatingTaskDecorator();

    @Bean("ioThreadPool")
    public Executor ioThreadPool(ThreadPoolProperties props) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.getIoCoreSize());
        executor.setMaxPoolSize(props.getIoMaxSize());
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("io-pool-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setTaskDecorator(CONTEXT_PROPAGATING_DECORATOR);
        executor.initialize();
        return executor;
    }

    @Bean("cpuThreadPool")
    public Executor cpuThreadPool(ThreadPoolProperties props) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.getCpuCoreSize());
        executor.setMaxPoolSize(props.getCpuCoreSize());
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("cpu-pool-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setTaskDecorator(CONTEXT_PROPAGATING_DECORATOR);
        executor.initialize();
        return executor;
    }

    @Bean("daemonThreadPool")
    public Executor daemonThreadPool(ThreadPoolProperties props) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.getDaemonCoreSize());
        executor.setMaxPoolSize(props.getDaemonCoreSize());
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("daemon-pool-");
        executor.setDaemon(true);
        executor.setTaskDecorator(CONTEXT_PROPAGATING_DECORATOR);
        executor.initialize();
        return executor;
    }

    @Bean("schedulerThreadPool")
    public ScheduledExecutorService schedulerThreadPool(ThreadPoolProperties props) {
        ScheduledExecutorService raw = Executors.newScheduledThreadPool(
                props.getSchedulerPoolSize(),
                r -> {
                    Thread t = new Thread(r, "scheduler-pool-" + System.currentTimeMillis());
                    t.setDaemon(true);
                    return t;
                });
        return new ContextPropagatingScheduledExecutorService(raw);
    }

    /**
     * ScheduledExecutorService 装饰器：在每次任务提交时传播三种上下文。
     * <p>
     * 覆盖 execute/submit/schedule/scheduleAtFixedRate/scheduleWithFixedDelay。
     * 生命周期方法直接委托给底层 Executor。
     */
    static class ContextPropagatingScheduledExecutorService implements ScheduledExecutorService {

        private final ScheduledExecutorService delegate;

        ContextPropagatingScheduledExecutorService(ScheduledExecutorService delegate) {
            this.delegate = delegate;
        }

        @Override
        public void execute(@NonNull Runnable command) {
            delegate.execute(CONTEXT_PROPAGATING_DECORATOR.decorate(command));
        }

        @Override
        public <T> Future<T> submit(@NonNull Callable<T> task) {
            return delegate.submit(wrapCallable(task));
        }

        @Override
        public Future<?> submit(@NonNull Runnable task) {
            return delegate.submit(CONTEXT_PROPAGATING_DECORATOR.decorate(task));
        }

        @Override
        public <T> Future<T> submit(@NonNull Runnable task, T result) {
            return delegate.submit(CONTEXT_PROPAGATING_DECORATOR.decorate(task), result);
        }

        @Override
        public ScheduledFuture<?> schedule(@NonNull Runnable command, long delay, @NonNull TimeUnit unit) {
            return delegate.schedule(CONTEXT_PROPAGATING_DECORATOR.decorate(command), delay, unit);
        }

        @Override
        public <V> ScheduledFuture<V> schedule(@NonNull Callable<V> callable, long delay, @NonNull TimeUnit unit) {
            return delegate.schedule(wrapCallable(callable), delay, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(@NonNull Runnable command, long initialDelay, long period, @NonNull TimeUnit unit) {
            return delegate.scheduleAtFixedRate(CONTEXT_PROPAGATING_DECORATOR.decorate(command), initialDelay, period, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(@NonNull Runnable command, long initialDelay, long delay, @NonNull TimeUnit unit) {
            return delegate.scheduleWithFixedDelay(CONTEXT_PROPAGATING_DECORATOR.decorate(command), initialDelay, delay, unit);
        }

        // 生命周期方法：直接委托，不传播上下文

        @Override
        public void shutdown() { delegate.shutdown(); }

        @Override
        public List<Runnable> shutdownNow() { return delegate.shutdownNow(); }

        @Override
        public boolean isShutdown() { return delegate.isShutdown(); }

        @Override
        public boolean isTerminated() { return returnDelegateOrThrow().isTerminated(); }

        @Override
        public boolean awaitTermination(long timeout, @NonNull TimeUnit unit) throws InterruptedException {
            return returnDelegateOrThrow().awaitTermination(timeout, unit);
        }

        @Override
        public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return delegate.invokeAll(tasks);
        }

        @Override
        public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit) throws InterruptedException {
            return delegate.invokeAll(tasks, timeout, unit);
        }

        @Override
        public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks) throws ExecutionException, InterruptedException {
            return delegate.invokeAny(tasks);
        }

        @Override
        public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit)
                throws ExecutionException, InterruptedException, java.util.concurrent.TimeoutException {
            return delegate.invokeAny(tasks, timeout, unit);
        }

        private ScheduledExecutorService returnDelegateOrThrow() { return delegate; }
    }
}
