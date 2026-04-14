package org.smm.archetype.shared.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SensitiveLogUtils")
class SensitiveLogUtilsUTest {

    @Nested
    @DisplayName("mask")
    class Mask {

        @Test
        @DisplayName("手机号脱敏")
        void should_mask_phone() {
            String result = SensitiveLogUtils.mask("13812345678");
            assertThat(result).contains("*");
            assertThat(result.length()).isEqualTo(11);
            assertThat(result).doesNotContain("2", "3", "4");
        }

        @Test
        @DisplayName("空值返回原值")
        void should_return_original_when_blank() {
            assertThat(SensitiveLogUtils.mask("")).isEmpty();
            assertThat(SensitiveLogUtils.mask(null)).isNull();
        }

        @Test
        @DisplayName("1 字符字符串保持原长度，1 个字符 + *")
        void should_mask_singleChar() {
            String result = SensitiveLogUtils.mask("A");
            assertThat(result).isEqualTo("A*");
        }

        @Test
        @DisplayName("2 字符字符串保持原长度，1 个字符 + *")
        void should_mask_twoChars() {
            String result = SensitiveLogUtils.mask("AB");
            assertThat(result).isEqualTo("A*");
        }

        @Test
        @DisplayName("3 字符字符串保留首尾，中间脱敏")
        void should_mask_threeChars() {
            String result = SensitiveLogUtils.mask("ABC");
            assertThat(result).hasSize(3);
            assertThat(result).startsWith("A");
            assertThat(result).contains("*");
        }

        @Test
        @DisplayName("自定义 ratio=0.5 时约一半字符被脱敏")
        void should_mask_half_withCustomRatio() {
            String result = SensitiveLogUtils.mask("ABCDEFGH", 0.5);
            assertThat(result).hasSize(8);
            assertThat(result).contains("*");
            // 首尾各保留约 2 个字符
            assertThat(result).startsWith("AB");
        }

    }

}
