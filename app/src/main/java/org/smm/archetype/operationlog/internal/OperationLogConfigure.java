package org.smm.archetype.operationlog.internal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OperationLogConfigure {

    @Bean
    OperationLogConverter operationLogConverter() {
        return new OperationLogConverter();
    }
}
