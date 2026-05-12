package org.smm.archetype.auth.internal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AuthConfigure {

    @Bean
    UserConverter userConverter() {
        return new UserConverter();
    }
}
