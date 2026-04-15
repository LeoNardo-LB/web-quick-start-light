package org.smm.archetype.component.sms;

import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.component.dto.ServiceProvider;
import org.smm.archetype.component.dto.SmsRequest;
import org.smm.archetype.component.dto.SmsResult;

import java.util.UUID;

/**
 * 无操作短信客户端实现。
 * <p>
 * 不调用真实短信 SDK，仅记录日志并返回成功结果。
 * 适用于开发环境和测试场景。
 */
@Slf4j
public class NoOpSmsComponent extends AbstractSmsComponent {

    @Override
    protected SmsResult doSendSms(SmsRequest request) {
        log.info("[NoOp] 发送短信: phoneNumber={}, templateId={}", request.phoneNumber(), request.templateId());
        return SmsResult.success(UUID.randomUUID().toString());
    }

    @Override
    protected SmsResult doSendSmsWithProvider(ServiceProvider provider, SmsRequest request) {
        log.info("[NoOp] 通过 {} 发送短信: phoneNumber={}, templateId={}", provider, request.phoneNumber(), request.templateId());
        return SmsResult.success(UUID.randomUUID().toString());
    }

    @Override
    protected SmsResult doSendBatchSms(SmsRequest request) {
        log.info("[NoOp] 发送批量短信: phoneNumber={}, templateId={}", request.phoneNumber(), request.templateId());
        return SmsResult.success(UUID.randomUUID().toString());
    }
}
