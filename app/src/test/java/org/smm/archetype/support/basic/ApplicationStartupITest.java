package org.smm.archetype.support.basic;

import org.junit.jupiter.api.Test;
import org.smm.archetype.support.IntegrationTestBase;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationStartupITest extends IntegrationTestBase {

    @Test
    void should_startApplicationSuccessfully_withAllRequiredBeans() {
        // Verify the application context loaded successfully
        assertThat(applicationContext).isNotNull();

        // Verify key beans are present
        assertThat(applicationContext.getBean("contextFillFilter")).isNotNull();
        assertThat(applicationContext.getBean("myMetaObjectHandler")).isNotNull();
        assertThat(applicationContext.getBean("logAspect")).isNotNull();
    }

    @Test
    void should_actuatorHealthEndpointBeReachable() {
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }
}
