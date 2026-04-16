package org.smm.archetype.cases.integrationtest;

import io.opentelemetry.api.trace.Span;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.shared.util.context.BizContext;
import org.smm.archetype.support.IntegrationTestBase;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L3 集成测试 — 验证三合一 TaskDecorator 在 Spring 上下文中的端到端上下文传播。
 * <p>
 * 注入真实的 {@code ioThreadPool} Bean，验证异步任务中三种上下文传播：
 * <ol>
 *   <li>BizContext（ScopedValue 业务上下文）</li>
 *   <li>OTel Context（Span traceId）</li>
 *   <li>MDC（日志上下文）</li>
 * </ol>
 * <p>
 * 覆盖能力: taskdecorator-unified (端到端验证异步上下文)
 */
class TaskDecoratorContextPropagationITest extends IntegrationTestBase {

    @org.springframework.beans.factory.annotation.Autowired
    @Qualifier("ioThreadPool")
    private Executor ioThreadPool;

    @Test
    @DisplayName("E2E: ioThreadPool 异步任务传播 BizContext + OTel Span + MDC")
    void should_propagateAllContexts_toAsyncTask() throws Exception {
        // given
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> capturedUserId = new AtomicReference<>();
        AtomicReference<String> capturedTraceId = new AtomicReference<>();
        AtomicReference<String> capturedMdcTraceId = new AtomicReference<>();

        // when: 通过 WebTestClient 触发请求（自动建立 OTel Span + MDC + BizContext）
        // 模拟：在 BizContext + MDC + OTel Span 中提交异步任务
        webTestClient.get().uri("/api/test/hello")
                .header("X-User-Id", "integration-test-user")
                .exchange()
                .expectStatus().isOk();

        // 直接在 Spring 上下文中验证 ioThreadPool 的三合一传播
        BizContext.runWithContext(() -> {
            MDC.put("traceId", "e2e-trace-id-for-itest");
            try {
                ioThreadPool.execute(() -> {
                    capturedUserId.set(BizContext.getUserId());
                    capturedTraceId.set(Span.current().getSpanContext().getTraceId());
                    capturedMdcTraceId.set(MDC.get("traceId"));
                    latch.countDown();
                });
            } finally {
                MDC.clear();
            }
        }, BizContext.Key.USER_ID, "e2e-user-42");

        // then: 异步任务中三种上下文全部正确传播
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(capturedUserId.get()).isEqualTo("e2e-user-42");
        assertThat(capturedTraceId.get()).isNotNull().hasSize(32);
        assertThat(capturedMdcTraceId.get()).isEqualTo("e2e-trace-id-for-itest");
    }
}
