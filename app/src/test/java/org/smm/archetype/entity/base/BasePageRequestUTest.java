package org.smm.archetype.entity.base;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.UnitTestBase;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BasePageRequest 基础分页请求")
class BasePageRequestUTest extends UnitTestBase {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Nested
    @DisplayName("默认值")
    class DefaultValues {

        @Test
        @DisplayName("pageNo 默认值应为 1")
        void should_have_default_pageNo_1() {
            // given
            BasePageRequest request = new BasePageRequest();

            // then
            assertThat(request.getPageNo()).isEqualTo(1);
        }

        @Test
        @DisplayName("pageSize 默认值应为 20")
        void should_have_default_pageSize_20() {
            // given
            BasePageRequest request = new BasePageRequest();

            // then
            assertThat(request.getPageSize()).isEqualTo(20);
        }

        @Test
        @DisplayName("继承的 requestId 和 traceId 默认值应为 null")
        void should_have_null_inherited_defaults() {
            // given
            BasePageRequest request = new BasePageRequest();

            // then
            assertThat(request.getRequestId()).isNull();
            assertThat(request.getTraceId()).isNull();
        }
    }

    @Nested
    @DisplayName("getter/setter")
    class GetterSetter {

        @Test
        @DisplayName("pageNo 应正确读写")
        void should_read_write_pageNo() {
            // given
            BasePageRequest request = new BasePageRequest();

            // when
            request.setPageNo(5);

            // then
            assertThat(request.getPageNo()).isEqualTo(5);
        }

        @Test
        @DisplayName("pageSize 应正确读写")
        void should_read_write_pageSize() {
            // given
            BasePageRequest request = new BasePageRequest();

            // when
            request.setPageSize(50);

            // then
            assertThat(request.getPageSize()).isEqualTo(50);
        }

        @Test
        @DisplayName("继承的 requestId 应正确读写")
        void should_read_write_requestId() {
            // given
            BasePageRequest request = new BasePageRequest();

            // when
            request.setRequestId("req-page-001");

            // then
            assertThat(request.getRequestId()).isEqualTo("req-page-001");
        }

        @Test
        @DisplayName("继承的 traceId 应正确读写")
        void should_read_write_traceId() {
            // given
            BasePageRequest request = new BasePageRequest();

            // when
            request.setTraceId("trace-page-001");

            // then
            assertThat(request.getTraceId()).isEqualTo("trace-page-001");
        }
    }

    @Nested
    @DisplayName("Bean Validation")
    class BeanValidation {

        @Test
        @DisplayName("默认值应通过校验")
        void should_pass_validation_with_defaults() {
            // given
            BasePageRequest request = new BasePageRequest();

            // when
            Set<jakarta.validation.ConstraintViolation<BasePageRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("pageNo=0 应校验失败（@Min(1)）")
        void should_fail_when_pageNo_is_zero() {
            // given
            BasePageRequest request = new BasePageRequest();
            request.setPageNo(0);

            // when
            Set<jakarta.validation.ConstraintViolation<BasePageRequest>> violations = validator.validate(request);

            // then
            assertThat(violations)
                    .anyMatch(v -> v.getPropertyPath().toString().equals("pageNo"));
        }

        @Test
        @DisplayName("pageNo=-1 应校验失败（@Min(1)）")
        void should_fail_when_pageNo_is_negative() {
            // given
            BasePageRequest request = new BasePageRequest();
            request.setPageNo(-1);

            // when
            Set<jakarta.validation.ConstraintViolation<BasePageRequest>> violations = validator.validate(request);

            // then
            assertThat(violations)
                    .anyMatch(v -> v.getPropertyPath().toString().equals("pageNo"));
        }

        @Test
        @DisplayName("pageSize=0 应校验失败（@Min(1)）")
        void should_fail_when_pageSize_is_zero() {
            // given
            BasePageRequest request = new BasePageRequest();
            request.setPageSize(0);

            // when
            Set<jakarta.validation.ConstraintViolation<BasePageRequest>> violations = validator.validate(request);

            // then
            assertThat(violations)
                    .anyMatch(v -> v.getPropertyPath().toString().equals("pageSize"));
        }

        @Test
        @DisplayName("pageSize=101 应校验失败（@Max(100)）")
        void should_fail_when_pageSize_exceeds_max() {
            // given
            BasePageRequest request = new BasePageRequest();
            request.setPageSize(101);

            // when
            Set<jakarta.validation.ConstraintViolation<BasePageRequest>> violations = validator.validate(request);

            // then
            assertThat(violations)
                    .anyMatch(v -> v.getPropertyPath().toString().equals("pageSize"));
        }

        @Test
        @DisplayName("pageSize=100 应通过校验（边界值）")
        void should_pass_when_pageSize_is_max() {
            // given
            BasePageRequest request = new BasePageRequest();
            request.setPageSize(100);

            // when
            Set<jakarta.validation.ConstraintViolation<BasePageRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("pageNo=1、pageSize=1 应通过校验（最小边界值）")
        void should_pass_when_min_boundary() {
            // given
            BasePageRequest request = new BasePageRequest();
            request.setPageNo(1);
            request.setPageSize(1);

            // when
            Set<jakarta.validation.ConstraintViolation<BasePageRequest>> violations = validator.validate(request);

            // then
            assertThat(violations).isEmpty();
        }
    }
}
