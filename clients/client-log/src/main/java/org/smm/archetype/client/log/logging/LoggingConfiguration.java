package org.smm.archetype.client.log.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class LoggingConfiguration implements ApplicationListener<ApplicationReadyEvent> {
    private static final String DEFAULT_LOG_PATH = ".logs";
    private final Environment environment;

    public LoggingConfiguration(Environment environment) {
        this.environment = environment;
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
