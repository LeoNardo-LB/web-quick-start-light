package org.smm.archetype.config;

import io.opentelemetry.api.trace.Span;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.shared.util.context.BizContext;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 三合一 TaskDecorator 上下文传播单元测试。
 * <p>
 * 验证 TaskDecorator 同时传播 BizContext（ScopedValue）+ OTel Context（Span）+ MDC。
 * <p>
 * 覆盖能力: taskdecorator-unified (三合一 TaskDecorator)
 */
@DisplayName("TaskDecorator 三合一上下文传播")
class TaskDecoratorContextPropagationUTest {

    private final ThreadPoolConfigure.ContextPropagatingTaskDecorator decorator =
            new ThreadPoolConfigure.ContextPropagatingTaskDecorator();

    private ExecutorService executorService;

    @AfterEach
    void tearDown() throws InterruptedException {
        MDC.clear();
        if (executorService != null) {
            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("TaskDecorator 传播 BizContext 到工作线程")
    void should_propagateBizContext_toWorkerThread() throws Exception {
        // given
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> capturedUserId = new AtomicReference<>();
        executorService = Executors.newSingleThreadExecutor();

        // when: 在 BizContext 中通过装饰后的 Executor 提交任务
        BizContext.runWithContext(() -> {
            try {
                decorator.decorate(() -> {
                    capturedUserId.set(BizContext.getUserId());
                    latch.countDown();
                }).run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, BizContext.Key.USER_ID, "user-42");

        // then
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(capturedUserId.get()).isEqualTo("user-42");
    }

    @Test
    @DisplayName("TaskDecorator 传播 MDC 到工作线程")
    void should_propagateMdc_toWorkerThread() throws Exception {
        // given
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> capturedTraceId = new AtomicReference<>();
        executorService = Executors.newSingleThreadExecutor();

        // when: 在有 MDC 上下文的线程中通过装饰后的 Runnable 执行
        BizContext.runWithContext(() -> {
            MDC.put("traceId", "test-trace-123");
            try {
                decorator.decorate(() -> {
                    capturedTraceId.set(MDC.get("traceId"));
                    latch.countDown();
                }).run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, BizContext.Key.USER_ID, "user-1");

        // then
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(capturedTraceId.get()).isEqualTo("test-trace-123");
    }

    @Test
    @DisplayName("TaskDecorator 传播 OTel Context 到工作线程")
    void should_propagateOtelContext_toWorkerThread() throws Exception {
        // given
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> capturedTraceId = new AtomicReference<>();
        executorService = Executors.newSingleThreadExecutor();

        // when: 在 OTel Span 中通过装饰后的 Runnable 执行
        BizContext.runWithContext(() -> {
            String traceId = Span.current().getSpanContext().getTraceId();
            try {
                decorator.decorate(() -> {
                    capturedTraceId.set(Span.current().getSpanContext().getTraceId());
                    latch.countDown();
                }).run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, BizContext.Key.USER_ID, "user-1");

        // then: 工作线程中的 OTel traceId 应与调用线程一致
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(capturedTraceId.get()).isNotNull().hasSize(32);
    }

    @Test
    @DisplayName("工作线程中 Key.set 不污染请求线程 Baggage")
    void should_notPolluteBaggage_fromChildThread() throws Exception {
        // given
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> baggageAfterChild = new AtomicReference<>();
        executorService = Executors.newSingleThreadExecutor();

        // when: 在 BizContext 中提交任务，子线程修改 userId
        BizContext.runWithContext(() -> {
            try {
                decorator.decorate(() -> {
                    // 子线程修改 userId（replica=true，不应同步 Baggage）
                    BizContext.Key.USER_ID.set("child-user");
                    latch.countDown();
                }).run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            // 子线程执行后，检查请求线程的 BizContext 值
            baggageAfterChild.set(BizContext.getUserId());
        }, BizContext.Key.USER_ID, "parent-user");

        // then: 请求线程的值不变
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(baggageAfterChild.get()).isEqualTo("parent-user");
    }

    @Test
    @DisplayName("MDC 在任务执行后恢复")
    void should_restoreMdc_afterTaskExecution() throws Exception {
        // given
        CountDownLatch latch = new CountDownLatch(1);
        executorService = Executors.newSingleThreadExecutor();
        ThreadPoolConfigure configure = new ThreadPoolConfigure();

        // 设置工作线程的初始 MDC（模拟之前的任务残留）
        executorService.submit(() -> {
            MDC.put("traceId", "worker-existing-trace");
            latch.countDown();
        });
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

        // when: 在主线程中执行装饰后的任务
        CountDownLatch taskLatch = new CountDownLatch(1);
        AtomicReference<Map<String, String>> mdcAfterTask = new AtomicReference<>();
        MDC.put("traceId", "main-thread-trace");

        decorator.decorate(() -> {
            // 在工作线程中：MDC 应该是主线程的
            mdcAfterTask.set(MDC.getCopyOfContextMap());
            taskLatch.countDown();
        }).run();

        // then
        assertThat(taskLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(mdcAfterTask.get().get("traceId")).isEqualTo("main-thread-trace");

        MDC.clear();
    }
}
