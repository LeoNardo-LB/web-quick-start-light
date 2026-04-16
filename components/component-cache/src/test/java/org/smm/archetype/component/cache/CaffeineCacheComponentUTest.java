package org.smm.archetype.component.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.exception.ClientException;
import org.smm.archetype.exception.CommonErrorCode;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CaffeineCacheComponent 单元测试。
 * <p>
 * 测试所有公开方法，基于 Caffeine 本地缓存实现。
 */
@DisplayName("CaffeineCacheComponent 功能测试")
class CaffeineCacheComponentUTest {

    private CaffeineCacheComponent cache;

    @BeforeEach
    void setUp() {
        cache = new CaffeineCacheComponent(100, 1000L, Duration.ofMinutes(30));
    }

    // ==================== put + get 往返测试 ====================

    @Nested
    @DisplayName("put + get 往返操作")
    class PutGetRoundtripTests {

        @Test
        @DisplayName("put 后 get 应返回相同值")
        void should_return_same_value_after_put() {
            cache.put("key1", "value1");

            String result = cache.get("key1");

            assertThat(result).isEqualTo("value1");
        }

        @Test
        @DisplayName("get 不存在的 key 应返回 null")
        void should_return_null_for_non_existing_key() {
            String result = cache.get("nonexistent");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("put 覆盖后 get 应返回新值")
        void should_return_new_value_after_overwrite() {
            cache.put("key1", "oldValue");
            cache.put("key1", "newValue");

            String result = cache.get("key1");

            assertThat(result).isEqualTo("newValue");
        }
    }

    // ==================== put with TTL + getExpire ====================

    @Nested
    @DisplayName("put 指定 TTL + getExpire")
    class PutWithTtlTests {

        @Test
        @DisplayName("put 指定 TTL 后 getExpire 应返回正数秒数")
        void should_return_positive_seconds_after_put_with_ttl() {
            cache.put("ttlKey", "value", Duration.ofSeconds(60));

            Long remaining = cache.getExpire("ttlKey");

            assertThat(remaining).isNotNull();
            assertThat(remaining).isGreaterThan(0L);
            assertThat(remaining).isLessThanOrEqualTo(60L);
        }

        @Test
        @DisplayName("使用默认 TTL put 后 getExpire 应返回正数秒数")
        void should_return_positive_seconds_with_default_ttl() {
            cache.put("defaultTtlKey", "value");

            Long remaining = cache.getExpire("defaultTtlKey");

            assertThat(remaining).isNotNull();
            assertThat(remaining).isGreaterThan(0L);
        }

        @Test
        @DisplayName("getExpire 不存在的 key 应返回 null")
        void should_return_null_for_non_existing_key() {
            Long remaining = cache.getExpire("nonexistent");

            assertThat(remaining).isNull();
        }
    }

    // ==================== getList / getList with range ====================

    @Nested
    @DisplayName("getList / getList 分页操作")
    class GetListTests {

        @Test
        @DisplayName("getList 应返回完整列表")
        void should_return_full_list() {
            List<String> list = List.of("a", "b", "c");
            cache.put("listKey", list);

            List<String> result = cache.getList("listKey");

            assertThat(result).containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("getList 不存在的 key 应返回 null")
        void should_return_null_for_non_existing_list_key() {
            List<String> result = cache.getList("nonexistent");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("getList 范围查询应返回子列表")
        void should_return_sublist_for_range_query() {
            List<String> list = List.of("a", "b", "c", "d", "e");
            cache.put("rangeKey", list);

            List<String> result = cache.getList("rangeKey", 1, 4);

            assertThat(result).containsExactly("b", "c", "d");
        }

        @Test
        @DisplayName("getList 范围超出应自动截断")
        void should_truncate_when_range_exceeds_size() {
            List<String> list = List.of("a", "b", "c");
            cache.put("truncateKey", list);

            List<String> result = cache.getList("truncateKey", 1, 100);

            assertThat(result).containsExactly("b", "c");
        }

        @Test
        @DisplayName("getList 范围起始超出应返回空列表")
        void should_return_empty_when_begin_exceeds_size() {
            List<String> list = List.of("a", "b", "c");
            cache.put("beyondKey", list);

            List<String> result = cache.getList("beyondKey", 10, 20);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getList 不存在的 key 的范围查询应返回 null")
        void should_return_null_for_non_existing_range_key() {
            List<String> result = cache.getList("nonexistent", 0, 10);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("getList 单值应包装为单元素列表")
        void should_wrap_single_value_as_list() {
            cache.put("singleKey", "singleValue");

            List<String> result = cache.getList("singleKey");

            assertThat(result).containsExactly("singleValue");
        }
    }

    // ==================== append ====================

    @Nested
    @DisplayName("append 追加操作")
    class AppendTests {

        @Test
        @DisplayName("append 到已存在的列表应追加元素")
        void should_append_to_existing_list() {
            cache.put("appendKey", List.of("a", "b"));

            cache.append("appendKey", "c");

            List<String> result = cache.getList("appendKey");
            assertThat(result).containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("append 到不存在的 key 应创建新列表")
        void should_create_new_list_when_appending_to_non_existing() {
            cache.append("newAppendKey", "first");

            List<String> result = cache.getList("newAppendKey");
            assertThat(result).containsExactly("first");
        }

        @Test
        @DisplayName("append 到单值应创建新列表（原有单值被替换）")
        void should_replace_single_value_with_new_list_on_append() {
            cache.put("convertKey", "original");

            cache.append("convertKey", "appended");

            // 单值不是 List，doAppend 会创建新列表，原有单值被替换
            List<String> result = cache.getList("convertKey");
            assertThat(result).containsExactly("appended");
        }
    }

    // ==================== delete + hasKey ====================

    @Nested
    @DisplayName("delete + hasKey 操作")
    class DeleteHasKeyTests {

        @Test
        @DisplayName("delete 已存在的 key 后 hasKey 应返回 false")
        void should_not_exist_after_delete() {
            cache.put("deleteKey", "value");
            assertThat(cache.hasKey("deleteKey")).isTrue();

            cache.delete("deleteKey");

            assertThat(cache.hasKey("deleteKey")).isFalse();
        }

        @Test
        @DisplayName("delete 不存在的 key 不应抛异常")
        void should_not_throw_when_deleting_non_existing() {
            cache.delete("nonexistent");
            assertThat(cache.hasKey("nonexistent")).isFalse();
        }

        @Test
        @DisplayName("hasKey 不存在的 key 应返回 false")
        void should_return_false_for_non_existing_key() {
            assertThat(cache.hasKey("nonexistent")).isFalse();
        }

        @Test
        @DisplayName("put 后 hasKey 应返回 true")
        void should_return_true_after_put() {
            cache.put("existsKey", "value");

            assertThat(cache.hasKey("existsKey")).isTrue();
        }
    }

    // ==================== expire ====================

    @Nested
    @DisplayName("expire 过期时间设置")
    class ExpireTests {

        @Test
        @DisplayName("expire 应更新已有 key 的过期时间")
        void should_update_expire_for_existing_key() {
            cache.put("expireKey", "value");

            Boolean result = cache.expire("expireKey", 120, TimeUnit.SECONDS);

            assertThat(result).isTrue();
            Long remaining = cache.getExpire("expireKey");
            assertThat(remaining).isNotNull().isGreaterThan(0L);
            assertThat(remaining).isLessThanOrEqualTo(120L);
        }

        @Test
        @DisplayName("expire 不存在的 key 应返回 false")
        void should_return_false_for_non_existing_key() {
            Boolean result = cache.expire("nonexistent", 60, TimeUnit.SECONDS);

            assertThat(result).isFalse();
        }
    }

    // ==================== 参数校验 ====================

    @Nested
    @DisplayName("参数校验")
    class ValidationTests {

        @Test
        @DisplayName("null key 应抛出 ClientException")
        void should_throw_for_null_key() {
            assertThatThrownBy(() -> cache.put(null, "value"))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("空字符串 key 应抛出 ClientException")
        void should_throw_for_empty_key() {
            assertThatThrownBy(() -> cache.put("", "value"))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("null key 调用 get 应抛出 ClientException")
        void should_throw_for_null_key_on_get() {
            assertThatThrownBy(() -> cache.get(null))
                    .isInstanceOf(ClientException.class);
        }

        @Test
        @DisplayName("空字符串 key 调用 delete 应抛出 ClientException")
        void should_throw_for_empty_key_on_delete() {
            assertThatThrownBy(() -> cache.delete(""))
                    .isInstanceOf(ClientException.class);
        }

        @Test
        @DisplayName("null key 调用 hasKey 应抛出 ClientException")
        void should_throw_for_null_key_on_hasKey() {
            assertThatThrownBy(() -> cache.hasKey(null))
                    .isInstanceOf(ClientException.class);
        }

        @Test
        @DisplayName("负数 duration 应抛出 ClientException")
        void should_throw_for_negative_duration() {
            assertThatThrownBy(() -> cache.put("key", "value", Duration.ofSeconds(-1)))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("零 duration 应抛出 ClientException")
        void should_throw_for_zero_duration() {
            assertThatThrownBy(() -> cache.put("key", "value", Duration.ZERO))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("null duration 应抛出 ClientException")
        void should_throw_for_null_duration() {
            assertThatThrownBy(() -> cache.put("key", "value", null))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("append null 值应抛出 ClientException")
        void should_throw_for_null_append_value() {
            assertThatThrownBy(() -> cache.append("key", null))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("负数 timeout 调用 expire 应抛出 ClientException")
        void should_throw_for_negative_timeout_on_expire() {
            assertThatThrownBy(() -> cache.expire("key", -1, TimeUnit.SECONDS))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("零 timeout 调用 expire 应抛出 ClientException")
        void should_throw_for_zero_timeout_on_expire() {
            assertThatThrownBy(() -> cache.expire("key", 0, TimeUnit.SECONDS))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("null TimeUnit 调用 expire 应抛出 ClientException")
        void should_throw_for_null_time_unit_on_expire() {
            assertThatThrownBy(() -> cache.expire("key", 60, null))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("getList 范围 beginIdx > endIdx 应抛出 ClientException")
        void should_throw_when_begin_idx_greater_than_end_idx() {
            assertThatThrownBy(() -> cache.getList("key", 5, 3))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("getList 范围负数索引应抛出 ClientException")
        void should_throw_for_negative_index() {
            assertThatThrownBy(() -> cache.getList("key", -1, 5))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }
    }

    // ==================== CacheProperties 默认值 ====================

    @Nested
    @DisplayName("CacheProperties 默认值")
    class CachePropertiesTests {

        @Test
        @DisplayName("使用 CacheProperties 默认值构造 CaffeineCacheComponent 应正常工作")
        void should_work_with_default_properties() {
            // CacheProperties 默认值: initialCapacity=1000, maximumSize=10000, expireAfterWrite=30天
            CaffeineCacheComponent defaultCache = new CaffeineCacheComponent(
                    1000,
                    10000L,
                    Duration.ofDays(30)
            );

            defaultCache.put("key", "value");
            String retrieved = defaultCache.get("key");
            assertThat(retrieved).isEqualTo("value");
        }
    }

    // ==================== CaffeineExpiry 行为测试 ====================

    @Nested
    @DisplayName("CaffeineExpiry 过期行为")
    class CaffeineExpiryTests {

        @Test
        @DisplayName("读取不应影响过期时间（getExpire 变化应极小）")
        void should_not_affect_expire_on_read() {
            cache.put("readKey", "value", Duration.ofSeconds(10));

            Long remaining1 = cache.getExpire("readKey");
            cache.get("readKey");
            Long remaining2 = cache.getExpire("readKey");

            assertThat(remaining2).isNotNull();
            assertThat(remaining2).isCloseTo(remaining1, org.assertj.core.data.Offset.offset(1L));
        }

        @Test
        @DisplayName("短 TTL 的 key 应能正确存储和获取")
        void should_handle_short_ttl_correctly() {
            cache.put("shortTtl", "value", Duration.ofMillis(100));

            String value = cache.get("shortTtl");
            assertThat(value).isEqualTo("value");
            Boolean exists = cache.hasKey("shortTtl");
            assertThat(exists).isTrue();
        }
    }
}
