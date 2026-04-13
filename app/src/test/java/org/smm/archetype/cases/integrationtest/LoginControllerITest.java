package org.smm.archetype.cases.integrationtest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.IntegrationTestBase;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LoginController 集成测试。
 * 测试登录/注销端到端流程。
 */
class LoginControllerITest extends IntegrationTestBase {

    @Test
    @DisplayName("POST /api/auth/login 成功返回 token")
    void loginShouldReturnToken() {
        webTestClient.post().uri("/api/auth/login")
                .bodyValue(Map.of("username", "admin", "password", "admin123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.token").isNotEmpty();
    }

    @Test
    @DisplayName("POST /api/auth/login 错误密码返回错误")
    void loginWithWrongPasswordShouldReturnError() {
        webTestClient.post().uri("/api/auth/login")
                .bodyValue(Map.of("username", "admin", "password", "wrong-password"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.code").isEqualTo(6602);
    }

    @Test
    @DisplayName("POST /api/auth/login 用户不存在返回错误")
    void loginWithNonexistentUserShouldReturnError() {
        webTestClient.post().uri("/api/auth/login")
                .bodyValue(Map.of("username", "nonexistent", "password", "any"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.code").isEqualTo(6603);
    }

    @Test
    @DisplayName("POST /api/auth/logout 成功")
    void logoutShouldSucceed() {
        webTestClient.post().uri("/api/auth/logout")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true);
    }
}
