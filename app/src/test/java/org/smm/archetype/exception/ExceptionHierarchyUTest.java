package org.smm.archetype.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.smm.archetype.support.UnitTestBase;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("异常体系层次结构")
class ExceptionHierarchyUTest extends UnitTestBase {

    private static final ErrorCode TEST_ERROR_CODE = CommonErrorCode.FAIL;
    private static final String CUSTOM_MSG = "自定义错误消息";
    private static final Throwable TEST_CAUSE = new RuntimeException("原始异常");

    static Stream<Class<? extends BaseException>> exceptionTypes() {
        return Stream.of(BizException.class, ClientException.class, SysException.class);
    }

    private BaseException newException(Class<? extends BaseException> type) {
        return switch (type.getSimpleName()) {
            case "BizException" -> new BizException(TEST_ERROR_CODE);
            case "ClientException" -> new ClientException(TEST_ERROR_CODE);
            case "SysException" -> new SysException(TEST_ERROR_CODE);
            default -> throw new IllegalArgumentException("未知异常类型: " + type);
        };
    }

    private BaseException newException(Class<? extends BaseException> type, String message) {
        return switch (type.getSimpleName()) {
            case "BizException" -> new BizException(TEST_ERROR_CODE, message);
            case "ClientException" -> new ClientException(TEST_ERROR_CODE, message);
            case "SysException" -> new SysException(TEST_ERROR_CODE, message);
            default -> throw new IllegalArgumentException("未知异常类型: " + type);
        };
    }

    private BaseException newException(Class<? extends BaseException> type, Throwable cause) {
        return switch (type.getSimpleName()) {
            case "BizException" -> new BizException(TEST_ERROR_CODE, cause);
            case "ClientException" -> new ClientException(TEST_ERROR_CODE, cause);
            case "SysException" -> new SysException(TEST_ERROR_CODE, cause);
            default -> throw new IllegalArgumentException("未知异常类型: " + type);
        };
    }

    @Nested
    @DisplayName("构造器(ErrorCode)")
    class ErrorCodeConstructor {

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.smm.archetype.exception.ExceptionHierarchyUTest#exceptionTypes")
        @DisplayName("应持有 ErrorCode 且消息等于 errorCode.message()")
        void should_hold_errorCode_and_match_message(Class<? extends BaseException> exceptionType) {
            BaseException ex = newException(exceptionType);

            assertThat(ex.getMessage()).isEqualTo(TEST_ERROR_CODE.message());
            assertThat(ex.getErrorCode()).isEqualTo(TEST_ERROR_CODE);
        }
    }

    @Nested
    @DisplayName("构造器(ErrorCode, String)")
    class ErrorCodeMessageConstructor {

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.smm.archetype.exception.ExceptionHierarchyUTest#exceptionTypes")
        @DisplayName("消息应为自定义消息")
        void should_use_custom_message(Class<? extends BaseException> exceptionType) {
            BaseException ex = newException(exceptionType, CUSTOM_MSG);

            assertThat(ex.getMessage()).isEqualTo(CUSTOM_MSG);
        }
    }

    @Nested
    @DisplayName("构造器(ErrorCode, Throwable)")
    class ErrorCodeCauseConstructor {

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.smm.archetype.exception.ExceptionHierarchyUTest#exceptionTypes")
        @DisplayName("应保留 cause 且消息等于 errorCode.message()")
        void should_preserve_cause_and_match_message(Class<? extends BaseException> exceptionType) {
            BaseException ex = newException(exceptionType, TEST_CAUSE);

            assertThat(ex.getCause()).isSameAs(TEST_CAUSE);
            assertThat(ex.getMessage()).isEqualTo(TEST_ERROR_CODE.message());
        }
    }

    @Nested
    @DisplayName("类型继承关系")
    class InstanceofChecks {

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.smm.archetype.exception.ExceptionHierarchyUTest#exceptionTypes")
        @DisplayName("应为 RuntimeException 和 BaseException 的实例")
        void should_be_runtime_and_base_exception(Class<? extends BaseException> exceptionType) {
            BaseException ex = newException(exceptionType);

            assertThat(ex).isInstanceOf(RuntimeException.class)
                    .isInstanceOf(BaseException.class)
                    .isExactlyInstanceOf(exceptionType);
        }
    }
}
