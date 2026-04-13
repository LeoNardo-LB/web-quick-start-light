package org.smm.archetype.client.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 限流客户端自动配置。
 * <p>
 * 条件：
 * <ul>
 *   <li>classpath 中存在 Bucket4j（{@code io.github.bucket4j.Bucket}）</li>
 *   <li>classpath 中存在 AspectJ（AOP 支持）</li>
 *   <li>配置 {@code middleware.ratelimit.enabled=true}（默认 true）</li>
 * </ul>
 * <p>
 * 注册的 Bean：
 * <ul>
 *   <li>RateLimitAspect — {@link RateLimit} 注解的 AOP 切面</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnClass({io.github.bucket4j.Bucket.class, Aspect.class})
@ConditionalOnProperty(prefix = "middleware.ratelimit", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@EnableConfigurationProperties(RateLimitProperties.class)
@Slf4j
public class RateLimitAutoConfiguration {

    @Bean
    public RateLimitAspect rateLimitAspect() {
        log.info("[CONFIG] RateLimitAutoConfiguration: registering RateLimitAspect");
        return new RateLimitAspect();
    }
}
