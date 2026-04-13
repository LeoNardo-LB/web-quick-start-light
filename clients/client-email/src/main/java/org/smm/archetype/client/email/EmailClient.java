package org.smm.archetype.client.email;

import org.smm.archetype.client.dto.EmailRequest;
import org.smm.archetype.client.dto.EmailResult;
import org.smm.archetype.client.dto.ServiceProvider;

/**
 * 邮件客户端接口。
 * <p>
 * 提供简单邮件、模板邮件和批量邮件发送功能。
 */
public interface EmailClient {

    /**
     * 发送简单邮件
     *
     * @param to      收件人
     * @param subject 主题
     * @param content 内容
     * @return 发送结果
     */
    EmailResult sendEmail(String to, String subject, String content);

    /**
     * 使用指定服务商发送邮件
     *
     * @param provider 服务提供商
     * @param request  邮件请求
     * @return 发送结果
     */
    EmailResult sendEmail(ServiceProvider provider, EmailRequest request);

    /**
     * 发送模板邮件
     *
     * @param request 邮件请求
     * @return 发送结果
     */
    EmailResult sendBatchEmail(EmailRequest request);
}
