package org.smm.archetype.config;

import ch.qos.logback.classic.LoggerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.smm.archetype.config.properties.LoggingProperties;
import org.smm.archetype.shared.aspect.operationlog.LogAspect;
import org.smm.archetype.shared.aspect.operationlog.OperationLogWriter;
import org.smm.archetype.shared.util.logging.SamplingTurboFilter;
import org.smm.archetype.shared.util.logging.SlowQueryInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 日志基础设施配置。
 * <p>
 * 统一管理日志相关的 Bean 注册和启动时日志目录验证。
 * <p>
 * 注册的 Bean：
 * <ul>
 *   <li>LogAspect — @BusinessLog 注解的 AOP 切面</li>
 *   <li>SlowQueryInterceptor — 慢 SQL 监控（需 MyBatis + logging.slow-query.enabled=true）</li>
 *   <li>SamplingTurboFilter — 日志采样（需 logging.sampling.enabled=true）</li>
 * </ul>
 * <p>
 * 启动行为：
 * <ul>
 *   <li>应用启动后自动验证日志目录（创建目录 + 磁盘空间检查）</li>
 * </ul>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@EnableConfigurationProperties(LoggingProperties.class)
public class LoggingConfigure implements ApplicationListener<ApplicationReadyEvent> {

    private static final String DEFAULT_LOG_PATH = ".logs";

    private final Environment environment;

    @Autowired(required = false)
    private OperationLogWriter operationLogWriter;

    @Bean
    public LogAspect logAspect() {
        log.info("[CONFIG] LoggingConfigure: registering LogAspect");
        if (operationLogWriter != null) {
            return new LogAspect(operationLogWriter);
        }
        return new LogAspect();
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

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String logPath = environment.getProperty("logging.file.path", DEFAULT_LOG_PATH);
        Path logDirPath = Paths.get(logPath);
        try {
            if (!Files.exists(logDirPath)) {
                Files.createDirectories(logDirPath);
            }
            if (Files.exists(logDirPath) && Files.isWritable(logDirPath)) {
                long freeSpaceMB = logDirPath.toFile().getFreeSpace() / (1024 * 1024);
                if (freeSpaceMB < 500) {
                    log.warn("[CONFIG] 日志目录磁盘空间不足: {}MB", freeSpaceMB);
                }
                log.info("[CONFIG] 日志配置验证完成 | 路径:{}", logDirPath.toAbsolutePath());
            }
        } catch (Exception e) {
            log.warn("[CONFIG] 日志目录验证失败: {}", e.getMessage());
        }
    }

}
