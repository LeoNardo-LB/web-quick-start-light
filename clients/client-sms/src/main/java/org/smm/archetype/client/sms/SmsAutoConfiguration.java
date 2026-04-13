package org.smm.archetype.client.sms;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 短信客户端自动配置。
 * <p>
 * 条件：middleware.sms.enabled=true（默认 false，不自动注册）
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "middleware.sms", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(SmsProperties.class)
public class SmsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SmsClient.class)
    public SmsClient smsClient() {
        return new NoOpSmsClient();
    }
}
