package org.smm.archetype.client.email;

import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.client.dto.EmailRequest;
import org.smm.archetype.client.dto.EmailResult;
import org.smm.archetype.client.dto.ServiceProvider;
import org.smm.archetype.exception.ClientException;
import org.smm.archetype.exception.CommonErrorCode;

/**
 * EmailClient 抽象基类，使用 Template Method 模式。
 * <p>
 * 所有公开方法标记为 final，完成参数校验、异常处理与日志记录。
 * 子类实现 do* 扩展点完成具体邮件发送操作。
 */
@Slf4j
public abstract class AbstractEmailClient implements EmailClient {

    @Override
    public final EmailResult sendEmail(String to, String subject, String content) {
        if (to == null || to.isBlank()) {
            throw new ClientException(CommonErrorCode.ILLEGAL_ARGUMENT, "收件人不能为空");
        }
        if (subject == null || subject.isBlank()) {
            throw new ClientException(CommonErrorCode.ILLEGAL_ARGUMENT, "邮件主题不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new ClientException(CommonErrorCode.ILLEGAL_ARGUMENT, "邮件内容不能为空");
        }
        log.info("开始发送简单邮件: to={}, subject={}", to, subject);
        try {
            EmailResult result = doSendEmail(to, subject, content);
            log.info("简单邮件发送完成: success={}, messageId={}", result.success(), result.messageId());
            return result;
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("简单邮件发送异常: to={}, subject={}", to, subject, e);
            throw new ClientException(CommonErrorCode.EMAIL_SEND_FAILED, "邮件发送失败", e);
        }
    }

    @Override
    public final EmailResult sendEmail(ServiceProvider provider, EmailRequest request) {
        validateEmailRequest(request);
        log.info("开始发送邮件(provider={}): to={}, templateId={}", provider, request.to(), request.templateId());
        try {
            EmailResult result = doSendEmailWithProvider(provider, request);
            log.info("邮件发送完成: success={}, messageId={}", result.success(), result.messageId());
            return result;
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("邮件发送异常: provider={}", provider, e);
            throw new ClientException(CommonErrorCode.EMAIL_SEND_FAILED, "邮件发送失败", e);
        }
    }

    @Override
    public final EmailResult sendBatchEmail(EmailRequest request) {
        validateEmailRequest(request);
        if (request.to() == null || request.to().isEmpty()) {
            throw new ClientException(CommonErrorCode.ILLEGAL_ARGUMENT, "收件人列表不能为空");
        }
        log.info("开始发送批量邮件: to={}, templateId={}", request.to(), request.templateId());
        try {
            EmailResult result = doSendBatchEmail(request);
            log.info("批量邮件发送完成: success={}, messageId={}", result.success(), result.messageId());
            return result;
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量邮件发送异常", e);
            throw new ClientException(CommonErrorCode.EMAIL_SEND_FAILED, "批量邮件发送失败", e);
        }
    }

    // ==================== 参数校验 ====================

    private void validateEmailRequest(EmailRequest request) {
        if (request == null) {
            throw new ClientException(CommonErrorCode.ILLEGAL_ARGUMENT, "email request 不能为空");
        }
    }

    // ==================== 子类扩展点 ====================

    protected abstract EmailResult doSendEmail(String to, String subject, String content);

    protected abstract EmailResult doSendEmailWithProvider(ServiceProvider provider, EmailRequest request);

    protected abstract EmailResult doSendBatchEmail(EmailRequest request);
}
