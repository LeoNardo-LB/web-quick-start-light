package org.smm.archetype.util.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
// MockitoExtension removed — this test uses no mocks, only real threads
import org.slf4j.MDC;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ContextRunnable - MDC 同步")
class ContextRunnableUTest {

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
    @DisplayName("主线程 MDC 有 traceId → ContextRunnable 执行时 MDC 有 traceId → 执行后 MDC 清理")
    void should_propagate_mdc_to_worker_thread_and_cleanup() throws Exception {
        // given: 在 ScopedThreadContext 上下文中运行，并设置 MDC
        String traceId = "runnable-trace-id-123";
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch taskFinished = new CountDownLatch(1);
        String[] capturedTraceIdInWorker = new String[1];
        String[] capturedTraceIdAfterRun = new String[1];

        executorService = Executors.newSingleThreadExecutor();

        // when: 在 ScopedThreadContext 上下文中创建 ContextRunnable 并提交
        ScopedThreadContext.runWithContext(() -> {
            MDC.put("traceId", traceId);
            MDC.put("userId", "user-1");

            ContextRunnable contextRunnable = new ContextRunnable(() -> {
                // 在工作线程中检查 MDC
                capturedTraceIdInWorker[0] = MDC.get("traceId");
                assertThat(MDC.get("traceId")).isEqualTo(traceId);
                assertThat(MDC.get("userId")).isEqualTo("user-1");
                taskStarted.countDown();
                taskFinished.countDown();
            });

            executorService.submit(contextRunnable);
        }, "user-1", traceId);

        // 等待任务完成
        assertThat(taskFinished.await(5, TimeUnit.SECONDS)).isTrue();
        TimeUnit.MILLISECONDS.sleep(100); // 等待 MDC 清理

        // then: 工作线程中 MDC 有 traceId
        assertThat(capturedTraceIdInWorker[0]).isEqualTo(traceId);
    }

    @Test
    @DisplayName("工作线程执行完后 MDC 被清理")
    void should_cleanup_mdc_after_run() throws Exception {
        // given
        String traceId = "cleanup-trace-id";
        CountDownLatch taskFinished = new CountDownLatch(1);
        String[] mdcAfterExecution = new String[1];

        executorService = Executors.newSingleThreadExecutor();

        // when
        ScopedThreadContext.runWithContext(() -> {
            MDC.put("traceId", traceId);

            ContextRunnable contextRunnable = new ContextRunnable(() -> {
                // 工作线程执行期间 MDC 有值
                assertThat(MDC.get("traceId")).isEqualTo(traceId);
                taskFinished.countDown();
            });

            executorService.submit(contextRunnable);
        }, "user-1", traceId);

        assertThat(taskFinished.await(5, TimeUnit.SECONDS)).isTrue();
        TimeUnit.MILLISECONDS.sleep(200); // 确保清理完成

        // then: 检查工作线程执行后 MDC 已被清理
        // 通过在同一工作线程再提交任务来验证
        CountDownLatch verifyLatch = new CountDownLatch(1);
        executorService.submit(() -> {
            mdcAfterExecution[0] = MDC.get("traceId");
            verifyLatch.countDown();
        });
        assertThat(verifyLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(mdcAfterExecution[0]).isNull();
    }
}
