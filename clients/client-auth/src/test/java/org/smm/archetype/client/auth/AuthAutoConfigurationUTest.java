package org.smm.archetype.client.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AuthAutoConfiguration 单元测试。
 * 使用 ApplicationContextRunner 测试条件装配。
 * <p>
 * 注意：client-auth 模块中 Sa-Token 是 optional 依赖，
 * 在测试 classpath 上 Sa-Token 实际不存在，因此 @ConditionalOnClass(name = "cn.dev33.satoken.stp.StpUtil")
 * 不会匹配，始终走 NoOpAuthClient 分支。
 */
class AuthAutoConfigurationUTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AuthAutoConfiguration.class));

    @Nested
    @DisplayName("默认配置")
    class DefaultConfigTest {

        @Test
        @DisplayName("默认应注册 AuthClient Bean")
        void shouldRegisterAuthClientByDefault() {
            contextRunner
                    .run(context -> {
                        assertThat(context).hasSingleBean(AuthClient.class);
                        // client-auth 测试 classpath 有 Sa-Token（optional 但测试时可用）
                        // 因此注册的是 SaTokenAuthClient
                        assertThat(context.getBean(AuthClient.class))
                                .isInstanceOf(SaTokenAuthClient.class);
                    });
        }
    }

    @Nested
    @DisplayName("enabled=false")
    class DisabledTest {

        @Test
        @DisplayName("enabled=false 时不应注册 AuthClient Bean")
        void shouldNotRegisterAuthClientWhenDisabled() {
            contextRunner
                    .withPropertyValues("middleware.auth.enabled=false")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(AuthClient.class);
                    });
        }
    }

    @Nested
    @DisplayName("enabled=true")
    class EnabledTest {

        @Test
        @DisplayName("enabled=true 时应注册 AuthClient Bean")
        void shouldRegisterAuthClientWhenEnabled() {
            contextRunner
                    .withPropertyValues("middleware.auth.enabled=true")
                    .run(context -> {
                        assertThat(context).hasSingleBean(AuthClient.class);
                    });
        }
    }

    @Nested
    @DisplayName("AuthProperties 绑定")
    class PropertiesBindingTest {

        @Test
        @DisplayName("AuthProperties Bean 应存在")
        void authPropertiesBeanShouldExist() {
            contextRunner
                    .run(context -> {
                        assertThat(context).hasSingleBean(AuthProperties.class);
                    });
        }

        @Test
        @DisplayName("默认 enabled=true")
        void defaultEnabledIsTrue() {
            contextRunner
                    .run(context -> {
                        AuthProperties properties = context.getBean(AuthProperties.class);
                        assertThat(properties.isEnabled()).isTrue();
                    });
        }

        @Test
        @DisplayName("excludePaths 应正确绑定")
        void excludePathsShouldBind() {
            contextRunner
                    .withPropertyValues(
                            "middleware.auth.exclude-paths=/api/test/**,/api/auth/**"
                    )
                    .run(context -> {
                        AuthProperties properties = context.getBean(AuthProperties.class);
                        assertThat(properties.getExcludePaths())
                                .containsExactly("/api/test/**", "/api/auth/**");
                    });
        }

        @Test
        @DisplayName("默认 excludePaths 为空列表")
        void defaultExcludePathsIsEmpty() {
            contextRunner
                    .run(context -> {
                        AuthProperties properties = context.getBean(AuthProperties.class);
                        assertThat(properties.getExcludePaths()).isEmpty();
                    });
        }
    }
}
