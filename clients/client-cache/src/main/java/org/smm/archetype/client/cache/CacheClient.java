package org.smm.archetype.client.cache;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 缓存客户端接口。
 * <p>
 * 提供基础的缓存读写操作，支持 List 类型、TTL 过期、键存在性检查等功能。
 */
public interface CacheClient {

    /**
     * 获取缓存值
     *
     * @param key 缓存键
     * @param <T> 值类型
     * @return 缓存值，不存在返回 null
     */
    <T> T get(String key);

    /**
     * 获取 List 类型缓存值
     *
     * @param key 缓存键
     * @param <T> 元素类型
     * @return List 缓存值，不存在返回 null
     */
    <T> List<T> getList(String key);

    /**
     * 获取 List 类型缓存值的子列表
     *
     * @param key      缓存键
     * @param beginIdx 起始索引（包含）
     * @param endIdx   结束索引（不包含）
     * @param <T>      元素类型
     * @return 子列表，不存在返回 null
     */
    <T> List<T> getList(String key, int beginIdx, int endIdx);

    /**
     * 写入缓存（使用默认过期时间）
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    void put(String key, Object value);

    /**
     * 写入缓存（指定过期时间）
     *
     * @param key      缓存键
     * @param value    缓存值
     * @param duration 过期时间
     */
    void put(String key, Object value, Duration duration);

    /**
     * 追加元素到 List 类型缓存
     *
     * @param key   缓存键
     * @param value 要追加的元素
     */
    void append(String key, Object value);

    /**
     * 删除缓存
     *
     * @param key 缓存键
     */
    void delete(String key);

    /**
     * 判断键是否存在
     *
     * @param key 缓存键
     * @return 存在返回 true
     */
    Boolean hasKey(String key);

    /**
     * 设置过期时间
     *
     * @param key     缓存键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 设置成功返回 true
     */
    Boolean expire(String key, long timeout, TimeUnit unit);

    /**
     * 获取剩余过期时间
     *
     * @param key 缓存键
     * @return 剩余秒数，不存在返回 null
     */
    Long getExpire(String key);
}
