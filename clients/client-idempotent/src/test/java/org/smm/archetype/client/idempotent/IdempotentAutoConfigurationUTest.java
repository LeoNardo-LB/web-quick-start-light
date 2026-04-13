package org.smm.archetype.client.idempotent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.client.idempotent.properties.IdempotentProperties;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IdempotentAutoConfiguration 条件装配")
class IdempotentAutoConfigurationUTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(IdempotentAutoConfiguration.class));

    @Test
    @DisplayName("enabled=true → IdempotentAspect Bean 存在")
    void should_register_aspect_when_enabled() {
        contextRunner
                .withPropertyValues("middleware.idempotent.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(IdempotentAspect.class);
                    assertThat(context).hasSingleBean(IdempotentProperties.class);
                });
    }

    @Test
    @DisplayName("enabled=false → 无 IdempotentAspect Bean")
    void should_not_register_aspect_when_disabled() {
        contextRunner
                .withPropertyValues("middleware.idempotent.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(IdempotentAspect.class);
                });
    }

    @Test
    @DisplayName("未配置 enabled 属性时默认不注册（显式需开启）")
    void should_not_register_aspect_when_property_not_set() {
        contextRunner
                .run(context -> {
                    assertThat(context).doesNotHaveBean(IdempotentAspect.class);
                });
    }
}
