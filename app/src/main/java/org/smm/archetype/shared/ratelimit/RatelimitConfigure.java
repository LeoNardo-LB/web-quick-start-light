package org.smm.archetype.shared.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 限流配置。
 * <p>
 * RateLimitAspect 由 @Component 自动注册，此处仅启用 CGLIB 代理和配置属性。
 */
@Slf4j
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@EnableConfigurationProperties(RateLimitProperties.class)
public class RatelimitConfigure {

}
