package org.smm.archetype.client.sms;

import org.smm.archetype.client.dto.ServiceProvider;
import org.smm.archetype.client.dto.SmsRequest;
import org.smm.archetype.client.dto.SmsResult;

/**
 * 短信客户端接口。
 * <p>
 * 提供模板短信、指定服务商短信和批量短信发送功能。
 */
public interface SmsClient {

    /**
     * 发送模板短信
     *
     * @param request 短信请求
     * @return 发送结果
     */
    SmsResult sendSms(SmsRequest request);

    /**
     * 使用指定服务商发送短信
     *
     * @param provider 服务提供商
     * @param request  短信请求
     * @return 发送结果
     */
    SmsResult sendSms(ServiceProvider provider, SmsRequest request);

    /**
     * 发送批量短信
     *
     * @param request 短信请求（phoneNumber 支持多个手机号）
     * @return 发送结果
     */
    SmsResult sendBatchSms(SmsRequest request);
}
