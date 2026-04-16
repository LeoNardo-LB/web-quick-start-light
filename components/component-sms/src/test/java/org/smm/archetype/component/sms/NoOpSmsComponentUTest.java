package org.smm.archetype.component.sms;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.component.dto.ServiceProvider;
import org.smm.archetype.component.dto.SmsRequest;
import org.smm.archetype.component.dto.SmsResult;
import org.smm.archetype.exception.ClientException;
import org.smm.archetype.exception.CommonErrorCode;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * NoOpSmsComponent 单元测试。
 * <p>
 * 测试所有公开方法，验证 NoOp 实现的正确行为。
 */
@DisplayName("NoOpSmsComponent 功能测试")
class NoOpSmsComponentUTest {

    private NoOpSmsComponent sms;

    @BeforeEach
    void setUp() {
        sms = new NoOpSmsComponent();
    }

    // ==================== sendSms(SmsRequest) ====================

    @Nested
    @DisplayName("sendSms(SmsRequest) - 模板短信发送")
    class SendSmsTests {

        @Test
        @DisplayName("发送模板短信应返回成功")
        void should_return_success_for_template_sms() {
            SmsRequest request = new SmsRequest("13800138000", "template-001", Map.of("code", "123456"));

            SmsResult result = sms.sendSms(request);

            assertThat(result.success()).isTrue();
        }

        @Test
        @DisplayName("返回的 requestId 应为有效 UUID 格式")
        void should_return_valid_uuid_request_id() {
            SmsRequest request = new SmsRequest("13800138000", "template-001", null);

            SmsResult result = sms.sendSms(request);

            assertThat(result.requestId()).isNotNull();
            assertThatCode(() -> UUID.fromString(result.requestId())).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("返回的 message 应为'发送成功'")
        void should_return_success_message() {
            SmsRequest request = new SmsRequest("13800138000", "template-001", null);

            SmsResult result = sms.sendSms(request);

            assertThat(result.message()).isEqualTo("发送成功");
        }

        @Test
        @DisplayName("每次发送的 requestId 应不同")
        void should_generate_different_request_ids() {
            SmsRequest request = new SmsRequest("13800138000", "template-001", null);

            SmsResult result1 = sms.sendSms(request);
            SmsResult result2 = sms.sendSms(request);

            assertThat(result1.requestId()).isNotEqualTo(result2.requestId());
        }
    }

    // ==================== sendSms(ServiceProvider, SmsRequest) ====================

    @Nested
    @DisplayName("sendSms(ServiceProvider, SmsRequest) - 指定服务商发送")
    class SendSmsWithProviderTests {

        @Test
        @DisplayName("通过 ALIYUN 发送短信应返回成功")
        void should_return_success_with_aliyun_provider() {
            SmsRequest request = new SmsRequest("13800138000", "template-001", Map.of("code", "654321"));

            SmsResult result = sms.sendSms(ServiceProvider.ALIYUN, request);

            assertThat(result.success()).isTrue();
        }

        @Test
        @DisplayName("通过 TENCENT 发送短信应返回成功")
        void should_return_success_with_tencent_provider() {
            SmsRequest request = new SmsRequest("13800138000", "template-002", null);

            SmsResult result = sms.sendSms(ServiceProvider.TENCENT, request);

            assertThat(result.success()).isTrue();
        }

        @Test
        @DisplayName("通过 LOCAL 发送短信应返回成功")
        void should_return_success_with_local_provider() {
            SmsRequest request = new SmsRequest("13800138000", "template-003", null);

            SmsResult result = sms.sendSms(ServiceProvider.LOCAL, request);

            assertThat(result.success()).isTrue();
        }

        @Test
        @DisplayName("通过 CUSTOM 发送短信应返回成功")
        void should_return_success_with_custom_provider() {
            SmsRequest request = new SmsRequest("13800138000", "template-004", null);

            SmsResult result = sms.sendSms(ServiceProvider.CUSTOM, request);

            assertThat(result.success()).isTrue();
        }

        @Test
        @DisplayName("不同服务商发送的 requestId 应不同")
        void should_generate_different_request_ids_for_different_providers() {
            SmsRequest request = new SmsRequest("13800138000", "template-001", null);

            SmsResult result1 = sms.sendSms(ServiceProvider.ALIYUN, request);
            SmsResult result2 = sms.sendSms(ServiceProvider.TENCENT, request);

            assertThat(result1.requestId()).isNotEqualTo(result2.requestId());
        }
    }

    // ==================== sendBatchSms ====================

    @Nested
    @DisplayName("sendBatchSms - 批量短信发送")
    class SendBatchSmsTests {

        @Test
        @DisplayName("批量短信发送应返回成功")
        void should_return_success_for_batch_sms() {
            SmsRequest request = new SmsRequest(
                    "13800138000,13900139000",
                    "batch-template",
                    Map.of("title", "通知")
            );

            SmsResult result = sms.sendBatchSms(request);

            assertThat(result.success()).isTrue();
        }

        @Test
        @DisplayName("批量短信返回的 requestId 应为有效 UUID")
        void should_return_valid_uuid_for_batch_sms() {
            SmsRequest request = new SmsRequest("13800138000", "batch-template", null);

            SmsResult result = sms.sendBatchSms(request);

            assertThat(result.requestId()).isNotNull();
            assertThatCode(() -> UUID.fromString(result.requestId())).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("批量短信返回的 message 应为'发送成功'")
        void should_return_success_message_for_batch_sms() {
            SmsRequest request = new SmsRequest("13800138000", "batch-template", null);

            SmsResult result = sms.sendBatchSms(request);

            assertThat(result.message()).isEqualTo("发送成功");
        }
    }

    // ==================== 参数校验 ====================

    @Nested
    @DisplayName("参数校验")
    class ValidationTests {

        @Test
        @DisplayName("sendSms(null) 应抛出 ClientException")
        void should_throw_for_null_sms_request() {
            assertThatThrownBy(() -> sms.sendSms(null))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("sendSms(phoneNumber=null) 应抛出 ClientException")
        void should_throw_for_null_phone_number() {
            SmsRequest request = new SmsRequest(null, "template-001", null);

            assertThatThrownBy(() -> sms.sendSms(request))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("sendSms(phoneNumber='') 应抛出 ClientException")
        void should_throw_for_empty_phone_number() {
            SmsRequest request = new SmsRequest("", "template-001", null);

            assertThatThrownBy(() -> sms.sendSms(request))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("sendSms(phoneNumber='  ') 应抛出 ClientException")
        void should_throw_for_blank_phone_number() {
            SmsRequest request = new SmsRequest("  ", "template-001", null);

            assertThatThrownBy(() -> sms.sendSms(request))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("sendSms(null provider, request) 应抛出 ClientException")
        void should_throw_for_null_provider() {
            SmsRequest request = new SmsRequest("13800138000", "template-001", null);

            assertThatThrownBy(() -> sms.sendSms(null, request))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("sendSms(provider, null request) 应抛出 ClientException")
        void should_throw_for_null_request_with_provider() {
            assertThatThrownBy(() -> sms.sendSms(ServiceProvider.ALIYUN, null))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("sendBatchSms(null) 应抛出 ClientException")
        void should_throw_for_null_batch_request() {
            assertThatThrownBy(() -> sms.sendBatchSms(null))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("sendBatchSms(phoneNumber=null) 应抛出 ClientException")
        void should_throw_for_null_phone_number_in_batch() {
            SmsRequest request = new SmsRequest(null, "batch-template", null);

            assertThatThrownBy(() -> sms.sendBatchSms(request))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }
    }
}
