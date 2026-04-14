package org.smm.archetype.shared.util;

import com.alibaba.fastjson2.TypeReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.UnitTestBase;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("KryoSerializer - 高性能序列化工具")
class KryoSerializerUTest extends UnitTestBase {

    // ──────────────────────────────────────────────
    // 参数校验
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("参数校验")
    class ParameterValidation {

        @Test
        @DisplayName("序列化 null 对象应抛出 IllegalArgumentException")
        void should_throw_for_null_input() {
            assertThatThrownBy(() -> KryoSerializer.serialize(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Cannot serialize null object");
        }

        @Test
        @DisplayName("反序列化 null 字节数组应抛出 IllegalArgumentException")
        void should_throw_for_null_bytes() {
            assertThatThrownBy(() -> KryoSerializer.deserialize(null, String.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid byte array");
        }

        @Test
        @DisplayName("反序列化空字节数组应抛出 IllegalArgumentException")
        void should_throw_for_empty_bytes() {
            assertThatThrownBy(() -> KryoSerializer.deserialize(new byte[0], String.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid byte array");
        }

        @Test
        @DisplayName("反序列化 null 类型应抛出 IllegalArgumentException")
        void should_throw_for_null_type() {
            byte[] bytes = KryoSerializer.serialize("test");

            assertThatThrownBy(() -> KryoSerializer.deserialize(bytes, (Class<?>) null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Target type cannot be null");
        }

    }

    // ──────────────────────────────────────────────
    // 基础类型往返序列化
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("基础类型往返序列化")
    class BasicTypeRoundtrip {

        @Test
        @DisplayName("字符串序列化与反序列化应保持值一致")
        void should_roundtrip_string() {
            // given
            String original = "hello";

            // when
            byte[] bytes = KryoSerializer.serialize(original);
            String result = KryoSerializer.deserialize(bytes, String.class);

            // then
            assertThat(result).isEqualTo(original);
        }

        @Test
        @DisplayName("整数序列化与反序列化应保持值一致")
        void should_roundtrip_integer() {
            // given
            Integer original = 42;

            // when
            byte[] bytes = KryoSerializer.serialize(original);
            Integer result = KryoSerializer.deserialize(bytes, Integer.class);

            // then
            assertThat(result).isEqualTo(original);
        }

        @Test
        @DisplayName("List<String> 序列化与反序列化应保持值一致")
        void should_roundtrip_list_of_strings() {
            // given
            List<String> original = List.of("a", "b");

            // when
            byte[] bytes = KryoSerializer.serialize(original);
            List<String> result = KryoSerializer.deserialize(bytes, new TypeReference<List<String>>() {});

            // then
            assertThat(result).isEqualTo(original);
        }

        @Test
        @DisplayName("Map 序列化与反序列化应保持值一致")
        void should_roundtrip_map() {
            // given
            Map<String, String> original = Map.of("key", "value");

            // when
            byte[] bytes = KryoSerializer.serialize(original);
            @SuppressWarnings("unchecked")
            Map<String, String> result = KryoSerializer.deserialize(bytes, new TypeReference<Map<String, String>>() {});

            // then
            assertThat(result).isEqualTo(original);
        }

    }

    // ──────────────────────────────────────────────
    // Java Time 类型往返序列化
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("Java Time 类型往返序列化")
    class JavaTimeRoundtrip {

        @Test
        @DisplayName("Instant 序列化与反序列化应保持值一致")
        void should_roundtrip_instant() {
            // given
            Instant original = Instant.now();

            // when
            byte[] bytes = KryoSerializer.serialize(original);
            Instant result = KryoSerializer.deserialize(bytes, Instant.class);

            // then
            assertThat(result).isEqualTo(original);
        }

        @Test
        @DisplayName("LocalDateTime 序列化与反序列化应保持值一致")
        void should_roundtrip_localdatetime() {
            // given
            LocalDateTime original = LocalDateTime.now();

            // when
            byte[] bytes = KryoSerializer.serialize(original);
            LocalDateTime result = KryoSerializer.deserialize(bytes, LocalDateTime.class);

            // then
            assertThat(result).isEqualTo(original);
        }

    }

    // ──────────────────────────────────────────────
    // 特殊类型往返序列化
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("特殊类型往返序列化")
    class SpecialTypeRoundtrip {

        @Test
        @DisplayName("Optional.empty 序列化与反序列化应保持为 empty")
        void should_roundtrip_optional_empty() {
            // given
            Optional<?> original = Optional.empty();

            // when
            byte[] bytes = KryoSerializer.serialize(original);
            Optional<?> result = KryoSerializer.deserialize(bytes, Optional.class);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Optional.of(\"test\") 序列化与反序列化应保持值一致")
        void should_roundtrip_optional_present() {
            // given
            Optional<String> original = Optional.of("test");

            // when
            byte[] bytes = KryoSerializer.serialize(original);
            Optional<?> result = KryoSerializer.deserialize(bytes, Optional.class);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo("test");
        }

        @Test
        @DisplayName("BigDecimal 序列化与反序列化应保持精度一致")
        void should_roundtrip_bigdecimal() {
            // given
            BigDecimal original = BigDecimal.valueOf(3.14159);

            // when
            byte[] bytes = KryoSerializer.serialize(original);
            BigDecimal result = KryoSerializer.deserialize(bytes, BigDecimal.class);

            // then
            assertThat(result).isEqualByComparingTo(original);
        }

    }

    // ──────────────────────────────────────────────
    // 嵌套泛型与类型安全
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("嵌套泛型与类型安全")
    class NestedGenericAndTypeSafety {

        @Test
        @DisplayName("嵌套泛型 Map<String, List<Integer>> 序列化与反序列化应保持结构一致")
        void should_roundtrip_nested_generic() {
            // given
            Map<String, List<Integer>> original = Map.of("nums", List.of(1, 2, 3));

            // when
            byte[] bytes = KryoSerializer.serialize(original);
            Map<String, List<Integer>> result = KryoSerializer.deserialize(
                    bytes, new TypeReference<Map<String, List<Integer>>>() {});

            // then
            assertThat(result).containsEntry("nums", List.of(1, 2, 3));
        }

        @Test
        @DisplayName("序列化字符串后以 Integer 类型反序列化应抛出 ClassCastException")
        void should_detect_type_mismatch() {
            // given: 序列化一个字符串
            byte[] bytes = KryoSerializer.serialize("string");

            // when / then: 以 Integer 类型反序列化应抛出 ClassCastException
            assertThatThrownBy(() -> KryoSerializer.deserialize(bytes, Integer.class))
                    .isInstanceOf(ClassCastException.class);
        }

    }

}
