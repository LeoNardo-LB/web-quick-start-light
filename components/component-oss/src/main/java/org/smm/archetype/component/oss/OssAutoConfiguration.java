package org.smm.archetype.component.oss;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 对象存储客户端自动配置。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "component.oss", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OssProperties.class)
public class OssAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(OssComponent.class)
    public OssComponent ossClient(OssProperties properties) {
        return new LocalOssComponent(properties);
    }
}
