package org.smm.archetype.config;

import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.client.cache.CacheClient;
import org.smm.archetype.shared.aspect.idempotent.IdempotentAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 幂等防护配置。
 * <p>
 * 注册 IdempotentAspect Bean（注入 CacheClient），启用 CGLIB 代理。
 * <p>
 * 仅当 CacheClient Bean 可用时才注册（CacheClient 由 client-cache 模块提供）。
 */
@Slf4j
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
public class IdempotentConfigure {

    @Bean
    @ConditionalOnBean(CacheClient.class)
    public IdempotentAspect idempotentAspect(CacheClient cacheClient) {
        log.info("[CONFIG] IdempotentConfigure: registering IdempotentAspect");
        return new IdempotentAspect(cacheClient);
    }

}
