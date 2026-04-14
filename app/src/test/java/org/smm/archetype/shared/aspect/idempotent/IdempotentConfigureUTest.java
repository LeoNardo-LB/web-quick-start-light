package org.smm.archetype.shared.aspect.idempotent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.client.cache.CacheClient;
import org.smm.archetype.config.IdempotentConfigure;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IdempotentConfigure 配置装配")
class IdempotentConfigureUTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                                                                   .withUserConfiguration(IdempotentConfigure.class);

    @Test
    @DisplayName("CacheClient Bean 存在时 → IdempotentAspect Bean 正常注册")
    void should_register_aspect_when_cache_client_available() {
        contextRunner
                .withBean(CacheClient.class, () -> org.mockito.Mockito.mock(CacheClient.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(IdempotentAspect.class);
                    assertThat(context).hasSingleBean(CacheClient.class);
                });
    }

    @Test
    @DisplayName("CacheClient Bean 不存在时 → IdempotentAspect Bean 创建失败（符合预期）")
    void should_fail_when_cache_client_not_available() {
        contextRunner
                .run(context -> {
                    // 没有 CacheClient Bean，IdempotentAspect 无法创建
                    assertThat(context).doesNotHaveBean(IdempotentAspect.class);
                });
    }

}
