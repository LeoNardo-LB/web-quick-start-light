package org.smm.archetype.client.email;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 邮件客户端自动配置。
 * <p>
 * 条件：middleware.email.enabled=true（默认 false，不自动注册）
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "middleware.email", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(EmailProperties.class)
public class EmailAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(EmailClient.class)
    public EmailClient emailClient() {
        return new NoOpEmailClient();
    }
}
