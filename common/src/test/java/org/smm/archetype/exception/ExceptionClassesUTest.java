package org.smm.archetype.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * common 模块异常体系单元测试
 * <p>
 * 覆盖 ErrorCode 接口契约、CommonErrorCode 枚举完整性、
 * BaseException 构造器、以及 BizException/ClientException/SysException 继承链。
 */
@DisplayName("异常体系 - common 模块类测试")
class ExceptionClassesUTest {

    // =========================================================================
    // 1. CommonErrorCode 枚举完整性
    // =========================================================================

    @Nested
    @DisplayName("CommonErrorCode 枚举完整性")
    class CommonErrorCodeCompleteness {

        @Test
        @DisplayName("应包含全部 16 个枚举值")
        void should_have_all_enum_values() {
            assertThat(CommonErrorCode.values()).hasSize(16);
        }

        @ParameterizedTest(name = "{0} 的 code 应为 {1}")
        @MethodSource("org.smm.archetype.exception.ExceptionClassesUTest#errorCodePairs")
        @DisplayName("每个枚举值的 code 应与预期一致")
        void should_have_correct_code(CommonErrorCode ec, int expectedCode) {
            assertThat(ec.code()).isEqualTo(expectedCode);
        }

        @ParameterizedTest(name = "{0} 的 message 应为 \"{1}\"")
        @MethodSource("org.smm.archetype.exception.ExceptionClassesUTest#errorCodePairs")
        @DisplayName("每个枚举值的 message 应非空")
        void should_have_non_empty_message(CommonErrorCode ec) {
            assertThat(ec.message()).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("code 范围分布：1xxx/2xxx/5xxx/6xxx")
        void should_codes_distributed_in_expected_ranges() {
            // 1xxx — SUCCESS
            assertThat(CommonErrorCode.SUCCESS.code()).isBetween(1000, 1999);

            // 2xxx — 通用业务错误
            Stream.of(CommonErrorCode.FAIL, CommonErrorCode.ILLEGAL_ARGUMENT, CommonErrorCode.RPC_EXCEPTION)
                    .forEach(ec -> assertThat(ec.code()).as("%s code", ec)
                            .isBetween(2000, 2999));

            // 5xxx — 系统错误
            assertThat(CommonErrorCode.SYS_ERROR.code()).as("SYS_ERROR code")
                    .isBetween(5000, 5999);

            // 9xxx — 未知兜底
            assertThat(CommonErrorCode.UNKNOWN_ERROR.code()).as("UNKNOWN_ERROR code")
                    .isBetween(9000, 9999);

            // 6xxx — 中间件/认证错误
            Stream.of(CommonErrorCode.CACHE_OPERATION_FAILED,
                            CommonErrorCode.OSS_OPERATION_FAILED,
                            CommonErrorCode.OSS_UPLOAD_FAILED,
                            CommonErrorCode.EMAIL_SEND_FAILED,
                            CommonErrorCode.SMS_SEND_FAILED,
                            CommonErrorCode.SEARCH_OPERATION_FAILED,
                            CommonErrorCode.RATE_LIMIT_EXCEEDED,
                            CommonErrorCode.AUTH_UNAUTHORIZED,
                            CommonErrorCode.AUTH_BAD_CREDENTIALS,
                            CommonErrorCode.AUTH_USER_NOT_FOUND)
                    .forEach(ec -> assertThat(ec.code()).as("%s code", ec)
                            .isBetween(6000, 6999));
        }
    }

    /** (CommonErrorCode, expectedCode) 对 */
    static Stream<Arguments> errorCodePairs() {
        return Stream.of(
                arguments(CommonErrorCode.SUCCESS, 1000),
                arguments(CommonErrorCode.FAIL, 2000),
                arguments(CommonErrorCode.ILLEGAL_ARGUMENT, 2001),
                arguments(CommonErrorCode.RPC_EXCEPTION, 2002),
                arguments(CommonErrorCode.SYS_ERROR, 5000),
                arguments(CommonErrorCode.UNKNOWN_ERROR, 9999),
                arguments(CommonErrorCode.CACHE_OPERATION_FAILED, 6001),
                arguments(CommonErrorCode.OSS_OPERATION_FAILED, 6101),
                arguments(CommonErrorCode.OSS_UPLOAD_FAILED, 6102),
                arguments(CommonErrorCode.EMAIL_SEND_FAILED, 6201),
                arguments(CommonErrorCode.SMS_SEND_FAILED, 6301),
                arguments(CommonErrorCode.SEARCH_OPERATION_FAILED, 6401),
                arguments(CommonErrorCode.RATE_LIMIT_EXCEEDED, 6501),
                arguments(CommonErrorCode.AUTH_UNAUTHORIZED, 6601),
                arguments(CommonErrorCode.AUTH_BAD_CREDENTIALS, 6602),
                arguments(CommonErrorCode.AUTH_USER_NOT_FOUND, 6603)
        );
    }

    // =========================================================================
    // 2. ErrorCode 接口契约
    // =========================================================================

    @Nested
    @DisplayName("ErrorCode 接口契约")
    class ErrorCodeContract {

        @ParameterizedTest(name = "{0}")
        @EnumSource(CommonErrorCode.class)
        @DisplayName("code() 应返回非负数")
        void should_codeBeNonNegative(CommonErrorCode ec) {
            assertThat(ec.code()).isGreaterThanOrEqualTo(0);
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(CommonErrorCode.class)
        @DisplayName("message() 应非空")
        void should_messageBeNonNull(CommonErrorCode ec) {
            assertThat(ec.message()).isNotNull();
        }

        @ParameterizedTest(name = "{0} => messageKey=\"{argumentsWithNames}\"")
        @EnumSource(CommonErrorCode.class)
        @DisplayName("messageKey() 格式应为 'error.XXXX'")
        void should_messageKeyFollowPattern(CommonErrorCode ec) {
            assertThat(ec.messageKey())
                    .as("messageKey for %s", ec)
                    .matches("^error\\.\\d+$")
                    .isEqualTo("error." + ec.code());
        }

        @Test
        @DisplayName("所有枚举值 code 应唯一")
        void should_all_codes_be_unique() {
            long distinctCount = Stream.of(CommonErrorCode.values())
                    .mapToInt(CommonErrorCode::code)
                    .distinct()
                    .count();
            assertThat(distinctCount).isEqualTo(CommonErrorCode.values().length);
        }
    }

    // =========================================================================
    // 3. BaseException 构造器
    // =========================================================================

    @Nested
    @DisplayName("BaseException 构造器")
    class BaseExceptionConstructors {

        private final ErrorCode errorCode = CommonErrorCode.FAIL;

        @Test
        @DisplayName("(ErrorCode) — message 应等于 errorCode.message()")
        void should_errorCodeConstructor_setMessageFromErrorCode() {
            // BaseException 是 abstract，用匿名子类测试
            BaseException ex = new BaseException(errorCode) {};

            assertThat(ex.getErrorCode()).isSameAs(errorCode);
            assertThat(ex.getMessage()).isEqualTo(errorCode.message());
            assertThat(ex.getCause()).isNull();
        }

        @Test
        @DisplayName("(ErrorCode, String) — message 应为自定义消息")
        void should_errorCodeMessageConstructor_useCustomMessage() {
            String customMsg = "自定义消息";
            BaseException ex = new BaseException(errorCode, customMsg) {};

            assertThat(ex.getErrorCode()).isSameAs(errorCode);
            assertThat(ex.getMessage()).isEqualTo(customMsg);
            assertThat(ex.getCause()).isNull();
        }

        @Test
        @DisplayName("(ErrorCode, Throwable) — 应保留 cause 且 message 来自 errorCode")
        void should_errorCodeCauseConstructor_preserveCause() {
            Throwable cause = new RuntimeException("原始异常");
            BaseException ex = new BaseException(errorCode, cause) {};

            assertThat(ex.getErrorCode()).isSameAs(errorCode);
            assertThat(ex.getMessage()).isEqualTo(errorCode.message());
            assertThat(ex.getCause()).isSameAs(cause);
        }

        @Test
        @DisplayName("(ErrorCode, String, Throwable) — message 和 cause 均为自定义")
        void should_errorCodeMessageCauseConstructor_useBoth() {
            String customMsg = "自定义消息";
            Throwable cause = new RuntimeException("原始异常");
            BaseException ex = new BaseException(errorCode, customMsg, cause) {};

            assertThat(ex.getErrorCode()).isSameAs(errorCode);
            assertThat(ex.getMessage()).isEqualTo(customMsg);
            assertThat(ex.getCause()).isSameAs(cause);
        }
    }

    // =========================================================================
    // 4. 异常子类构造器 — 参数化共享测试
    // =========================================================================

    @Nested
    @DisplayName("异常子类构造器")
    class ExceptionSubclassConstructors {

        private static final ErrorCode TEST_EC = CommonErrorCode.FAIL;
        private static final String CUSTOM_MSG = "自定义错误消息";
        private static final Throwable TEST_CAUSE = new RuntimeException("原始异常");

        static Stream<Class<? extends BaseException>> exceptionTypes() {
            return Stream.of(BizException.class, ClientException.class, SysException.class);
        }

        /** 工厂方法：创建指定类型的异常实例（ErrorCode 构造器） */
        static BaseException create(Class<? extends BaseException> type) {
            return switch (type.getSimpleName()) {
                case "BizException" -> new BizException(TEST_EC);
                case "ClientException" -> new ClientException(TEST_EC);
                case "SysException" -> new SysException(TEST_EC);
                default -> throw new IllegalArgumentException("未知类型: " + type);
            };
        }

        /** 工厂方法：创建指定类型的异常实例（ErrorCode + String 构造器） */
        static BaseException createWithMessage(Class<? extends BaseException> type) {
            return switch (type.getSimpleName()) {
                case "BizException" -> new BizException(TEST_EC, CUSTOM_MSG);
                case "ClientException" -> new ClientException(TEST_EC, CUSTOM_MSG);
                case "SysException" -> new SysException(TEST_EC, CUSTOM_MSG);
                default -> throw new IllegalArgumentException("未知类型: " + type);
            };
        }

        /** 工厂方法：创建指定类型的异常实例（ErrorCode + Throwable 构造器） */
        static BaseException createWithCause(Class<? extends BaseException> type) {
            return switch (type.getSimpleName()) {
                case "BizException" -> new BizException(TEST_EC, TEST_CAUSE);
                case "ClientException" -> new ClientException(TEST_EC, TEST_CAUSE);
                case "SysException" -> new SysException(TEST_EC, TEST_CAUSE);
                default -> throw new IllegalArgumentException("未知类型: " + type);
            };
        }

        /** 工厂方法：创建指定类型的异常实例（ErrorCode + String + Throwable 构造器） */
        static BaseException createWithMessageAndCause(Class<? extends BaseException> type) {
            return switch (type.getSimpleName()) {
                case "BizException" -> new BizException(TEST_EC, CUSTOM_MSG, TEST_CAUSE);
                case "ClientException" -> new ClientException(TEST_EC, CUSTOM_MSG, TEST_CAUSE);
                case "SysException" -> new SysException(TEST_EC, CUSTOM_MSG, TEST_CAUSE);
                default -> throw new IllegalArgumentException("未知类型: " + type);
            };
        }

        // --- 构造器(ErrorCode) ---

        @Nested
        @DisplayName("构造器(ErrorCode)")
        class ErrorCodeCtor {

            @ParameterizedTest(name = "{0}")
            @MethodSource("org.smm.archetype.exception.ExceptionClassesUTest$ExceptionSubclassConstructors#exceptionTypes")
            @DisplayName("message 应等于 errorCode.message()")
            void should_messageFromErrorCode(Class<? extends BaseException> type) {
                BaseException ex = create(type);
                assertThat(ex.getMessage()).isEqualTo(TEST_EC.message());
            }

            @ParameterizedTest(name = "{0}")
            @MethodSource("org.smm.archetype.exception.ExceptionClassesUTest$ExceptionSubclassConstructors#exceptionTypes")
            @DisplayName("errorCode 应正确持有")
            void should_holdErrorCode(Class<? extends BaseException> type) {
                BaseException ex = create(type);
                assertThat(ex.getErrorCode()).isSameAs(TEST_EC);
            }
        }

        // --- 构造器(ErrorCode, String) ---

        @Nested
        @DisplayName("构造器(ErrorCode, String)")
        class ErrorCodeMessageCtor {

            @ParameterizedTest(name = "{0}")
            @MethodSource("org.smm.archetype.exception.ExceptionClassesUTest$ExceptionSubclassConstructors#exceptionTypes")
            @DisplayName("message 应为自定义消息")
            void should_useCustomMessage(Class<? extends BaseException> type) {
                BaseException ex = createWithMessage(type);
                assertThat(ex.getMessage()).isEqualTo(CUSTOM_MSG);
            }

            @ParameterizedTest(name = "{0}")
            @MethodSource("org.smm.archetype.exception.ExceptionClassesUTest$ExceptionSubclassConstructors#exceptionTypes")
            @DisplayName("cause 应为 null")
            void should_causeBeNull(Class<? extends BaseException> type) {
                BaseException ex = createWithMessage(type);
                assertThat(ex.getCause()).isNull();
            }
        }

        // --- 构造器(ErrorCode, Throwable) ---

        @Nested
        @DisplayName("构造器(ErrorCode, Throwable)")
        class ErrorCodeCauseCtor {

            @ParameterizedTest(name = "{0}")
            @MethodSource("org.smm.archetype.exception.ExceptionClassesUTest$ExceptionSubclassConstructors#exceptionTypes")
            @DisplayName("应保留 cause")
            void should_preserveCause(Class<? extends BaseException> type) {
                BaseException ex = createWithCause(type);
                assertThat(ex.getCause()).isSameAs(TEST_CAUSE);
            }

            @ParameterizedTest(name = "{0}")
            @MethodSource("org.smm.archetype.exception.ExceptionClassesUTest$ExceptionSubclassConstructors#exceptionTypes")
            @DisplayName("message 应等于 errorCode.message()")
            void should_messageFromErrorCode(Class<? extends BaseException> type) {
                BaseException ex = createWithCause(type);
                assertThat(ex.getMessage()).isEqualTo(TEST_EC.message());
            }
        }

        // --- 构造器(ErrorCode, String, Throwable) ---

        @Nested
        @DisplayName("构造器(ErrorCode, String, Throwable)")
        class ErrorCodeMessageCauseCtor {

            @ParameterizedTest(name = "{0}")
            @MethodSource("org.smm.archetype.exception.ExceptionClassesUTest$ExceptionSubclassConstructors#exceptionTypes")
            @DisplayName("message 和 cause 均为自定义值")
            void should_useCustomMessageAndCause(Class<? extends BaseException> type) {
                BaseException ex = createWithMessageAndCause(type);
                assertThat(ex.getMessage()).isEqualTo(CUSTOM_MSG);
                assertThat(ex.getCause()).isSameAs(TEST_CAUSE);
            }
        }
    }

    // =========================================================================
    // 5. 异常类型继承关系
    // =========================================================================

    @Nested
    @DisplayName("异常类型继承关系")
    class ExceptionHierarchy {

        @Test
        @DisplayName("BizException 继承 BaseException 和 RuntimeException")
        void should_bizException_extend_correctly() {
            BizException ex = new BizException(CommonErrorCode.FAIL);
            assertThat(ex)
                    .isInstanceOf(RuntimeException.class)
                    .isInstanceOf(BaseException.class)
                    .isExactlyInstanceOf(BizException.class);
        }

        @Test
        @DisplayName("ClientException 继承 BaseException 和 RuntimeException")
        void should_clientException_extend_correctly() {
            ClientException ex = new ClientException(CommonErrorCode.FAIL);
            assertThat(ex)
                    .isInstanceOf(RuntimeException.class)
                    .isInstanceOf(BaseException.class)
                    .isExactlyInstanceOf(ClientException.class);
        }

        @Test
        @DisplayName("SysException 继承 BaseException 和 RuntimeException")
        void should_sysException_extend_correctly() {
            SysException ex = new SysException(CommonErrorCode.FAIL);
            assertThat(ex)
                    .isInstanceOf(RuntimeException.class)
                    .isInstanceOf(BaseException.class)
                    .isExactlyInstanceOf(SysException.class);
        }

        @Test
        @DisplayName("BaseException 继承 RuntimeException")
        void should_baseException_extend_runtimeException() {
            BaseException ex = new BaseException(CommonErrorCode.FAIL) {};
            assertThat(ex)
                    .isInstanceOf(RuntimeException.class)
                    .isInstanceOf(BaseException.class);
        }

        @Test
        @DisplayName("子类之间互不兼容")
        void should_subclasses_not_assignable_to_each_other() {
            BizException biz = new BizException(CommonErrorCode.FAIL);
            ClientException client = new ClientException(CommonErrorCode.FAIL);
            SysException sys = new SysException(CommonErrorCode.FAIL);

            assertThat(biz).isNotInstanceOf(ClientException.class)
                    .isNotInstanceOf(SysException.class);
            assertThat(client).isNotInstanceOf(BizException.class)
                    .isNotInstanceOf(SysException.class);
            assertThat(sys).isNotInstanceOf(BizException.class)
                    .isNotInstanceOf(ClientException.class);
        }
    }

    // =========================================================================
    // 6. 边界情况
    // =========================================================================

    @Nested
    @DisplayName("边界情况")
    class EdgeCases {

        private final ErrorCode testEc = CommonErrorCode.FAIL;

        // --- 空消息 ---

        @Test
        @DisplayName("BizException 允许空消息")
        void should_bizException_accept_empty_message() {
            BizException ex = new BizException(testEc, "");
            assertThat(ex.getMessage()).isEmpty();
            assertThat(ex.getErrorCode()).isSameAs(testEc);
        }

        @Test
        @DisplayName("ClientException 允许空消息")
        void should_clientException_accept_empty_message() {
            ClientException ex = new ClientException(testEc, "");
            assertThat(ex.getMessage()).isEmpty();
        }

        @Test
        @DisplayName("SysException 允许空消息")
        void should_sysException_accept_empty_message() {
            SysException ex = new SysException(testEc, "");
            assertThat(ex.getMessage()).isEmpty();
        }

        // --- null cause（4 参数构造器传 null） ---

        @Test
        @DisplayName("BizException 四参构造器传 null cause 不抛异常")
        void should_bizException_accept_null_cause() {
            assertThatNoException().isThrownBy(() ->
                    new BizException(testEc, "msg", null));
            BizException ex = new BizException(testEc, "msg", null);
            assertThat(ex.getCause()).isNull();
        }

        @Test
        @DisplayName("ClientException 四参构造器传 null cause 不抛异常")
        void should_clientException_accept_null_cause() {
            assertThatNoException().isThrownBy(() ->
                    new ClientException(testEc, "msg", null));
            ClientException ex = new ClientException(testEc, "msg", null);
            assertThat(ex.getCause()).isNull();
        }

        @Test
        @DisplayName("SysException 四参构造器传 null cause 不抛异常")
        void should_sysException_accept_null_cause() {
            assertThatNoException().isThrownBy(() ->
                    new SysException(testEc, "msg", null));
            SysException ex = new SysException(testEc, "msg", null);
            assertThat(ex.getCause()).isNull();
        }

        // --- 两参构造器传 null cause ---

        @Test
        @DisplayName("BizException 两参(ErrorCode, Throwable)传 null cause 不抛异常")
        void should_bizException_two_arg_accept_null_cause() {
            assertThatNoException().isThrownBy(() ->
                    new BizException(testEc, (Throwable) null));
            BizException ex = new BizException(testEc, (Throwable) null);
            assertThat(ex.getCause()).isNull();
        }

        @Test
        @DisplayName("ClientException 两参(ErrorCode, Throwable)传 null cause 不抛异常")
        void should_clientException_two_arg_accept_null_cause() {
            assertThatNoException().isThrownBy(() ->
                    new ClientException(testEc, (Throwable) null));
            ClientException ex = new ClientException(testEc, (Throwable) null);
            assertThat(ex.getCause()).isNull();
        }

        @Test
        @DisplayName("SysException 两参(ErrorCode, Throwable)传 null cause 不抛异常")
        void should_sysException_two_arg_accept_null_cause() {
            assertThatNoException().isThrownBy(() ->
                    new SysException(testEc, (Throwable) null));
            SysException ex = new SysException(testEc, (Throwable) null);
            assertThat(ex.getCause()).isNull();
        }

        // --- SUCCESS 错误码 ---

        @Test
        @DisplayName("SUCCESS 错误码可用于异常（虽语义上不推荐）")
        void should_success_error_code_usable_in_exception() {
            BizException ex = new BizException(CommonErrorCode.SUCCESS);
            assertThat(ex.getErrorCode()).isEqualTo(CommonErrorCode.SUCCESS);
            assertThat(ex.getMessage()).isEqualTo("success");
        }

        // --- UNKNOWN_ERROR 错误码 ---

        @Test
        @DisplayName("UNKNOWN_ERROR code 为 9999")
        void should_unknown_error_have_code_9999() {
            assertThat(CommonErrorCode.UNKNOWN_ERROR.code()).isEqualTo(9999);
            assertThat(CommonErrorCode.UNKNOWN_ERROR.message()).isEqualTo("未知异常");
        }
    }
}
