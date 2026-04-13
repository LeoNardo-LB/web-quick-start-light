package org.smm.archetype.config;

import org.smm.archetype.config.properties.ThreadPoolProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@EnableConfigurationProperties(ThreadPoolProperties.class)
public class ThreadPoolConfigure {

    @Bean("ioThreadPool")
    public Executor ioThreadPool(ThreadPoolProperties props) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.getIoCoreSize());
        executor.setMaxPoolSize(props.getIoMaxSize());
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("io-pool-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean("cpuThreadPool")
    public Executor cpuThreadPool(ThreadPoolProperties props) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.getCpuCoreSize());
        executor.setMaxPoolSize(props.getCpuCoreSize());
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("cpu-pool-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean("daemonThreadPool")
    public Executor daemonThreadPool(ThreadPoolProperties props) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.getDaemonCoreSize());
        executor.setMaxPoolSize(props.getDaemonCoreSize());
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("daemon-pool-");
        executor.setDaemon(true);
        executor.initialize();
        return executor;
    }

    @Bean("schedulerThreadPool")
    public ScheduledExecutorService schedulerThreadPool(ThreadPoolProperties props) {
        return Executors.newScheduledThreadPool(props.getSchedulerPoolSize(),
                r -> {
                    Thread t = new Thread(r, "scheduler-pool-" + System.currentTimeMillis());
                    t.setDaemon(true);
                    return t;
                });
    }
}
