package org.smm.archetype.client.auth;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 认证客户端自动配置。
 * <p>
 * 条件装配策略：
 * <ul>
 *   <li>middleware.auth.enabled=true（默认 true）时启用</li>
 *   <li>Sa-Token 在 classpath 时注册 SaTokenAuthClient</li>
 *   <li>Sa-Token 不在 classpath 时注册 NoOpAuthClient</li>
 * </ul>
 */
@AutoConfiguration
@EnableConfigurationProperties(AuthProperties.class)
@ConditionalOnProperty(prefix = "middleware.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuthAutoConfiguration {

    /**
     * Sa-Token 在 classpath 时，注册 SaTokenAuthClient。
     */
    @Bean
    @ConditionalOnClass(name = "cn.dev33.satoken.stp.StpUtil")
    @ConditionalOnMissingBean(AuthClient.class)
    public AuthClient saTokenAuthClient() {
        return new SaTokenAuthClient();
    }

    /**
     * 兜底：注册 NoOpAuthClient。
     */
    @Bean
    @ConditionalOnMissingBean(AuthClient.class)
    public AuthClient noOpAuthClient() {
        return new NoOpAuthClient();
    }
}
