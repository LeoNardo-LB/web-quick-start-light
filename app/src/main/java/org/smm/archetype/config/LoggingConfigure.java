package org.smm.archetype.config;

import ch.qos.logback.classic.LoggerContext;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.smm.archetype.config.properties.LoggingProperties;
import org.smm.archetype.shared.aspect.operationlog.LogAspect;
import org.smm.archetype.shared.aspect.operationlog.OperationLogWriter;
import org.smm.archetype.shared.logging.LoggingConfiguration;
import org.smm.archetype.shared.logging.SamplingTurboFilter;
import org.smm.archetype.shared.logging.SlowQueryInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;

/**
 * 日志基础设施配置。
 * <p>
 * 替代 client-log 模块的 LogAutoConfiguration 和 LoggingConfigure，
 * 将日志 AOP 切面和日志基础设施统一注册到 app 模块。
 * <p>
 * 注册的 Bean：
 * <ul>
 *   <li>LogAspect — @BusinessLog 注解的 AOP 切面</li>
 *   <li>LoggingConfiguration — 日志目录验证</li>
 *   <li>SlowQueryInterceptor — 慢 SQL 监控（需 MyBatis + logging.slow-query.enabled=true）</li>
 *   <li>SamplingTurboFilter — 日志采样（需 logging.sampling.enabled=true）</li>
 * </ul>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@EnableConfigurationProperties(LoggingProperties.class)
public class LoggingConfigure {

    private final Environment environment;

    @Autowired(required = false)
    private OperationLogWriter operationLogWriter;

    @Bean
    public LogAspect logAspect(MeterRegistry meterRegistry) {
        log.info("[CONFIG] LoggingConfigure: registering LogAspect");
        if (operationLogWriter != null) {
            return new LogAspect(meterRegistry, operationLogWriter);
        }
        return new LogAspect(meterRegistry);
    }

    @Bean
    public LoggingConfiguration loggingConfiguration() {
        return new LoggingConfiguration(environment);
    }

    @Bean
    @ConditionalOnProperty(name = "logging.slow-query.enabled", havingValue = "true")
    public SlowQueryInterceptor slowQueryInterceptor(LoggingProperties properties) {
        log.info("Enabling slow query logging with threshold: {}ms", properties.getSlowQuery().getThresholdMs());
        return new SlowQueryInterceptor(properties.getSlowQuery());
    }

    @Bean
    @ConditionalOnProperty(name = "logging.sampling.enabled", havingValue = "true")
    public SamplingTurboFilter samplingTurboFilter(LoggingProperties properties) {
        double rate = properties.getSampling().getRate();
        log.info("Enabling log sampling with rate: {} ({}%)", rate, (int) (rate * 100));
        SamplingTurboFilter filter = new SamplingTurboFilter();
        filter.setSampleRate(rate);
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.addTurboFilter(filter);
        return filter;
    }

}
