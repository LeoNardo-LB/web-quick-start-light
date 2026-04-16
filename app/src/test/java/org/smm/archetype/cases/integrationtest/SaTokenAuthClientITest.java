package org.smm.archetype.cases.integrationtest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.component.auth.AuthComponent;
import org.smm.archetype.component.auth.SaTokenAuthComponent;
import org.smm.archetype.support.IntegrationTestBase;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SaTokenAuthClient 集成测试。
 * <p>
 * 核心逻辑测试在 component-auth 模块的单元测试中完成（mock StpUtil）。
 * 此处测试端到端行为（需要完整 Spring 上下文 + HTTP 请求）。
 * <p>
 * 更完整的端到端测试见 AuthInterceptorITest（Task 5.6）。
 */
class SaTokenAuthClientITest extends IntegrationTestBase {

    @Nested
    @DisplayName("认证模块基本可用性验证")
    class AuthModuleAvailabilityTest {

        @Test
        @DisplayName("Spring 上下文中 AuthComponent Bean 存在且为 SaTokenAuthComponent 实例")
        void authClientBeanShouldExist() {
            AuthComponent authComponent = applicationContext.getBean(AuthComponent.class);
            assertThat(authComponent).isNotNull();
            assertThat(authComponent).isInstanceOf(SaTokenAuthComponent.class);
        }

        @Test
        @DisplayName("无保护接口无需认证即可访问")
        void unprotectedEndpointAccessible() {
            webTestClient.get().uri("/api/test/hello")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true);
        }
    }

    @Nested
    @DisplayName("认证组件端到端行为验证")
    class AuthComponentBehaviorTest {

        @Test
        @DisplayName("未登录状态下 NotLoginException 被正确处理为 401")
        void should_handleNotLoginExceptionAs401() {
            // 通过 HTTP 端点验证 SaToken 异常处理链路已正确接入
            // TestController.not-login() 直接抛 NotLoginException，验证 WebExceptionAdvise 处理
            webTestClient.get().uri("/api/test/not-login")
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.code").isNotEmpty();
        }

        @Test
        @DisplayName("登录端点可达且返回业务错误（无测试用户时返回 AUTH_USER_NOT_FOUND）")
        void should_loginEndpointReachable() {
            // 使用不存在用户名测试，验证登录链路（AuthComponent → SaToken）已正确接入
            webTestClient.post().uri("/api/auth/login")
                    .bodyValue(new LoginRequestBody("nonexistent_user", "any_password"))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.code").isNotEmpty();
        }

        @Test
        @DisplayName("注销端点可达")
        void should_logoutEndpointReachable() {
            webTestClient.post().uri("/api/auth/logout")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true);
        }

        /**
         * 用于登录请求的 DTO（避免引入额外依赖）。
         */
        record LoginRequestBody(String username, String password) {}
    }
}
