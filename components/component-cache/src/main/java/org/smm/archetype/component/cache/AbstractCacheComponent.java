package org.smm.archetype.component.cache;

import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.exception.ClientException;
import org.smm.archetype.exception.CommonErrorCode;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * CacheComponent 抽象基类，使用 Template Method 模式。
 * <p>
 * 所有公开方法标记为 final，完成参数校验、异常处理与日志记录。
 * 子类实现 do* 扩展点完成具体缓存操作。
 */
@Slf4j
public abstract class AbstractCacheComponent implements CacheComponent {

    @Override
    public final <T> T get(String key) {
        validateKey(key);
        log.debug("Cache get: key={}", key);
        try {
            return doGet(key);
        } catch (Exception e) {
            log.error("Cache get 异常: key={}", key, e);
            throw new ClientException(CommonErrorCode.CACHE_OPERATION_FAILED, "缓存读取失败: " + key, e);
        }
    }

    @Override
    public final <T> List<T> getList(String key) {
        validateKey(key);
        log.debug("Cache getList: key={}", key);
        try {
            return doGetList(key);
        } catch (Exception e) {
            log.error("Cache getList 异常: key={}", key, e);
            throw new ClientException(CommonErrorCode.CACHE_OPERATION_FAILED, "缓存读取失败: " + key, e);
        }
    }

    @Override
    public final <T> List<T> getList(String key, int beginIdx, int endIdx) {
        validateKey(key);
        if (beginIdx < 0 || endIdx < 0 || beginIdx > endIdx) {
            throw new ClientException(CommonErrorCode.ILLEGAL_ARGUMENT,
                    "索引范围不合法: beginIdx=" + beginIdx + ", endIdx=" + endIdx);
        }
        log.debug("Cache getList range: key={}, beginIdx={}, endIdx={}", key, beginIdx, endIdx);
        try {
            return doGetListRange(key, beginIdx, endIdx);
        } catch (Exception e) {
            log.error("Cache getList range 异常: key={}", key, e);
            throw new ClientException(CommonErrorCode.CACHE_OPERATION_FAILED, "缓存读取失败: " + key, e);
        }
    }

    @Override
    public final void put(String key, Object value) {
        validateKey(key);
        log.debug("Cache put: key={}", key);
        try {
            doPut(key, value, getDefaultDuration());
        } catch (Exception e) {
            log.error("Cache put 异常: key={}", key, e);
            throw new ClientException(CommonErrorCode.CACHE_OPERATION_FAILED, "缓存写入失败: " + key, e);
        }
    }

    @Override
    public final void put(String key, Object value, Duration duration) {
        validateKey(key);
        if (duration == null || duration.isNegative() || duration.isZero()) {
            throw new ClientException(CommonErrorCode.ILLEGAL_ARGUMENT, "过期时间必须为正数");
        }
        log.debug("Cache put with TTL: key={}, duration={}", key, duration);
        try {
            doPut(key, value, duration);
        } catch (Exception e) {
            log.error("Cache put with TTL 异常: key={}", key, e);
            throw new ClientException(CommonErrorCode.CACHE_OPERATION_FAILED, "缓存写入失败: " + key, e);
        }
    }

    @Override
    public final void append(String key, Object value) {
        validateKey(key);
        if (value == null) {
            throw new ClientException(CommonErrorCode.ILLEGAL_ARGUMENT, "追加值不能为空");
        }
        log.debug("Cache append: key={}", key);
        try {
            doAppend(key, value);
        } catch (Exception e) {
            log.error("Cache append 异常: key={}", key, e);
            throw new ClientException(CommonErrorCode.CACHE_OPERATION_FAILED, "缓存追加失败: " + key, e);
        }
    }

    @Override
    public final void delete(String key) {
        validateKey(key);
        log.debug("Cache delete: key={}", key);
        try {
            doDelete(key);
        } catch (Exception e) {
            log.error("Cache delete 异常: key={}", key, e);
            throw new ClientException(CommonErrorCode.CACHE_OPERATION_FAILED, "缓存删除失败: " + key, e);
        }
    }

    @Override
    public final Boolean hasKey(String key) {
        validateKey(key);
        log.debug("Cache hasKey: key={}", key);
        try {
            return doHasKey(key);
        } catch (Exception e) {
            log.error("Cache hasKey 异常: key={}", key, e);
            throw new ClientException(CommonErrorCode.CACHE_OPERATION_FAILED, "缓存检查失败: " + key, e);
        }
    }

    @Override
    public final Boolean expire(String key, long timeout, TimeUnit unit) {
        validateKey(key);
        if (timeout <= 0) {
            throw new ClientException(CommonErrorCode.ILLEGAL_ARGUMENT, "超时时间必须为正数");
        }
        if (unit == null) {
            throw new ClientException(CommonErrorCode.ILLEGAL_ARGUMENT, "时间单位不能为空");
        }
        log.debug("Cache expire: key={}, timeout={}, unit={}", key, timeout, unit);
        try {
            return doExpire(key, timeout, unit);
        } catch (Exception e) {
            log.error("Cache expire 异常: key={}", key, e);
            throw new ClientException(CommonErrorCode.CACHE_OPERATION_FAILED, "缓存过期设置失败: " + key, e);
        }
    }

    @Override
    public final Long getExpire(String key) {
        validateKey(key);
        log.debug("Cache getExpire: key={}", key);
        try {
            return doGetExpire(key);
        } catch (Exception e) {
            log.error("Cache getExpire 异常: key={}", key, e);
            throw new ClientException(CommonErrorCode.CACHE_OPERATION_FAILED, "缓存过期查询失败: " + key, e);
        }
    }

    // ==================== 参数校验 ====================

    private void validateKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new ClientException(CommonErrorCode.ILLEGAL_ARGUMENT, "cache key must not be null or empty");
        }
    }

    // ==================== 子类扩展点 ====================

    /**
     * 子类实现：获取默认过期时间
     */
    protected abstract Duration getDefaultDuration();

    /**
     * 子类实现：读取缓存
     */
    protected abstract <T> T doGet(String key);

    /**
     * 子类实现：读取 List 类型缓存
     */
    protected abstract <T> List<T> doGetList(String key);

    /**
     * 子类实现：读取 List 类型缓存子列表
     */
    protected abstract <T> List<T> doGetListRange(String key, int beginIdx, int endIdx);

    /**
     * 子类实现：写入缓存（指定过期时间）
     */
    protected abstract void doPut(String key, Object value, Duration duration);

    /**
     * 子类实现：追加元素到 List 类型缓存
     */
    protected abstract void doAppend(String key, Object value);

    /**
     * 子类实现：删除缓存
     */
    protected abstract void doDelete(String key);

    /**
     * 子类实现：判断键是否存在
     */
    protected abstract Boolean doHasKey(String key);

    /**
     * 子类实现：设置过期时间
     */
    protected abstract Boolean doExpire(String key, long timeout, TimeUnit unit);

    /**
     * 子类实现：获取剩余过期时间
     */
    protected abstract Long doGetExpire(String key);
}
