package org.smm.archetype.service.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.exception.BizException;
import org.smm.archetype.exception.CommonErrorCode;
import org.smm.archetype.support.IntegrationTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LoginFacade 集成测试
 * <p>
 * 使用真实 Spring 上下文 + 内存 SQLite，init.sql 加载 admin/admin123 用户。
 * <p>
 * 注意：SaToken 需要 Servlet 请求上下文，因此 login 成功和 logout 测试通过
 * WebTestClient 发起 HTTP 请求；用户不存在和密码错误在 facade 层即抛出
 * BizException，可直接调用 facade 方法验证。
 */
@DisplayName("LoginFacade")
class LoginFacadeITest extends IntegrationTestBase {

    @Autowired
    private LoginFacade loginFacade;

    @Nested
    @DisplayName("login 成功场景")
    class LoginSuccessTest {

        @Test
        @DisplayName("MFT: 正确用户名密码登录成功返回非空 token")
        void should_loginSuccessfully_andReturnToken() {
            // SaToken 需要 Servlet 请求上下文，通过 HTTP 调用测试
            webTestClient.post().uri("/api/auth/login")
                    .bodyValue(Map.of("username", "admin", "password", "admin123"))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data.token").isNotEmpty();
        }
    }

    @Nested
    @DisplayName("login 失败场景")
    class LoginFailureTest {

        @Test
        @DisplayName("DIR: 用户不存在时抛 BizException 且 errorCode 为 AUTH_USER_NOT_FOUND")
        void should_throwWhenUserNotFound() {
            assertThatThrownBy(() -> loginFacade.login("nonexistent", "password"))
                    .isInstanceOf(BizException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.AUTH_USER_NOT_FOUND);
        }

        @Test
        @DisplayName("DIR: 密码错误时抛 BizException 且 errorCode 为 AUTH_BAD_CREDENTIALS")
        void should_throwWhenBadPassword() {
            assertThatThrownBy(() -> loginFacade.login("admin", "wrong-password"))
                    .isInstanceOf(BizException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.AUTH_BAD_CREDENTIALS);
        }
    }

    @Nested
    @DisplayName("logout")
    class LogoutTest {

        @Test
        @DisplayName("MFT: 注销成功不抛异常")
        void should_logoutSuccessfully() {
            // SaToken 需要 Servlet 请求上下文，通过 HTTP 调用测试
            webTestClient.post().uri("/api/auth/logout")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true);
        }
    }
}
