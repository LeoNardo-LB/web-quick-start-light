package org.smm.archetype.client.log.logging;

import ch.qos.logback.classic.LoggerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.client.log.properties.LoggingProperties;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(LoggingProperties.class)
public class LoggingConfigure {

    private final Environment environment;

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
        log.info("Enabling log sampling with rate: {} ({}%)", rate, (int)(rate * 100));
        SamplingTurboFilter filter = new SamplingTurboFilter();
        filter.setSampleRate(rate);
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.addTurboFilter(filter);
        return filter;
    }
}
