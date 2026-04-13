package org.smm.archetype.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "thread-pool")
public class ThreadPoolProperties {
    private int ioCoreSize = 4;
    private int ioMaxSize = 8;
    private int cpuCoreSize = 3;
    private int daemonCoreSize = 2;
    private int schedulerPoolSize = 2;
}
