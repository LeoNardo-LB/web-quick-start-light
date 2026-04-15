package org.smm.archetype.component.auth;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 认证组件自动配置。
 * <p>
 * 条件装配策略：
 * <ul>
 *   <li>component.auth.enabled=true（默认 true）时启用</li>
 *   <li>Sa-Token 在 classpath 时注册 SaTokenAuthComponent</li>
 *   <li>Sa-Token 不在 classpath 时注册 NoOpAuthComponent</li>
 * </ul>
 */
@AutoConfiguration
@EnableConfigurationProperties(AuthProperties.class)
@ConditionalOnProperty(prefix = "component.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuthAutoConfiguration {

    /**
     * Sa-Token 在 classpath 时，注册 SaTokenAuthComponent。
     */
    @Bean
    @ConditionalOnClass(name = "cn.dev33.satoken.stp.StpUtil")
    @ConditionalOnMissingBean(AuthComponent.class)
    public AuthComponent saTokenAuthComponent() {
        return new SaTokenAuthComponent();
    }

    /**
     * 兜底：注册 NoOpAuthComponent。
     */
    @Bean
    @ConditionalOnMissingBean(AuthComponent.class)
    public AuthComponent noOpAuthComponent() {
        return new NoOpAuthComponent();
    }
}
