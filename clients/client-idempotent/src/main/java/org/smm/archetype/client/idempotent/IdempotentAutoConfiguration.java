package org.smm.archetype.client.idempotent;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.client.idempotent.properties.IdempotentProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 幂等防护自动配置。
 * <p>
 * 条件：
 * <ul>
 *   <li>middleware.idempotent.enabled=true</li>
 *   <li>classpath 中存在 Caffeine 和 AspectJ</li>
 * </ul>
 * <p>
 * 注册的 Bean：
 * <ul>
 *   <li>IdempotentAspect — @Idempotent 注解的 AOP 切面</li>
 * </ul>
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass({Caffeine.class, org.aspectj.lang.annotation.Aspect.class})
@ConditionalOnProperty(prefix = "middleware.idempotent", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(IdempotentProperties.class)
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
public class IdempotentAutoConfiguration {

    @Bean
    public IdempotentAspect idempotentAspect(IdempotentProperties properties) {
        log.info("[CONFIG] IdempotentAutoConfiguration: registering IdempotentAspect");
        // 使用带 expireAfterWrite 的 Caffeine Cache，最大容量 10000
        // expireAfterWrite 作为兜底清理机制，实际过期由切面内的时间戳检查控制
        Cache<String, Long> cache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(10, java.util.concurrent.TimeUnit.MINUTES)
                .build();
        return new IdempotentAspect(cache);
    }
}
