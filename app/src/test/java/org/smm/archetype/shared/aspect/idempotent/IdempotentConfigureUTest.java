package org.smm.archetype.shared.aspect.idempotent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.component.cache.CacheComponent;
import org.smm.archetype.config.IdempotentConfigure;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IdempotentConfigure 配置装配")
class IdempotentConfigureUTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                                                                   .withUserConfiguration(IdempotentConfigure.class);

    @Test
    @DisplayName("CacheComponent Bean 存在时 → IdempotentAspect Bean 正常注册")
    void should_register_aspect_when_cache_client_available() {
        contextRunner
                .withBean(CacheComponent.class, () -> org.mockito.Mockito.mock(CacheComponent.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(IdempotentAspect.class);
                    assertThat(context).hasSingleBean(CacheComponent.class);
                });
    }

    @Test
    @DisplayName("CacheComponent Bean 不存在时 → IdempotentAspect Bean 创建失败（符合预期）")
    void should_fail_when_cache_client_not_available() {
        contextRunner
                .run(context -> {
                    // 没有 CacheComponent Bean，IdempotentAspect 无法创建
                    assertThat(context).doesNotHaveBean(IdempotentAspect.class);
                });
    }

}
