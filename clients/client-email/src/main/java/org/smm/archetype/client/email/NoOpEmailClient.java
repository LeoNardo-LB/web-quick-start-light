package org.smm.archetype.client.email;

import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.client.dto.EmailRequest;
import org.smm.archetype.client.dto.EmailResult;
import org.smm.archetype.client.dto.ServiceProvider;

import java.util.UUID;

/**
 * 无操作邮件客户端实现。
 * <p>
 * 不调用真实邮件 SDK，仅记录日志并返回成功结果。
 * 适用于开发环境和测试场景。
 */
@Slf4j
public class NoOpEmailClient extends AbstractEmailClient {

    @Override
    protected EmailResult doSendEmail(String to, String subject, String content) {
        log.info("[NoOp] 发送简单邮件: to={}, subject={}", to, subject);
        return EmailResult.success(UUID.randomUUID().toString());
    }

    @Override
    protected EmailResult doSendEmailWithProvider(ServiceProvider provider, EmailRequest request) {
        log.info("[NoOp] 通过 {} 发送邮件: to={}, templateId={}", provider, request.to(), request.templateId());
        return EmailResult.success(UUID.randomUUID().toString());
    }

    @Override
    protected EmailResult doSendBatchEmail(EmailRequest request) {
        log.info("[NoOp] 发送批量邮件: to={}, templateId={}", request.to(), request.templateId());
        return EmailResult.success(UUID.randomUUID().toString());
    }
}
