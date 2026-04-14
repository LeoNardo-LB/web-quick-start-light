package org.smm.archetype.shared.aspect.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BucketFactory")
class BucketFactoryUTest {

    @Nested
    @DisplayName("createBucket")
    class CreateBucket {

        @Test
        @DisplayName("正常创建桶 → 桶可用")
        void should_create_bucket_with_normal_params() {
            Bucket bucket = BucketFactory.createBucket(10, 10, 1, TimeUnit.SECONDS);
            assertThat(bucket).isNotNull();
            assertThat(bucket.tryConsume(1)).isTrue();
        }

        @Test
        @DisplayName("容量为 0 → Math.max(1, 0) 最小容量为 1，桶仍可用")
        void should_create_bucket_with_zero_capacity() {
            Bucket bucket = BucketFactory.createBucket(0, 10, 1, TimeUnit.SECONDS);
            assertThat(bucket).isNotNull();
            // 最小容量为 1，消费 1 个令牌应成功
            assertThat(bucket.tryConsume(1)).isTrue();
        }

        @Test
        @DisplayName("refillTokens 为 0 → Math.max(1, 0) 最小补充为 1")
        void should_create_bucket_with_zero_refill_tokens() {
            Bucket bucket = BucketFactory.createBucket(10, 0, 1, TimeUnit.SECONDS);
            assertThat(bucket).isNotNull();
            // 桶可用
            assertThat(bucket.tryConsume(1)).isTrue();
        }

        @Test
        @DisplayName("容量 5 → 消费 5 个令牌成功，消费第 6 个失败")
        void should_consume_up_to_capacity() {
            Bucket bucket = BucketFactory.createBucket(5, 5, 1, TimeUnit.SECONDS);
            // 消费 5 个令牌
            assertThat(bucket.tryConsume(5)).isTrue();
            // 第 6 个令牌不可用
            assertThat(bucket.tryConsume(1)).isFalse();
        }

        @Test
        @DisplayName("消费所有令牌后等待补充 → 令牌可用")
        void should_refill_tokens_after_wait() throws InterruptedException {
            // 使用极短补充周期：容量 1，每 100ms 补充 1 个
            Bucket bucket = BucketFactory.createBucket(1, 1, 100, TimeUnit.MILLISECONDS);

            // 消费唯一的令牌
            assertThat(bucket.tryConsume(1)).isTrue();
            assertThat(bucket.tryConsume(1)).isFalse();

            // 等待令牌补充
            Thread.sleep(150);

            // 令牌应已补充
            assertThat(bucket.tryConsume(1)).isTrue();
        }

        @Test
        @DisplayName("tryConsumeAndReturnRemaining → 返回正确的 ConsumptionProbe")
        void should_return_correct_consumption_probe() {
            Bucket bucket = BucketFactory.createBucket(10, 10, 1, TimeUnit.SECONDS);

            // 消费 3 个令牌
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(3);

            assertThat(probe.isConsumed()).isTrue();
            assertThat(probe.getRemainingTokens()).isEqualTo(7);
        }

        @Test
        @DisplayName("使用不同时间单位创建桶 → 桶正常工作")
        void should_create_bucket_with_minutes_unit() {
            Bucket bucket = BucketFactory.createBucket(10, 10, 1, TimeUnit.MINUTES);
            assertThat(bucket).isNotNull();
            assertThat(bucket.tryConsume(1)).isTrue();
        }

        @Test
        @DisplayName("使用 NANOSECONDS 时间单位创建桶 → 桶正常工作（补充速率 ≤ 1 token/ns）")
        void should_create_bucket_with_nanoseconds_unit() {
            // Bucket4j 限制：最高补充速率为 1 token/nanosecond
            Bucket bucket = BucketFactory.createBucket(10, 1, 1, TimeUnit.NANOSECONDS);
            assertThat(bucket).isNotNull();
            assertThat(bucket.tryConsume(1)).isTrue();
        }

        @Test
        @DisplayName("小数容量参数 → 向下取整后 Math.max(1, ...) 确保最小容量")
        void should_handle_fractional_capacity() {
            Bucket bucket = BucketFactory.createBucket(0.5, 10, 1, TimeUnit.SECONDS);
            assertThat(bucket).isNotNull();
            // (long) 0.5 = 0, Math.max(1, 0) = 1
            assertThat(bucket.tryConsume(1)).isTrue();
        }

    }

}
