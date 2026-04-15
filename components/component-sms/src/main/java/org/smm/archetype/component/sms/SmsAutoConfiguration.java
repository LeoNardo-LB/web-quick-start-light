package org.smm.archetype.component.sms;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 短信客户端自动配置。
 * <p>
 * 条件：component.sms.enabled=true（默认 false，不自动注册）
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "component.sms", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(SmsProperties.class)
public class SmsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SmsComponent.class)
    public SmsComponent smsClient() {
        return new NoOpSmsComponent();
    }
}
