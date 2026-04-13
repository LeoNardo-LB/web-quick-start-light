package org.smm.archetype.client.log;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.smm.archetype.client.log.logging.LoggingConfiguration;
import org.smm.archetype.client.log.logging.LoggingConfigure;
import org.smm.archetype.client.log.properties.LoggingProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

/**
 * 日志客户端自动配置。
 * <p>
 * 条件：
 * <ul>
 *   <li>classpath 中存在 Micrometer MeterRegistry</li>
 *   <li>classpath 中存在 AspectJ（AOP 支持）</li>
 * </ul>
 * <p>
 * 注册的 Bean：
 * <ul>
 *   <li>LogAspect — @BusinessLog 注解的 AOP 切面</li>
 *   <li>LoggingConfiguration — 日志目录验证</li>
 *   <li>SlowQueryInterceptor — 慢 SQL 监控（需 MyBatis + logging.slow-query.enabled=true）</li>
 *   <li>SamplingTurboFilter — 日志采样（需 logging.sampling.enabled=true）</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnClass({MeterRegistry.class, Aspect.class})
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@EnableConfigurationProperties(LoggingProperties.class)
@Import(LoggingConfigure.class)
@Slf4j
public class LogAutoConfiguration {

    @Bean
    public LogAspect logAspect(MeterRegistry meterRegistry) {
        log.info("[CONFIG] LogAutoConfiguration: registering LogAspect");
        return new LogAspect(meterRegistry);
    }
}
