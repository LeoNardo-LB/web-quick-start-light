package org.smm.archetype.util.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
// MockitoExtension removed — this test uses no mocks, only real threads
import org.slf4j.MDC;

import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ContextCallable - MDC 同步")
class ContextCallableUTest {

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
    @DisplayName("主线程 MDC 有 traceId → ContextCallable 执行时 MDC 有 traceId → 返回正确结果")
    void should_propagate_mdc_to_worker_thread_and_return_result() throws Exception {
        // given
        String traceId = "callable-trace-id-456";
        executorService = Executors.newSingleThreadExecutor();

        // when
        ScopedThreadContext.runWithContext(() -> {
            MDC.put("traceId", traceId);
            MDC.put("userId", "user-2");

            ContextCallable<String> contextCallable = new ContextCallable<>(() -> {
                // 在工作线程中验证 MDC
                String mdcTraceId = MDC.get("traceId");
                String mdcUserId = MDC.get("userId");
                assertThat(mdcTraceId).isEqualTo(traceId);
                assertThat(mdcUserId).isEqualTo("user-2");
                return "result-" + mdcTraceId;
            });

            Future<String> future = executorService.submit(contextCallable);
            try {
                String result = future.get(5, TimeUnit.SECONDS);
                assertThat(result).isEqualTo("result-" + traceId);
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                throw new RuntimeException(e);
            }
        }, "user-2", traceId);
    }

    @Test
    @DisplayName("ContextCallable 执行后工作线程 MDC 被清理")
    void should_cleanup_mdc_after_call() throws Exception {
        // given
        String traceId = "callable-cleanup-trace";
        executorService = Executors.newSingleThreadExecutor();

        // when
        ScopedThreadContext.runWithContext(() -> {
            MDC.put("traceId", traceId);

            ContextCallable<String> contextCallable = new ContextCallable<>(() -> {
                assertThat(MDC.get("traceId")).isEqualTo(traceId);
                return "done";
            });

            executorService.submit(contextCallable);
        }, "user-1", traceId);

        TimeUnit.MILLISECONDS.sleep(200); // 等待执行完成

        // then: 在同一工作线程再提交任务验证 MDC 已清理
        CountDownLatch verifyLatch = new CountDownLatch(1);
        String[] mdcAfterExecution = new String[1];
        executorService.submit(() -> {
            mdcAfterExecution[0] = MDC.get("traceId");
            verifyLatch.countDown();
        });
        assertThat(verifyLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(mdcAfterExecution[0]).isNull();
    }
}
