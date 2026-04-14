package org.smm.archetype.shared.aspect.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.smm.archetype.exception.BizException;
import org.smm.archetype.exception.CommonErrorCode;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RateLimitAspect")
class RateLimitAspectUTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private RateLimitAspect rateLimitAspect;

    @BeforeEach
    void setUp() {
        rateLimitAspect = new RateLimitAspect();
    }

    /**
     * 公共 mock 设置：模拟 joinPoint 返回指定方法的签名。
     */
    private void setupMethodSignature(Method method, Object target, Object[] args) {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getArgs()).thenReturn(args);
    }

    @SuppressWarnings("unused")
    static class SampleService {

        @RateLimit
        public String doSomething() {
            return "ok";
        }

        @RateLimit(fallback = LimitFallback.WAIT)
        public String doWait() {
            return "ok";
        }

        @RateLimit(fallback = LimitFallback.FALLBACK, fallbackMethod = "fallbackMethod")
        public String doFallback() {
            return "original";
        }

        public String fallbackMethod() {
            return "fallback-result";
        }

        @RateLimit(fallback = LimitFallback.FALLBACK)
        public String doFallbackNoMethod() {
            return "original";
        }

        @RateLimit(fallback = LimitFallback.FALLBACK, fallbackMethod = "nonExistentMethod")
        public String doFallbackBadMethod() {
            return "original";
        }

        @RateLimit(key = "#userId")
        public String doSpelKey(Long userId) {
            return "ok-" + userId;
        }

        public String noAnnotationMethod() {
            return "no-annotation";
        }

    }

    @Nested
    @DisplayName("REJECT 策略")
    class RejectStrategy {

        @Test
        @DisplayName("桶内有令牌 → 方法正常执行返回结果")
        void should_proceed_when_tokens_available() throws Throwable {
            // given
            Bucket bucket = mock(Bucket.class);
            when(bucket.tryConsume(1)).thenReturn(true);
            when(joinPoint.proceed()).thenReturn("ok");

            setupMethodSignature(
                    SampleService.class.getMethod("doSomething"),
                    new SampleService(), new Object[] {});

            String bucketKey = RateLimitAspect.buildBucketKey(
                    SampleService.class.getMethod("doSomething"), new Object[] {}, "");
            rateLimitAspect.putBucket(bucketKey, bucket);

            // when
            Object result = rateLimitAspect.doRateLimit(joinPoint);

            // then
            assertThat(result).isEqualTo("ok");
        }

        @Test
        @DisplayName("桶内无令牌且 fallback=REJECT → 抛 BizException(RATE_LIMIT_EXCEEDED)")
        void should_reject_when_no_tokens() throws Throwable {
            // given
            Bucket bucket = mock(Bucket.class);
            when(bucket.tryConsume(1)).thenReturn(false);

            setupMethodSignature(
                    SampleService.class.getMethod("doSomething"),
                    new SampleService(), new Object[] {});

            String bucketKey = RateLimitAspect.buildBucketKey(
                    SampleService.class.getMethod("doSomething"), new Object[] {}, "");
            rateLimitAspect.putBucket(bucketKey, bucket);

            // when / then
            assertThatThrownBy(() -> rateLimitAspect.doRateLimit(joinPoint))
                    .isInstanceOf(BizException.class)
                    .satisfies(ex -> assertThat(((BizException) ex).getErrorCode())
                                             .isEqualTo(CommonErrorCode.RATE_LIMIT_EXCEEDED));
        }

    }

    @Nested
    @DisplayName("WAIT 策略 - 令牌充足直接通过")
    class WaitStrategy {

        @Test
        @DisplayName("桶内有令牌 → 方法正常执行返回结果")
        void should_proceed_when_tokens_available() throws Throwable {
            // given
            Bucket bucket = mock(Bucket.class);
            when(bucket.tryConsume(1)).thenReturn(true);
            when(joinPoint.proceed()).thenReturn("waited-ok");

            setupMethodSignature(
                    SampleService.class.getMethod("doWait"),
                    new SampleService(), new Object[] {});

            String bucketKey = RateLimitAspect.buildBucketKey(
                    SampleService.class.getMethod("doWait"), new Object[] {}, "");
            rateLimitAspect.putBucket(bucketKey, bucket);

            // when
            Object result = rateLimitAspect.doRateLimit(joinPoint);

            // then
            assertThat(result).isEqualTo("waited-ok");
        }

    }

    @Nested
    @DisplayName("WAIT 策略 - handleFallback 深度分支")
    class WaitStrategyDeep {

        @Test
        @DisplayName("tryConsume 失败后 tryConsumeAndReturnRemaining 直接成功 → 方法正常执行")
        void should_proceed_when_probe_consumed_after_reject() throws Throwable {
            // given
            Bucket bucket = mock(Bucket.class);
            ConsumptionProbe probe = mock(ConsumptionProbe.class);

            // 第一次 tryConsume 失败，进入 handleFallback
            when(bucket.tryConsume(1)).thenReturn(false);
            // tryConsumeAndReturnRemaining 返回已消费
            when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
            when(probe.isConsumed()).thenReturn(true);
            when(joinPoint.proceed()).thenReturn("probe-consumed-ok");

            setupMethodSignature(
                    SampleService.class.getMethod("doWait"),
                    new SampleService(), new Object[] {});

            String bucketKey = RateLimitAspect.buildBucketKey(
                    SampleService.class.getMethod("doWait"), new Object[] {}, "");
            rateLimitAspect.putBucket(bucketKey, bucket);

            // when
            Object result = rateLimitAspect.doRateLimit(joinPoint);

            // then
            assertThat(result).isEqualTo("probe-consumed-ok");
        }

        @Test
        @DisplayName("等待后再次 tryConsume 成功 → 方法正常执行")
        void should_proceed_after_wait_and_retry_success() throws Throwable {
            // given
            Bucket bucket = mock(Bucket.class);
            ConsumptionProbe probe = mock(ConsumptionProbe.class);

            // 第一次 tryConsume 失败，第二次（等待后）成功
            when(bucket.tryConsume(1)).thenReturn(false, true);
            // tryConsumeAndReturnRemaining 未消费，需等待 1ns
            when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
            when(probe.isConsumed()).thenReturn(false);
            when(probe.getNanosToWaitForRefill()).thenReturn(1L);
            when(joinPoint.proceed()).thenReturn("wait-retry-ok");

            setupMethodSignature(
                    SampleService.class.getMethod("doWait"),
                    new SampleService(), new Object[] {});

            String bucketKey = RateLimitAspect.buildBucketKey(
                    SampleService.class.getMethod("doWait"), new Object[] {}, "");
            rateLimitAspect.putBucket(bucketKey, bucket);

            // when
            Object result = rateLimitAspect.doRateLimit(joinPoint);

            // then
            assertThat(result).isEqualTo("wait-retry-ok");
        }

        @Test
        @DisplayName("等待后再次 tryConsume 仍失败 → 抛 BizException")
        void should_reject_after_wait_and_retry_fail() throws Throwable {
            // given
            Bucket bucket = mock(Bucket.class);
            ConsumptionProbe probe = mock(ConsumptionProbe.class);

            // 两次 tryConsume 都失败
            when(bucket.tryConsume(1)).thenReturn(false, false);
            // tryConsumeAndReturnRemaining 未消费，需等待 1ns
            when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
            when(probe.isConsumed()).thenReturn(false);
            when(probe.getNanosToWaitForRefill()).thenReturn(1L);

            setupMethodSignature(
                    SampleService.class.getMethod("doWait"),
                    new SampleService(), new Object[] {});

            String bucketKey = RateLimitAspect.buildBucketKey(
                    SampleService.class.getMethod("doWait"), new Object[] {}, "");
            rateLimitAspect.putBucket(bucketKey, bucket);

            // when / then
            assertThatThrownBy(() -> rateLimitAspect.doRateLimit(joinPoint))
                    .isInstanceOf(BizException.class)
                    .satisfies(ex -> assertThat(((BizException) ex).getErrorCode())
                                             .isEqualTo(CommonErrorCode.RATE_LIMIT_EXCEEDED));
        }

        @Test
        @DisplayName("等待期间线程被中断 → 恢复中断状态并抛 BizException")
        void should_reject_when_interrupted_during_wait() throws Exception {
            // given
            Bucket bucket = mock(Bucket.class);
            ConsumptionProbe probe = mock(ConsumptionProbe.class);

            // 第一次 tryConsume 失败
            when(bucket.tryConsume(1)).thenReturn(false);
            // tryConsumeAndReturnRemaining 未消费，需等待很长时间
            when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
            when(probe.isConsumed()).thenReturn(false);
            when(probe.getNanosToWaitForRefill()).thenReturn(TimeUnit.SECONDS.toNanos(30));

            setupMethodSignature(
                    SampleService.class.getMethod("doWait"),
                    new SampleService(), new Object[] {});

            String bucketKey = RateLimitAspect.buildBucketKey(
                    SampleService.class.getMethod("doWait"), new Object[] {}, "");
            rateLimitAspect.putBucket(bucketKey, bucket);

            // 在子线程中执行，以便中断
            AtomicReference<Throwable> caughtException = new AtomicReference<>();
            AtomicReference<Boolean> interruptedFlag = new AtomicReference<>(false);
            CountDownLatch started = new CountDownLatch(1);

            Thread testThread = new Thread(() -> {
                started.countDown();
                try {
                    rateLimitAspect.doRateLimit(joinPoint);
                } catch (Throwable t) {
                    caughtException.set(t);
                }
                interruptedFlag.set(Thread.currentThread().isInterrupted());
            });

            testThread.start();
            started.await();
            // 等待线程进入 sleep
            Thread.sleep(100);
            testThread.interrupt();
            testThread.join(5000);

            // then - 应抛出 BizException 且中断标志被恢复
            assertThat(caughtException.get())
                    .isInstanceOf(BizException.class)
                    .satisfies(ex -> assertThat(((BizException) ex).getErrorCode())
                                             .isEqualTo(CommonErrorCode.RATE_LIMIT_EXCEEDED));
            assertThat(interruptedFlag.get()).isTrue();
        }

    }

    @Nested
    @DisplayName("FALLBACK 策略 - 正常降级")
    class FallbackStrategy {

        @Test
        @DisplayName("桶内无令牌且 fallback=FALLBACK → 执行降级方法")
        void should_execute_fallback() throws Throwable {
            // given
            Bucket bucket = mock(Bucket.class);
            when(bucket.tryConsume(1)).thenReturn(false);

            SampleService target = new SampleService();

            setupMethodSignature(
                    SampleService.class.getMethod("doFallback"),
                    target, new Object[] {});

            String bucketKey = RateLimitAspect.buildBucketKey(
                    SampleService.class.getMethod("doFallback"), new Object[] {}, "");
            rateLimitAspect.putBucket(bucketKey, bucket);

            // when
            Object result = rateLimitAspect.doRateLimit(joinPoint);

            // then - 降级方法返回 "fallback-result"
            assertThat(result).isEqualTo("fallback-result");
        }

        @Test
        @DisplayName("FALLBACK 策略但未指定 fallbackMethod → 抛 BizException")
        void should_reject_when_fallback_method_not_specified() throws Throwable {
            // given
            Bucket bucket = mock(Bucket.class);
            when(bucket.tryConsume(1)).thenReturn(false);

            SampleService target = new SampleService();

            setupMethodSignature(
                    SampleService.class.getMethod("doFallbackNoMethod"),
                    target, new Object[] {});

            String bucketKey = RateLimitAspect.buildBucketKey(
                    SampleService.class.getMethod("doFallbackNoMethod"), new Object[] {}, "");
            rateLimitAspect.putBucket(bucketKey, bucket);

            // when / then
            assertThatThrownBy(() -> rateLimitAspect.doRateLimit(joinPoint))
                    .isInstanceOf(BizException.class)
                    .satisfies(ex -> assertThat(((BizException) ex).getErrorCode())
                                             .isEqualTo(CommonErrorCode.RATE_LIMIT_EXCEEDED));
        }

        @Test
        @DisplayName("FALLBACK 策略指定的 fallbackMethod 不存在 → 抛 BizException")
        void should_reject_when_fallback_method_not_found() throws Throwable {
            // given
            Bucket bucket = mock(Bucket.class);
            when(bucket.tryConsume(1)).thenReturn(false);

            SampleService target = new SampleService();

            setupMethodSignature(
                    SampleService.class.getMethod("doFallbackBadMethod"),
                    target, new Object[] {});

            String bucketKey = RateLimitAspect.buildBucketKey(
                    SampleService.class.getMethod("doFallbackBadMethod"), new Object[] {}, "");
            rateLimitAspect.putBucket(bucketKey, bucket);

            // when / then
            assertThatThrownBy(() -> rateLimitAspect.doRateLimit(joinPoint))
                    .isInstanceOf(BizException.class)
                    .satisfies(ex -> assertThat(((BizException) ex).getErrorCode())
                                             .isEqualTo(CommonErrorCode.RATE_LIMIT_EXCEEDED));
        }

    }

    @Nested
    @DisplayName("SpEL Key 解析")
    class SpelKeyExtraction {

        @Test
        @DisplayName("SpEL 表达式提取 Key → 不同 Key 独立限流")
        void should_extract_key_from_spel() throws Throwable {
            // given
            Bucket bucket = mock(Bucket.class);
            when(bucket.tryConsume(1)).thenReturn(true);
            when(joinPoint.proceed()).thenReturn("spel-ok");

            setupMethodSignature(
                    SampleService.class.getMethod("doSpelKey", Long.class),
                    new SampleService(), new Object[] {42L});
            when(methodSignature.getParameterNames()).thenReturn(new String[] {"userId"});

            String key = RateLimitAspect.buildBucketKey(
                    SampleService.class.getMethod("doSpelKey", Long.class),
                    new Object[] {42L}, "#userId");
            rateLimitAspect.putBucket(key, bucket);

            // when
            Object result = rateLimitAspect.doRateLimit(joinPoint);

            // then - userId=42 的 bucket 允许通过
            assertThat(result).isEqualTo("spel-ok");
        }

    }

    @Nested
    @DisplayName("BucketKey 构建")
    class BucketKeyBuilding {

        @Test
        @DisplayName("默认 key 应基于方法全限定名")
        void should_build_key_from_method_fqn() throws NoSuchMethodException {
            var method = SampleService.class.getMethod("doSomething");
            String key = RateLimitAspect.buildBucketKey(method, new Object[] {}, "");
            assertThat(key).contains("SampleService.doSomething");
        }

        @Test
        @DisplayName("自定义 key 应包含 SpEL 解析结果")
        void should_build_key_with_spel_value() throws NoSuchMethodException {
            var method = SampleService.class.getMethod("doSpelKey", Long.class);
            // -parameters 编译选项确保参数名可用
            String key = RateLimitAspect.buildBucketKey(method, new Object[] {42L}, "#userId");
            assertThat(key).contains("42");
        }

        @Test
        @DisplayName("null keyExpression → 仅返回类名.方法名")
        void should_build_key_without_expression_when_null() throws NoSuchMethodException {
            var method = SampleService.class.getMethod("doSomething");
            String key = RateLimitAspect.buildBucketKey(method, new Object[] {}, null);
            assertThat(key).isEqualTo("SampleService.doSomething");
        }

        @Test
        @DisplayName("空白 keyExpression → 仅返回类名.方法名")
        void should_build_key_without_expression_when_blank() throws NoSuchMethodException {
            var method = SampleService.class.getMethod("doSomething");
            String key = RateLimitAspect.buildBucketKey(method, new Object[] {}, "   ");
            assertThat(key).isEqualTo("SampleService.doSomething");
        }

    }

    // ========== 测试用样例服务 ==========

    @Nested
    @DisplayName("RateLimit 注解为 null")
    class NullAnnotation {

        @Test
        @DisplayName("方法无 @RateLimit 注解 → 直接执行 proceed")
        void should_proceed_when_annotation_null() throws Throwable {
            // given
            Method noAnnotationMethod = SampleService.class.getMethod("noAnnotationMethod");
            when(joinPoint.proceed()).thenReturn("no-annotation");

            // 使用一个没有 @RateLimit 注解的方法
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getMethod()).thenReturn(noAnnotationMethod);
            when(joinPoint.getTarget()).thenReturn(new SampleService());
            when(joinPoint.getArgs()).thenReturn(new Object[] {});

            // when
            Object result = rateLimitAspect.doRateLimit(joinPoint);

            // then - 注解为 null，直接放行
            assertThat(result).isEqualTo("no-annotation");
        }

    }

}
