package org.smm.archetype.client.sms;

import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.client.dto.ServiceProvider;
import org.smm.archetype.client.dto.SmsRequest;
import org.smm.archetype.client.dto.SmsResult;
import org.smm.archetype.exception.ClientException;
import org.smm.archetype.exception.CommonErrorCode;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * SmsClient 抽象基类，使用 Template Method 模式。
 * <p>
 * 所有公开方法标记为 final，完成参数校验、异常处理与日志记录。
 * 子类实现 do* 扩展点完成具体短信发送操作。
 */
@Slf4j
public abstract class AbstractSmsClient implements SmsClient {

    @Override
    public final SmsResult sendSms(SmsRequest request) {
        validateSmsRequest(request);
        log.info("发送模板短信, phoneNumber={}, templateId={}", request.phoneNumber(), request.templateId());
        try {
            SmsResult result = doSendSms(request);
            log.info("发送模板短信完成, phoneNumber={}, success={}", request.phoneNumber(), result.success());
            return result;
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("发送模板短信异常, phoneNumber={}", request.phoneNumber(), e);
            throw new ClientException(CommonErrorCode.SMS_SEND_FAILED, e.getMessage(), e);
        }
    }

    @Override
    public final SmsResult sendSms(ServiceProvider provider, SmsRequest request) {
        validateSmsRequest(request);
        if (provider == null) {
            throw new ClientException(CommonErrorCode.ILLEGAL_ARGUMENT, "短信服务商不能为空");
        }
        log.info("通过 {} 发送短信, phoneNumber={}, templateId={}", provider, request.phoneNumber(), request.templateId());
        try {
            SmsResult result = doSendSmsWithProvider(provider, request);
            log.info("发送短信完成, phoneNumber={}, success={}", request.phoneNumber(), result.success());
            return result;
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("发送短信异常, provider={}, phoneNumber={}", provider, request.phoneNumber(), e);
            throw new ClientException(CommonErrorCode.SMS_SEND_FAILED, e.getMessage(), e);
        }
    }

    @Override
    public final SmsResult sendBatchSms(SmsRequest request) {
        validateSmsRequest(request);
        log.info("发送批量短信, phoneNumber={}, templateId={}", request.phoneNumber(), request.templateId());
        try {
            SmsResult result = doSendBatchSms(request);
            log.info("发送批量短信完成, success={}", result.success());
            return result;
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("发送批量短信异常", e);
            throw new ClientException(CommonErrorCode.SMS_SEND_FAILED, e.getMessage(), e);
        }
    }

    // ==================== 参数校验 ====================

    private void validateSmsRequest(SmsRequest request) {
        if (request == null || !StringUtils.hasText(request.phoneNumber())) {
            throw new ClientException(CommonErrorCode.ILLEGAL_ARGUMENT, "短信请求参数不合法");
        }
    }

    // ==================== 子类扩展点 ====================

    protected abstract SmsResult doSendSms(SmsRequest request);

    protected abstract SmsResult doSendSmsWithProvider(ServiceProvider provider, SmsRequest request);

    protected abstract SmsResult doSendBatchSms(SmsRequest request);
}
