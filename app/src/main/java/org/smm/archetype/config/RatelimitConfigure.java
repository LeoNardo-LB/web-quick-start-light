package org.smm.archetype.config;

import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.shared.aspect.ratelimit.RateLimitAspect;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 限流配置。
 * <p>
 * 注册 RateLimitAspect Bean，启用 CGLIB 代理。
 */
@Slf4j
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@EnableConfigurationProperties(org.smm.archetype.config.properties.RateLimitProperties.class)
public class RatelimitConfigure {

    @Bean
    public RateLimitAspect rateLimitAspect() {
        log.info("[CONFIG] RatelimitConfigure: registering RateLimitAspect");
        return new RateLimitAspect();
    }

}
