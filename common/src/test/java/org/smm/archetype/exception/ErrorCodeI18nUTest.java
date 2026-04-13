package org.smm.archetype.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ErrorCode 国际化能力单元测试
 * <p>
 * 验证 ErrorCode 接口的 messageKey() 默认方法正确性
 */
@DisplayName("ErrorCode messageKey 国际化")
class ErrorCodeI18nUTest {

    @Nested
    @DisplayName("messageKey() 格式验证")
    class MessageKeyFormat {

        @Test
        @DisplayName("FAIL 的 messageKey 为 'error.2000'")
        void should_failMessageKeyBeError2000() {
            assertThat(CommonErrorCode.FAIL.messageKey()).isEqualTo("error.2000");
        }

        @Test
        @DisplayName("SUCCESS 的 messageKey 为 'error.1000'")
        void should_successMessageKeyBeError1000() {
            assertThat(CommonErrorCode.SUCCESS.messageKey()).isEqualTo("error.1000");
        }

        @Test
        @DisplayName("AUTH_UNAUTHORIZED 的 messageKey 为 'error.6601'")
        void should_authUnauthorizedMessageKeyBeError6601() {
            assertThat(CommonErrorCode.AUTH_UNAUTHORIZED.messageKey()).isEqualTo("error.6601");
        }
    }

    @Nested
    @DisplayName("messageKey() 遍历验证")
    class AllEnumValues {

        @Test
        @DisplayName("所有 CommonErrorCode 枚举值都有 messageKey")
        void should_allEnumValuesHaveMessageKey() {
            for (CommonErrorCode errorCode : CommonErrorCode.values()) {
                assertThat(errorCode.messageKey()).isNotNull().isNotEmpty();
            }
        }

        @Test
        @DisplayName("所有 messageKey 格式为 'error.' + code()")
        void should_allMessageKeysFollowFormat() {
            for (CommonErrorCode errorCode : CommonErrorCode.values()) {
                assertThat(errorCode.messageKey())
                        .as("messageKey for %s", errorCode)
                        .isEqualTo("error." + errorCode.code());
            }
        }
    }

    @Nested
    @DisplayName("向后兼容性")
    class BackwardCompatibility {

        @Test
        @DisplayName("message() 保持返回中文消息")
        void should_messageReturnChineseText() {
            assertThat(CommonErrorCode.FAIL.message()).isEqualTo("操作失败");
            assertThat(CommonErrorCode.SUCCESS.message()).isEqualTo("success");
            assertThat(CommonErrorCode.AUTH_UNAUTHORIZED.message()).isEqualTo("未登录或登录已过期");
        }
    }
}
