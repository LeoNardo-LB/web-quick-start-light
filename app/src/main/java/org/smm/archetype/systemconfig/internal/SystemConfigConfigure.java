package org.smm.archetype.systemconfig.internal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SystemConfigConfigure {

    @Bean
    SystemConfigConverter systemConfigConverter() {
        return new SystemConfigConverter();
    }
}
