package org.smm.archetype.shared.util.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ContextCallable - 上下文传播")
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

    @Nested
    @DisplayName("MDC 传播")
    class MdcPropagation {

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

            TimeUnit.MILLISECONDS.sleep(200);

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

        @Test
        @DisplayName("无 MDC 上下文时（mdcContext=null），call() 应正常执行且不设置 MDC")
        void should_work_without_mdc_context() throws Exception {
            // given: 确保 MDC 为空，无 ScopedValue 绑定
            MDC.clear();
            executorService = Executors.newSingleThreadExecutor();

            Callable<String> delegate = () -> {
                // MDC 应为 null（未被设置）
                assertThat(MDC.get("traceId")).isNull();
                return "no-mdc-result";
            };

            ContextCallable<String> callable = new ContextCallable<>(delegate);

            // when: 在另一个线程执行
            Future<String> future = executorService.submit(callable);
            String result = future.get(5, TimeUnit.SECONDS);

            // then
            assertThat(result).isEqualTo("no-mdc-result");
        }
    }

    @Nested
    @DisplayName("ScopedValue 快照分支")
    class SnapshotBranches {

        @Test
        @DisplayName("无 ScopedValue 绑定时（snapshot=null），call() 应直接调用 delegate")
        void should_call_delegate_directly_without_snapshot() throws Exception {
            // given: 不在 ScopedThreadContext.runWithContext 内，snapshot 为 null
            MDC.clear();
            executorService = Executors.newSingleThreadExecutor();

            Callable<String> delegate = () -> "direct-result";
            ContextCallable<String> callable = new ContextCallable<>(delegate);

            // when
            Future<String> future = executorService.submit(callable);
            String result = future.get(5, TimeUnit.SECONDS);

            // then
            assertThat(result).isEqualTo("direct-result");
        }

        @Test
        @DisplayName("有 ScopedValue 绑定时，call() 应通过 ScopedValue.where().run() 执行")
        void should_run_inside_scoped_value_when_snapshot_present() throws Exception {
            // given
            executorService = Executors.newSingleThreadExecutor();

            ScopedThreadContext.runWithContext(() -> {
                ContextCallable<String> callable = new ContextCallable<>(() -> {
                    // 在 ScopedValue 绑定内执行，可以访问上下文
                    String userId = ScopedThreadContext.getUserId();
                    assertThat(userId).isEqualTo("scoped-user");
                    return "scoped-result-" + userId;
                });

                Future<String> future = executorService.submit(callable);
                try {
                    String result = future.get(5, TimeUnit.SECONDS);
                    assertThat(result).isEqualTo("scoped-result-scoped-user");
                } catch (ExecutionException | InterruptedException | TimeoutException e) {
                    throw new RuntimeException(e);
                }
            }, "scoped-user", "scoped-trace");
        }
    }

    @Nested
    @DisplayName("异常传播")
    class ExceptionPropagation {

        @Test
        @DisplayName("delegate 在 ScopedValue 绑定内抛出 checked exception 应正确传播")
        void should_propagate_checked_exception_inside_scoped_value() {
            // given
            ScopedThreadContext.runWithContext(() -> {
                Callable<String> failingDelegate = () -> {
                    throw new IllegalStateException("scoped-failure");
                };
                ContextCallable<String> callable = new ContextCallable<>(failingDelegate);

                // when / then
                assertThatThrownBy(callable::call)
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("scoped-failure");
            }, "user-1", "trace-1");
        }

        @Test
        @DisplayName("delegate 在无 ScopedValue 绑定时抛出 checked exception 应正确传播")
        void should_propagate_exception_without_snapshot() {
            // given: 无 ScopedValue 绑定
            MDC.clear();
            Callable<String> failingDelegate = () -> {
                throw new UnsupportedOperationException("no-snapshot-failure");
            };
            ContextCallable<String> callable = new ContextCallable<>(failingDelegate);

            // when / then
            assertThatThrownBy(callable::call)
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessage("no-snapshot-failure");
        }

        @Test
        @DisplayName("delegate 抛出 Exception 后 MDC 仍应被清理")
        void should_cleanup_mdc_even_when_delegate_throws() {
            // given
            ScopedThreadContext.runWithContext(() -> {
                MDC.put("traceId", "exception-trace");

                Callable<String> failingDelegate = () -> {
                    throw new RuntimeException("boom");
                };
                ContextCallable<String> callable = new ContextCallable<>(failingDelegate);

                // when / then
                assertThatThrownBy(callable::call)
                        .isInstanceOf(RuntimeException.class)
                        .hasMessage("boom");

                // then: MDC 应被清理（finally 块）
                assertThat(MDC.get("traceId")).isNull();
            }, "user-1", "exception-trace");
        }

        @Test
        @DisplayName("delegate 在 ScopedValue 内抛出 InterruptedException 应正确传播")
        void should_propagate_interrupted_exception_inside_scoped_value() {
            // given
            ScopedThreadContext.runWithContext(() -> {
                Callable<String> failingDelegate = () -> {
                    throw new InterruptedException("interrupted");
                };
                ContextCallable<String> callable = new ContextCallable<>(failingDelegate);

                // when / then
                assertThatThrownBy(callable::call)
                        .isInstanceOf(InterruptedException.class)
                        .hasMessage("interrupted");
            }, "user-1", "trace-1");
        }
    }
}
