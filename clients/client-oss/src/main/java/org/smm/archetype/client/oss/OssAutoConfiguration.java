package org.smm.archetype.client.oss;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 对象存储客户端自动配置。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "middleware.object-storage", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OssProperties.class)
public class OssAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(OssClient.class)
    public OssClient ossClient(OssProperties properties) {
        return new LocalOssClient(properties);
    }
}
