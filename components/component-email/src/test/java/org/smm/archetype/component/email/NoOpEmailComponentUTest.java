package org.smm.archetype.component.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.component.dto.EmailRequest;
import org.smm.archetype.component.dto.EmailResult;
import org.smm.archetype.component.dto.ServiceProvider;
import org.smm.archetype.exception.ClientException;
import org.smm.archetype.exception.CommonErrorCode;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * NoOpEmailComponent 单元测试。
 * <p>
 * 测试所有公开方法，验证 NoOp 实现的正确行为。
 */
@DisplayName("NoOpEmailComponent 功能测试")
class NoOpEmailComponentUTest {

    private NoOpEmailComponent email;

    @BeforeEach
    void setUp() {
        email = new NoOpEmailComponent();
    }

    // ==================== sendEmail(String, String, String) ====================

    @Nested
    @DisplayName("sendEmail(String, String, String) - 简单邮件发送")
    class SendSimpleEmailTests {

        @Test
        @DisplayName("发送简单邮件应返回成功")
        void should_return_success_for_simple_email() {
            EmailResult result = email.sendEmail("test@example.com", "测试主题", "测试内容");

            assertThat(result.success()).isTrue();
        }

        @Test
        @DisplayName("返回的 messageId 应为有效 UUID 格式")
        void should_return_valid_uuid_message_id() {
            EmailResult result = email.sendEmail("test@example.com", "主题", "内容");

            assertThat(result.messageId()).isNotNull();
            assertThatCode(() -> UUID.fromString(result.messageId())).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("返回的 message 应为'发送成功'")
        void should_return_success_message() {
            EmailResult result = email.sendEmail("test@example.com", "主题", "内容");

            assertThat(result.message()).isEqualTo("发送成功");
        }
    }

    // ==================== sendEmail(ServiceProvider, EmailRequest) ====================

    @Nested
    @DisplayName("sendEmail(ServiceProvider, EmailRequest) - 指定服务商发送")
    class SendEmailWithProviderTests {

        @Test
        @DisplayName("通过 ALIYUN 发送邮件应返回成功")
        void should_return_success_with_aliyun_provider() {
            EmailRequest request = new EmailRequest(
                    List.of("test@example.com"),
                    "template-001",
                    Map.of("name", "Alice"),
                    "测试邮件"
            );

            EmailResult result = email.sendEmail(ServiceProvider.ALIYUN, request);

            assertThat(result.success()).isTrue();
        }

        @Test
        @DisplayName("通过 TENCENT 发送邮件应返回成功")
        void should_return_success_with_tencent_provider() {
            EmailRequest request = new EmailRequest(
                    List.of("test@example.com"),
                    "template-002",
                    Map.of("name", "Bob"),
                    "测试邮件"
            );

            EmailResult result = email.sendEmail(ServiceProvider.TENCENT, request);

            assertThat(result.success()).isTrue();
        }

        @Test
        @DisplayName("通过 LOCAL 发送邮件应返回成功")
        void should_return_success_with_local_provider() {
            EmailRequest request = new EmailRequest(
                    List.of("test@example.com"),
                    "template-003",
                    null,
                    "测试邮件"
            );

            EmailResult result = email.sendEmail(ServiceProvider.LOCAL, request);

            assertThat(result.success()).isTrue();
        }

        @Test
        @DisplayName("每次发送的 messageId 应不同")
        void should_generate_different_message_ids() {
            EmailRequest request = new EmailRequest(
                    List.of("test@example.com"),
                    "template-001",
                    null,
                    "测试邮件"
            );

            EmailResult result1 = email.sendEmail(ServiceProvider.ALIYUN, request);
            EmailResult result2 = email.sendEmail(ServiceProvider.ALIYUN, request);

            assertThat(result1.messageId()).isNotEqualTo(result2.messageId());
        }
    }

    // ==================== sendBatchEmail ====================

    @Nested
    @DisplayName("sendBatchEmail - 批量邮件发送")
    class SendBatchEmailTests {

        @Test
        @DisplayName("批量邮件发送应返回成功")
        void should_return_success_for_batch_email() {
            EmailRequest request = new EmailRequest(
                    List.of("a@example.com", "b@example.com", "c@example.com"),
                    "batch-template",
                    Map.of("title", "通知"),
                    "批量邮件"
            );

            EmailResult result = email.sendBatchEmail(request);

            assertThat(result.success()).isTrue();
        }

        @Test
        @DisplayName("批量邮件返回的 messageId 应为有效 UUID")
        void should_return_valid_uuid_for_batch_email() {
            EmailRequest request = new EmailRequest(
                    List.of("a@example.com"),
                    "batch-template",
                    null,
                    "批量邮件"
            );

            EmailResult result = email.sendBatchEmail(request);

            assertThat(result.messageId()).isNotNull();
            assertThatCode(() -> UUID.fromString(result.messageId())).doesNotThrowAnyException();
        }
    }

    // ==================== 参数校验 ====================

    @Nested
    @DisplayName("参数校验")
    class ValidationTests {

        @Test
        @DisplayName("sendEmail(null, subject, content) 应抛出 ClientException")
        void should_throw_for_null_to() {
            assertThatThrownBy(() -> email.sendEmail(null, "主题", "内容"))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("sendEmail(to, null, content) 应抛出 ClientException")
        void should_throw_for_null_subject() {
            assertThatThrownBy(() -> email.sendEmail("test@example.com", null, "内容"))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("sendEmail(to, subject, null) 应抛出 ClientException")
        void should_throw_for_null_content() {
            assertThatThrownBy(() -> email.sendEmail("test@example.com", "主题", null))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("sendEmail(to, '', content) 应抛出 ClientException")
        void should_throw_for_blank_subject() {
            assertThatThrownBy(() -> email.sendEmail("test@example.com", "", "内容"))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("sendEmail(to, subject, '  ') 应抛出 ClientException")
        void should_throw_for_blank_content() {
            assertThatThrownBy(() -> email.sendEmail("test@example.com", "主题", "  "))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("sendEmail(provider, null) 应抛出 ClientException")
        void should_throw_for_null_email_request() {
            assertThatThrownBy(() -> email.sendEmail(ServiceProvider.ALIYUN, null))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("sendBatchEmail(null) 应抛出 ClientException")
        void should_throw_for_null_batch_request() {
            assertThatThrownBy(() -> email.sendBatchEmail(null))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("sendBatchEmail(to 为 null) 应抛出 ClientException")
        void should_throw_for_null_to_list() {
            EmailRequest request = new EmailRequest(null, "template", null, "主题");

            assertThatThrownBy(() -> email.sendBatchEmail(request))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("sendBatchEmail(to 为空列表) 应抛出 ClientException")
        void should_throw_for_empty_to_list() {
            EmailRequest request = new EmailRequest(List.of(), "template", null, "主题");

            assertThatThrownBy(() -> email.sendBatchEmail(request))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }
    }
}
