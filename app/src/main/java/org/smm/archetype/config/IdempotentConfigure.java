package org.smm.archetype.config;

import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.component.cache.CacheComponent;
import org.smm.archetype.shared.aspect.idempotent.IdempotentAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 幂等防护配置。
 * <p>
 * 注册 IdempotentAspect Bean（注入 CacheComponent），启用 CGLIB 代理。
 * <p>
 * 仅当 CacheComponent Bean 可用时才注册（CacheComponent 由 component-cache 模块提供）。
 */
@Slf4j
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
public class IdempotentConfigure {

    @Bean
    @ConditionalOnBean(CacheComponent.class)
    public IdempotentAspect idempotentAspect(CacheComponent cacheComponent) {
        log.info("[CONFIG] IdempotentConfigure: registering IdempotentAspect");
        return new IdempotentAspect(cacheComponent);
    }

}
