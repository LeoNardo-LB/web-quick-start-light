package org.smm.archetype.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.UnitTestBase;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CommonErrorCode 枚举")
class CommonErrorCodeUTest extends UnitTestBase {

    @Nested
    @DisplayName("SUCCESS")
    class Success {

        @Test
        @DisplayName("SUCCESS.code() 应返回 1000")
        void should_have_success_code_1000() {
            assertThat(CommonErrorCode.SUCCESS.code()).isEqualTo(1000);
        }

        @Test
        @DisplayName("SUCCESS.message() 应返回 \"success\"")
        void should_have_success_message() {
            assertThat(CommonErrorCode.SUCCESS.message()).isEqualTo("success");
        }
    }

    @Nested
    @DisplayName("FAIL")
    class Fail {

        @Test
        @DisplayName("FAIL.code() 应返回 2000")
        void should_have_fail_code_2000() {
            assertThat(CommonErrorCode.FAIL.code()).isEqualTo(2000);
        }
    }

    @Nested
    @DisplayName("ILLEGAL_ARGUMENT")
    class IllegalArgument {

        @Test
        @DisplayName("ILLEGAL_ARGUMENT.code() 应返回 2001")
        void should_have_illegal_argument_code_2001() {
            assertThat(CommonErrorCode.ILLEGAL_ARGUMENT.code()).isEqualTo(2001);
        }
    }

    @Nested
    @DisplayName("RPC_EXCEPTION")
    class RpcException {

        @Test
        @DisplayName("RPC_EXCEPTION.code() 应返回 2002")
        void should_have_rpc_exception_code_2002() {
            assertThat(CommonErrorCode.RPC_EXCEPTION.code()).isEqualTo(2002);
        }
    }

    @Nested
    @DisplayName("SYS_ERROR")
    class SysError {

        @Test
        @DisplayName("SYS_ERROR.code() 应返回 5000")
        void should_have_sys_error_code_5000() {
            assertThat(CommonErrorCode.SYS_ERROR.code()).isEqualTo(5000);
        }
    }

    @Nested
    @DisplayName("UNKNOWN_ERROR")
    class UnknownError {

        @Test
        @DisplayName("UNKNOWN_ERROR.code() 应返回 9999")
        void should_have_unknown_error_code_9999() {
            assertThat(CommonErrorCode.UNKNOWN_ERROR.code()).isEqualTo(9999);
        }
    }

    @Nested
    @DisplayName("全局约束")
    class GlobalConstraints {

        @Test
        @DisplayName("所有枚举值的 code 应唯一，且总共有 16 个")
        void should_have_unique_codes_for_all_values() {
            CommonErrorCode[] values = CommonErrorCode.values();
            assertThat(values).hasSize(16);

            long distinctCount = java.util.Arrays.stream(values)
                    .mapToInt(CommonErrorCode::code)
                    .distinct()
                    .count();
            assertThat(distinctCount).isEqualTo(16);
        }

        @Test
        @DisplayName("所有枚举值的 message 不应为 null")
        void should_all_messages_be_non_null() {
            for (CommonErrorCode ec : CommonErrorCode.values()) {
                assertThat(ec.message()).isNotNull();
            }
        }
    }
}
