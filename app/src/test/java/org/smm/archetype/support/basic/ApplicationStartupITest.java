package org.smm.archetype.support.basic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.shared.aspect.operationlog.LogAspect;
import org.smm.archetype.shared.util.dal.MyMetaObjectHandler;
import org.smm.archetype.support.IntegrationTestBase;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.smm.archetype.controller.global.ContextFillFilter;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationStartupITest extends IntegrationTestBase {

    @Test
    @DisplayName("应用启动成功且关键 Bean 类型正确")
    void should_startApplicationSuccessfully_withAllRequiredBeans() {
        // Verify the application context loaded successfully
        assertThat(applicationContext).isNotNull();

        // Verify ContextFillFilter — registered as FilterRegistrationBean, extract inner filter
        @SuppressWarnings("unchecked")
        FilterRegistrationBean<ContextFillFilter> filterRegistration =
                (FilterRegistrationBean<ContextFillFilter>) applicationContext.getBean("contextFillFilter");
        assertThat(filterRegistration).isNotNull();
        assertThat(filterRegistration.getFilter()).isInstanceOf(ContextFillFilter.class);

        // Verify MyMetaObjectHandler — direct @Component bean
        assertThat(applicationContext.getBean(MyMetaObjectHandler.class))
                .isNotNull()
                .isInstanceOf(MyMetaObjectHandler.class);

        // Verify LogAspect — @Bean registered in LoggingConfigure
        assertThat(applicationContext.getBean(LogAspect.class))
                .isNotNull()
                .isInstanceOf(LogAspect.class);
    }

    @Test
    @DisplayName("Actuator 健康检查端点可达")
    void should_actuatorHealthEndpointBeReachable() {
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }
}
