package org.smm.archetype.cases.integrationtest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.client.auth.AuthClient;
import org.smm.archetype.support.IntegrationTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SaTokenAuthClient 集成测试。
 * <p>
 * 核心逻辑测试在 client-auth 模块的单元测试中完成（mock StpUtil）。
 * 此处测试端到端行为（需要完整 Spring 上下文 + HTTP 请求）。
 * <p>
 * 更完整的端到端测试见 AuthInterceptorITest（Task 5.6）。
 */
class SaTokenAuthClientITest extends IntegrationTestBase {

    @Nested
    @DisplayName("认证模块基本可用性验证")
    class AuthModuleAvailabilityTest {

        @Test
        @DisplayName("Spring 上下文中 AuthClient Bean 存在")
        void authClientBeanShouldExist() {
            AuthClient authClient = applicationContext.getBean(AuthClient.class);
            assertNotNull(authClient);
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
}
