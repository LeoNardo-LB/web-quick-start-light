package org.smm.archetype.client.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Caffeine 的本地缓存实现。
 * <p>
 * 每个 entry 拥有独立的过期时间，通过 Caffeine Expiry 策略实现。
 * 支持单个值和 List 类型缓存。
 */
public class CaffeineCacheClient extends AbstractCacheClient {

    private final Cache<String, CacheValueWrapper> cache;
    private final Duration defaultDuration;

    public CaffeineCacheClient(Integer initialCapacity, Long maximumSize, Duration expireAfterWrite) {
        this.defaultDuration = expireAfterWrite;
        this.cache = Caffeine.newBuilder()
                .initialCapacity(initialCapacity != null ? initialCapacity : 1000)
                .maximumSize(maximumSize != null ? maximumSize : 10000L)
                .expireAfter(new CaffeineExpiry())
                .build();
    }

    @Override
    protected Duration getDefaultDuration() {
        return defaultDuration;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T doGet(String key) {
        CacheValueWrapper wrapper = cache.getIfPresent(key);
        if (wrapper == null) {
            return null;
        }
        if (wrapper.value instanceof List<?> list) {
            // List 类型应使用 getList 方法
            return (T) list;
        }
        return (T) wrapper.value;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> List<T> doGetList(String key) {
        CacheValueWrapper wrapper = cache.getIfPresent(key);
        if (wrapper == null) {
            return null;
        }
        Object value = wrapper.value;
        if (value instanceof List<?> list) {
            return (List<T>) list;
        }
        // 单值包装为单元素列表
        List<T> result = new ArrayList<>(1);
        result.add((T) value);
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> List<T> doGetListRange(String key, int beginIdx, int endIdx) {
        List<T> fullList = doGetList(key);
        if (fullList == null) {
            return null;
        }
        int size = fullList.size();
        int from = Math.min(beginIdx, size);
        int to = Math.min(endIdx, size);
        if (from >= to) {
            return new ArrayList<>();
        }
        return fullList.subList(from, to);
    }

    @Override
    protected void doPut(String key, Object value, Duration duration) {
        cache.put(key, new CacheValueWrapper(value, duration));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doAppend(String key, Object value) {
        CacheValueWrapper wrapper = cache.getIfPresent(key);
        List<Object> list;
        if (wrapper == null || !(wrapper.value instanceof List)) {
            list = new ArrayList<>();
        } else {
            list = new ArrayList<>((List<Object>) wrapper.value);
        }
        list.add(value);
        cache.put(key, new CacheValueWrapper(list, wrapper != null ? wrapper.expireDuration : defaultDuration));
    }

    @Override
    protected void doDelete(String key) {
        cache.invalidate(key);
    }

    @Override
    protected Boolean doHasKey(String key) {
        return cache.getIfPresent(key) != null;
    }

    @Override
    protected Boolean doExpire(String key, long timeout, TimeUnit unit) {
        CacheValueWrapper wrapper = cache.getIfPresent(key);
        if (wrapper == null) {
            return false;
        }
        // 通过重新写入来更新过期时间
        Duration newDuration = Duration.ofNanos(unit.toNanos(timeout));
        cache.put(key, new CacheValueWrapper(wrapper.value, newDuration));
        return true;
    }

    @Override
    protected Long doGetExpire(String key) {
        CacheValueWrapper wrapper = cache.getIfPresent(key);
        if (wrapper == null) {
            return null;
        }
        long elapsedNanos = System.nanoTime() - wrapper.createTimeNanos;
        long expireNanos = wrapper.expireDuration.toNanos();
        long remainingNanos = expireNanos - elapsedNanos;
        if (remainingNanos <= 0) {
            return 0L;
        }
        return TimeUnit.NANOSECONDS.toSeconds(remainingNanos);
    }

    // ==================== 内部类 ====================

    /**
     * 缓存值包装器，携带每个 entry 的过期时间信息。
     */
    private static class CacheValueWrapper {
        final Object value;
        final Duration expireDuration;
        final long createTimeNanos;

        CacheValueWrapper(Object value, Duration expireDuration) {
            this.value = value;
            this.expireDuration = expireDuration;
            this.createTimeNanos = System.nanoTime();
        }
    }

    /**
     * Caffeine 自定义过期策略，基于每个 entry 的独立过期时间。
     */
    private static class CaffeineExpiry implements Expiry<String, CacheValueWrapper> {

        @Override
        public long expireAfterCreate(String key, CacheValueWrapper wrapper, long currentTime) {
            return wrapper.expireDuration.toNanos();
        }

        @Override
        public long expireAfterUpdate(String key, CacheValueWrapper wrapper,
                                       long currentTime, long currentDuration) {
            // 更新后重置过期时间
            return wrapper.expireDuration.toNanos();
        }

        @Override
        public long expireAfterRead(String key, CacheValueWrapper wrapper,
                                     long currentTime, long currentDuration) {
            // 读取不影响过期时间
            return currentDuration;
        }
    }
}
