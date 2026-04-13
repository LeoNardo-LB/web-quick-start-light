package org.smm.archetype.cases.integrationtest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.IntegrationTestBase;

/**
 * AuthInterceptor 集成测试。
 * 测试 Sa-Token 拦截器对路由的保护/放行行为。
 */
class AuthInterceptorITest extends IntegrationTestBase {

    @Test
    @DisplayName("未登录访问受保护端点应返回 401")
    void unauthenticatedAccessShouldReturn401() {
        webTestClient.get().uri("/api/protected/test")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("/api/test/hello 无需认证")
    void testHelloShouldBeAccessible() {
        webTestClient.get().uri("/api/test/hello")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true);
    }

    @Test
    @DisplayName("/api/test/validate 无需认证")
    void testValidateShouldBeAccessible() {
        webTestClient.get().uri("/api/test/validate?name=test")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true);
    }

    @Test
    @DisplayName("/api/auth/** 应放行（返回 404 而非 401）")
    void authEndpointsShouldBeExcluded() {
        webTestClient.post().uri("/api/auth/nonexistent")
                .exchange()
                .expectStatus().isNotFound();
    }
}
