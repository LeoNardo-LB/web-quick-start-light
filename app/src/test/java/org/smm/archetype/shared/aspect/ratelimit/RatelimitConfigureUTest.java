package org.smm.archetype.shared.aspect.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.config.RatelimitConfigure;
import org.smm.archetype.config.properties.RateLimitProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RatelimitConfigure 配置装配")
class RatelimitConfigureUTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                                                                   .withUserConfiguration(RatelimitConfigure.class);

    @Test
    @DisplayName("RatelimitConfigure 加载成功 → RateLimitAspect Bean 存在")
    void should_create_aspect_when_configured() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasSingleBean(RateLimitAspect.class);
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
