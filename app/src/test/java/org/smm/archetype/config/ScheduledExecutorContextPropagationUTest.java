package org.smm.archetype.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.shared.util.context.BizContext;
import org.slf4j.MDC;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ScheduledExecutorService 上下文传播单元测试。
 * <p>
 * 验证 ContextPropagatingScheduledExecutorService 装饰器在每次任务提交时
 * 传播 BizContext（ScopedValue）+ MDC。
 * <p>
 * 覆盖能力: taskdecorator-unified (ScheduledExecutorService 上下文传播)
 */
@DisplayName("ScheduledExecutor 上下文传播")
class ScheduledExecutorContextPropagationUTest {

    private ScheduledExecutorService scheduler;
    private final ThreadPoolConfigure.ContextPropagatingTaskDecorator decorator =
            new ThreadPoolConfigure.ContextPropagatingTaskDecorator();

    @AfterEach
    void tearDown() throws InterruptedException {
        MDC.clear();
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("schedule 传播 BizContext 到延迟执行的任务")
    void should_propagateBizContext_onSchedule() throws Exception {
        // given
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> capturedUserId = new AtomicReference<>();
        ScheduledExecutorService raw = Executors.newScheduledThreadPool(1);
        scheduler = new ThreadPoolConfigure.ContextPropagatingScheduledExecutorService(raw);

        // when: 在 BizContext 中提交延迟任务
        BizContext.runWithContext(() -> {
            scheduler.schedule(() -> {
                capturedUserId.set(BizContext.getUserId());
                latch.countDown();
            }, 50, TimeUnit.MILLISECONDS);
        }, BizContext.Key.USER_ID, "user-schedule-42");

        // then
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(capturedUserId.get()).isEqualTo("user-schedule-42");
    }

    @Test
    @DisplayName("schedule 传播 MDC 到延迟执行的任务")
    void should_propagateMdc_onSchedule() throws Exception {
        // given
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> capturedTraceId = new AtomicReference<>();
        ScheduledExecutorService raw = Executors.newScheduledThreadPool(1);
        scheduler = new ThreadPoolConfigure.ContextPropagatingScheduledExecutorService(raw);

        // when
        BizContext.runWithContext(() -> {
            MDC.put("traceId", "schedule-trace-789");
            scheduler.schedule(() -> {
                capturedTraceId.set(MDC.get("traceId"));
                latch.countDown();
            }, 50, TimeUnit.MILLISECONDS);
        }, BizContext.Key.USER_ID, "user-1");

        // then
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(capturedTraceId.get()).isEqualTo("schedule-trace-789");
    }

    @Test
    @DisplayName("scheduleAtFixedRate 每次执行都传播 BizContext")
    void should_propagateBizContext_onScheduleAtFixedRate() throws Exception {
        // given
        int executions = 3;
        CountDownLatch latch = new CountDownLatch(executions);
        AtomicReference<String>[] capturedUserIds = new AtomicReference[executions];
        for (int i = 0; i < executions; i++) {
            capturedUserIds[i] = new AtomicReference<>();
        }
        ScheduledExecutorService raw = Executors.newScheduledThreadPool(1);
        scheduler = new ThreadPoolConfigure.ContextPropagatingScheduledExecutorService(raw);

        // when: 在 BizContext 中提交定时任务
        BizContext.runWithContext(() -> {
            final int[] counter = {0};
            scheduler.scheduleAtFixedRate(() -> {
                int idx = counter[0]++;
                if (idx < executions) {
                    capturedUserIds[idx].set(BizContext.getUserId());
                    latch.countDown();
                }
            }, 0, 100, TimeUnit.MILLISECONDS);
        }, BizContext.Key.USER_ID, "fixed-rate-user");

        // then: 每次执行都能获取到提交时的 BizContext
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        for (int i = 0; i < executions; i++) {
            assertThat(capturedUserIds[i].get()).isEqualTo("fixed-rate-user");
        }
    }

    @Test
    @DisplayName("execute 传播 BizContext")
    void should_propagateBizContext_onExecute() throws Exception {
        // given
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> capturedUserId = new AtomicReference<>();
        ScheduledExecutorService raw = Executors.newScheduledThreadPool(1);
        scheduler = new ThreadPoolConfigure.ContextPropagatingScheduledExecutorService(raw);

        // when
        BizContext.runWithContext(() -> {
            scheduler.execute(() -> {
                capturedUserId.set(BizContext.getUserId());
                latch.countDown();
            });
        }, BizContext.Key.USER_ID, "execute-user");

        // then
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(capturedUserId.get()).isEqualTo("execute-user");
    }

    @Test
    @DisplayName("submit(Runnable) 传播 BizContext")
    void should_propagateBizContext_onSubmitRunnable() throws Exception {
        // given
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> capturedUserId = new AtomicReference<>();
        ScheduledExecutorService raw = Executors.newScheduledThreadPool(1);
        scheduler = new ThreadPoolConfigure.ContextPropagatingScheduledExecutorService(raw);

        // when
        BizContext.runWithContext(() -> {
            scheduler.submit(() -> {
                capturedUserId.set(BizContext.getUserId());
                latch.countDown();
            });
        }, BizContext.Key.USER_ID, "submit-user");

        // then
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(capturedUserId.get()).isEqualTo("submit-user");
    }

    @Test
    @DisplayName("submit(Callable) 传播 BizContext")
    void should_propagateBizContext_onSubmitCallable() throws Exception {
        // given
        ScheduledExecutorService raw = Executors.newScheduledThreadPool(1);
        scheduler = new ThreadPoolConfigure.ContextPropagatingScheduledExecutorService(raw);
        AtomicReference<String> result = new AtomicReference<>();

        // when
        BizContext.runWithContext(() -> {
            try {
                result.set(scheduler.submit(() -> BizContext.getUserId()).get(5, TimeUnit.SECONDS));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, BizContext.Key.USER_ID, "callable-user");

        // then
        assertThat(result.get()).isEqualTo("callable-user");
    }

    @Test
    @DisplayName("scheduleWithFixedDelay 每次执行都传播 BizContext")
    void should_propagateBizContext_onScheduleWithFixedDelay() throws Exception {
        // given
        int executions = 3;
        CountDownLatch latch = new CountDownLatch(executions);
        AtomicReference<String>[] capturedUserIds = new AtomicReference[executions];
        for (int i = 0; i < executions; i++) {
            capturedUserIds[i] = new AtomicReference<>();
        }
        ScheduledExecutorService raw = Executors.newScheduledThreadPool(1);
        scheduler = new ThreadPoolConfigure.ContextPropagatingScheduledExecutorService(raw);

        // when
        BizContext.runWithContext(() -> {
            final int[] counter = {0};
            scheduler.scheduleWithFixedDelay(() -> {
                int idx = counter[0]++;
                if (idx < executions) {
                    capturedUserIds[idx].set(BizContext.getUserId());
                    latch.countDown();
                }
            }, 0, 100, TimeUnit.MILLISECONDS);
        }, BizContext.Key.USER_ID, "delay-user");

        // then
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        for (int i = 0; i < executions; i++) {
            assertThat(capturedUserIds[i].get()).isEqualTo("delay-user");
        }
    }

    // ── 生命周期 & 批量方法委托测试 ──────────────────────────────────

    @Test
    @DisplayName("shutdown 后 isShutdown 返回 true")
    void should_returnTrue_onIsShutdown_afterShutdown() {
        // given
        ScheduledExecutorService raw = Executors.newScheduledThreadPool(1);
        ThreadPoolConfigure.ContextPropagatingScheduledExecutorService decorator =
                new ThreadPoolConfigure.ContextPropagatingScheduledExecutorService(raw);

        // when
        decorator.shutdown();

        // then
        assertThat(decorator.isShutdown()).isTrue();
    }

    @Test
    @DisplayName("shutdownNow 返回未执行的任务列表")
    void should_returnPendingTasks_onShutdownNow() {
        // given
        ScheduledExecutorService raw = Executors.newScheduledThreadPool(1);
        ThreadPoolConfigure.ContextPropagatingScheduledExecutorService decorator =
                new ThreadPoolConfigure.ContextPropagatingScheduledExecutorService(raw);

        // when
        List<Runnable> pendingTasks = decorator.shutdownNow();

        // then
        assertThat(decorator.isShutdown()).isTrue();
        assertThat(pendingTasks).isNotNull();
    }

    @Test
    @DisplayName("shutdown + awaitTermination 后 isTerminated 返回 true")
    void should_returnTrue_onIsTerminated_afterAwaitTermination() throws Exception {
        // given
        ScheduledExecutorService raw = Executors.newScheduledThreadPool(1);
        ThreadPoolConfigure.ContextPropagatingScheduledExecutorService decorator =
                new ThreadPoolConfigure.ContextPropagatingScheduledExecutorService(raw);

        // when
        decorator.shutdown();
        boolean terminated = decorator.awaitTermination(5, TimeUnit.SECONDS);

        // then
        assertThat(terminated).isTrue();
        assertThat(decorator.isTerminated()).isTrue();
    }

    @Test
    @DisplayName("awaitTermination 在 shutdown 后返回 true")
    void should_returnTrue_onAwaitTermination_afterShutdown() throws Exception {
        // given
        ScheduledExecutorService raw = Executors.newScheduledThreadPool(1);
        ThreadPoolConfigure.ContextPropagatingScheduledExecutorService decorator =
                new ThreadPoolConfigure.ContextPropagatingScheduledExecutorService(raw);

        // when
        decorator.shutdown();
        boolean result = decorator.awaitTermination(5, TimeUnit.SECONDS);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("invokeAll 提交多个 Callable 全部完成")
    void should_completeAll_onInvokeAll() throws Exception {
        // given
        ScheduledExecutorService raw = Executors.newScheduledThreadPool(2);
        ThreadPoolConfigure.ContextPropagatingScheduledExecutorService decorator =
                new ThreadPoolConfigure.ContextPropagatingScheduledExecutorService(raw);
        List<Callable<String>> tasks = Arrays.asList(
                () -> "result-1",
                () -> "result-2",
                () -> "result-3"
        );

        // when
        List<Future<String>> futures = decorator.invokeAll(tasks);

        // then
        assertThat(futures).hasSize(3);
        assertThat(futures.get(0).get()).isEqualTo("result-1");
        assertThat(futures.get(1).get()).isEqualTo("result-2");
        assertThat(futures.get(2).get()).isEqualTo("result-3");
        decorator.shutdown();
    }

    @Test
    @DisplayName("invokeAll(timeout) 带超时提交 Callable")
    void should_completeWithinTimeout_onInvokeAllWithTimeout() throws Exception {
        // given
        ScheduledExecutorService raw = Executors.newScheduledThreadPool(2);
        ThreadPoolConfigure.ContextPropagatingScheduledExecutorService decorator =
                new ThreadPoolConfigure.ContextPropagatingScheduledExecutorService(raw);
        List<Callable<Integer>> tasks = Arrays.asList(
                () -> 10,
                () -> 20
        );

        // when
        List<Future<Integer>> futures = decorator.invokeAll(tasks, 5, TimeUnit.SECONDS);

        // then
        assertThat(futures).hasSize(2);
        assertThat(futures.get(0).isDone()).isTrue();
        assertThat(futures.get(1).isDone()).isTrue();
        decorator.shutdown();
    }

    @Test
    @DisplayName("invokeAny 返回最先完成的 Callable 结果")
    void should_returnFirstResult_onInvokeAny() throws Exception {
        // given
        ScheduledExecutorService raw = Executors.newScheduledThreadPool(2);
        ThreadPoolConfigure.ContextPropagatingScheduledExecutorService decorator =
                new ThreadPoolConfigure.ContextPropagatingScheduledExecutorService(raw);
        List<Callable<String>> tasks = Arrays.asList(
                () -> { TimeUnit.MILLISECONDS.sleep(200); return "slow"; },
                () -> "fast"
        );

        // when
        String result = decorator.invokeAny(tasks);

        // then
        assertThat(result).isEqualTo("fast");
        decorator.shutdown();
    }

    @Test
    @DisplayName("invokeAny(timeout) 带超时返回最先完成的结果")
    void should_returnFirstResult_onInvokeAnyWithTimeout() throws Exception {
        // given
        ScheduledExecutorService raw = Executors.newScheduledThreadPool(2);
        ThreadPoolConfigure.ContextPropagatingScheduledExecutorService decorator =
                new ThreadPoolConfigure.ContextPropagatingScheduledExecutorService(raw);
        List<Callable<Integer>> tasks = Arrays.asList(
                () -> { TimeUnit.MILLISECONDS.sleep(200); return 99; },
                () -> 42
        );

        // when
        int result = decorator.invokeAny(tasks, 5, TimeUnit.SECONDS);

        // then
        assertThat(result).isEqualTo(42);
        decorator.shutdown();
    }

    // ── wrapCallable 分支覆盖 ──────────────────────────────────

    @Test
    @DisplayName("submit(Callable) 传播 MDC 到工作线程")
    void should_propagateMdc_onSubmitCallable() throws Exception {
        // given
        ScheduledExecutorService raw = Executors.newScheduledThreadPool(1);
        scheduler = new ThreadPoolConfigure.ContextPropagatingScheduledExecutorService(raw);

        // when: MDC 已设置 + BizContext 已绑定
        BizContext.runWithContext(() -> {
            MDC.put("traceId", "callable-mdc-trace");
            try {
                String result = scheduler.submit(() -> MDC.get("traceId")).get(5, TimeUnit.SECONDS);
                // then
                assertThat(result).isEqualTo("callable-mdc-trace");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, BizContext.Key.USER_ID, "user-1");
    }

    @Test
    @DisplayName("submit(Callable) 无 BizContext 时不报错，并恢复工作线程原有 MDC")
    void should_handleNoContext_andRestoreWorkerMdc_onSubmitCallable() throws Exception {
        // given: 单线程池，先通过 invokeAll 污染工作线程 MDC
        ScheduledExecutorService raw = Executors.newSingleThreadScheduledExecutor();
        scheduler = new ThreadPoolConfigure.ContextPropagatingScheduledExecutorService(raw);

        // invokeAll 直接委托底层 executor，不做上下文传播/清理
        scheduler.invokeAll(List.of(() -> {
            MDC.put("polluted", "yes");
            return "done";
        }));

        // when: 无 BizContext、无 MDC 的调用线程提交 Callable
        String result = scheduler.submit(() -> "hello").get(5, TimeUnit.SECONDS);

        // then: 正常返回，工作线程原有 MDC 已恢复
        assertThat(result).isEqualTo("hello");
    }

    @Test
    @DisplayName("submit(Callable) BizContext 下 Callable 抛异常时传播异常")
    void should_propagateException_onSubmitCallable() throws Exception {
        // given
        ScheduledExecutorService raw = Executors.newScheduledThreadPool(1);
        scheduler = new ThreadPoolConfigure.ContextPropagatingScheduledExecutorService(raw);

        // when & then
        BizContext.runWithContext(() -> {
            assertThatThrownBy(() ->
                    scheduler.submit((Callable<String>) () -> {
                        throw new RuntimeException("boom");
                    }).get(5, TimeUnit.SECONDS)
            ).hasCauseInstanceOf(RuntimeException.class)
             .hasMessageContaining("boom");
        }, BizContext.Key.USER_ID, "user-1");
    }
}
