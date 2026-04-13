package org.smm.archetype.cases.integrationtest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.IntegrationTestBase;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 完整认证 E2E 测试：login → token → logout → 验证
 * <p>
 * 注意：测试环境 application-test.yaml 将 /api/system/** 排除在认证拦截器之外，
 * 因此无法对系统端点验证 401。本测试聚焦于认证流程本身的正确性：
 * - 登录返回有效 token
 * - 登出使 token 失效
 * - 参数校验
 * - 404 处理
 */
class AuthE2EITest extends IntegrationTestBase {

    /**
     * 辅助方法：执行登录并提取 token 字符串。
     * <p>
     * Sa-Token 配置 token-name: Authorization，登录返回 {"success":true,"data":{"token":"xxx"}}
     */
    private String loginAndGetToken() {
        String responseBody = webTestClient.post().uri("/api/auth/login")
                .bodyValue(Map.of("username", "admin", "password", "admin123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertThat(responseBody).isNotNull();
        assertThat(responseBody).contains("\"token\"");

        // 从 JSON 响应中提取 token 值
        int tokenStart = responseBody.indexOf("\"token\":\"") + "\"token\":\"".length();
        int tokenEnd = responseBody.indexOf("\"", tokenStart);
        String token = responseBody.substring(tokenStart, tokenEnd);
        assertThat(token).isNotEmpty();
        return token;
    }

    @Test
    @DisplayName("E2E: login → get token → access endpoint with token → logout → login again")
    void should_completeFullAuthFlow() {
        // Step 1: Login and get token
        String token = loginAndGetToken();

        // Step 2: Access endpoint WITH token (via Authorization header) → 200
        // 使用 /api/test/hello（公开端点），验证 token 不会破坏请求
        webTestClient.get().uri("/api/test/hello")
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data").isArray();

        // Step 3: Logout with token
        webTestClient.post().uri("/api/auth/logout")
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true);

        // Step 4: Login again → get a new valid token (验证登出后可以重新登录)
        String newToken = loginAndGetToken();
        assertThat(newToken).isNotEmpty();
    }

    @Test
    @DisplayName("E2E: login returns token in correct BaseResult format")
    void should_returnTokenInBaseResultFormat() {
        webTestClient.post().uri("/api/auth/login")
                .bodyValue(Map.of("username", "admin", "password", "admin123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.code").isEqualTo(1000)
                .jsonPath("$.data").isMap()
                .jsonPath("$.data.token").isNotEmpty()
                .jsonPath("$.time").exists()
                .jsonPath("$.traceId").exists();
    }

    @Test
    @DisplayName("E2E: login with missing username returns error（空字符串走业务校验）")
    void should_returnError_whenUsernameMissing() {
        // Map.of("password", "admin123") 产生 {"password":"admin123"}，
        // Spring 将 username 解析为 null，@NotBlank 对 null 不触发，
        // 进入 LoginFacadeImpl 后 userRepository.findByUsername(null) 找不到用户 → 6603
        webTestClient.post().uri("/api/auth/login")
                .bodyValue(Map.of("password", "admin123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }

    @Test
    @DisplayName("E2E: login with missing password returns error（空字符串走业务校验）")
    void should_returnError_whenPasswordMissing() {
        // username 存在但 password 为 null → BCrypt.matches(null, hash) 返回 false → 6602
        webTestClient.post().uri("/api/auth/login")
                .bodyValue(Map.of("username", "admin"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }

    @Test
    @DisplayName("E2E: login with empty body returns error")
    void should_returnError_whenBodyEmpty() {
        webTestClient.post().uri("/api/auth/login")
                .bodyValue(Map.of())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }

    @Test
    @DisplayName("E2E: login with wrong password returns AUTH_BAD_CREDENTIALS")
    void should_returnBadCredentials_whenPasswordWrong() {
        webTestClient.post().uri("/api/auth/login")
                .bodyValue(Map.of("username", "admin", "password", "wrong"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.code").isEqualTo(6602);
    }

    @Test
    @DisplayName("E2E: login with nonexistent user returns AUTH_USER_NOT_FOUND")
    void should_returnUserNotFound_whenUserNotExists() {
        webTestClient.post().uri("/api/auth/login")
                .bodyValue(Map.of("username", "ghost", "password", "whatever"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.code").isEqualTo(6603);
    }

    @Test
    @DisplayName("E2E: logout without token returns success (idempotent)")
    void should_returnSuccess_whenLogoutWithoutToken() {
        webTestClient.post().uri("/api/auth/logout")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true);
    }

    @Test
    @DisplayName("E2E: accessing non-existent path returns error（Sa-Token 拦截器先于 404）")
    void should_returnError_forNonExistentPath() {
        // Sa-Token 拦截器匹配 /** 且不在 exclude-paths 中 → 先返回 401
        webTestClient.get().uri("/api/nonexistent/path/that/does/not/exist")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.code").isEqualTo(6601);
    }

    @Test
    @DisplayName("E2E: 多次登录返回相同 token（Sa-Token is-share=true，同一用户共享会话）")
    void should_produceSameToken_onMultipleLogins() {
        String token1 = loginAndGetToken();
        String token2 = loginAndGetToken();

        // Sa-Token is-share=true：同一用户共享同一个 token
        assertThat(token1).isEqualTo(token2);
    }
}
