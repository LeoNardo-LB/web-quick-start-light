package org.smm.archetype.client.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimitAutoConfiguration 条件装配")
class RateLimitAutoConfigurationUTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RateLimitAutoConfiguration.class));

    @Test
    @DisplayName("Bucket4j 在 classpath 且 enabled=true → RateLimitAspect Bean 存在")
    void should_create_aspect_when_bucket4j_and_enabled() {
        contextRunner
                .withPropertyValues("middleware.ratelimit.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(RateLimitAspect.class);
                });
    }

    @Test
    @DisplayName("enabled 未设置（默认 true）→ RateLimitAspect Bean 存在")
    void should_create_aspect_when_enabled_default() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasSingleBean(RateLimitAspect.class);
                });
    }

    @Test
    @DisplayName("enabled=false → 无 RateLimitAspect Bean")
    void should_not_create_aspect_when_disabled() {
        contextRunner
                .withPropertyValues("middleware.ratelimit.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(RateLimitAspect.class);
                });
    }

    @Test
    @DisplayName("RateLimitProperties 应正确绑定")
    void should_bind_properties() {
        contextRunner
                .withPropertyValues(
                        "middleware.ratelimit.enabled=true",
                        "middleware.ratelimit.default-capacity=100",
                        "middleware.ratelimit.default-refill-tokens=50"
                )
                .run(context -> {
                    RateLimitProperties props = context.getBean(RateLimitProperties.class);
                    assertThat(props.isEnabled()).isTrue();
                    assertThat(props.getDefaultCapacity()).isEqualTo(100);
                    assertThat(props.getDefaultRefillTokens()).isEqualTo(50);
                });
    }
}
