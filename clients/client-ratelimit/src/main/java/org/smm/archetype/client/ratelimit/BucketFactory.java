package org.smm.archetype.client.ratelimit;

import io.github.bucket4j.Bucket;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Bucket4j Bucket 工厂。
 * <p>
 * 封装令牌桶的创建逻辑，支持自定义容量和补充策略。
 */
public final class BucketFactory {

    private BucketFactory() {
        // 工具类禁止实例化
    }

    /**
     * 创建令牌桶实例。
     *
     * @param capacity      桶容量（最大突发请求数）
     * @param refillTokens  每次补充的令牌数
     * @param refillDuration 补充时间窗口长度
     * @param refillUnit    补充时间单位
     * @return Bucket 实例
     */
    public static Bucket createBucket(double capacity, double refillTokens,
                                       long refillDuration, TimeUnit refillUnit) {
        Duration refillDurationDuration = Duration.of(refillDuration, 
                refillUnit.toChronoUnit());

        return Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(Math.max(1, (long) capacity))
                        .refillGreedy(Math.max(1, (long) refillTokens), refillDurationDuration))
                .build();
    }
}
