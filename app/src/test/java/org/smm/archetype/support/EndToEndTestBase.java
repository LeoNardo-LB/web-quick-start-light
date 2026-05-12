package org.smm.archetype.support;

import org.junit.jupiter.api.DisplayName;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * 端到端测试基类。
 * <p>
 * 继承 {@link IntegrationTestBase} 复用 Spring 上下文和 WebTestClient，
 * 侧重<strong>业务流程编排</strong>：测试完整用户场景（如 CRUD 全流程），
 * 而非单个 API 调用。
 * </p>
 *
 * <h3>命名约定</h3>
 * <ul>
 *   <li>测试类以 {@code ETest} 结尾</li>
 *   <li>使用 {@code @DisplayName} 描述业务场景</li>
 * </ul>
 */
@DisplayName("端到端测试")
public abstract class EndToEndTestBase extends IntegrationTestBase {

    // --- HTTP 辅助方法 ---

    protected WebTestClient.ResponseSpec exchangeGet(String path) {
        return webTestClient.get()
                .uri(path)
                .exchange();
    }

    protected WebTestClient.ResponseSpec exchangePost(String path, Object body) {
        return webTestClient.post()
                .uri(path)
                .bodyValue(body)
                .exchange();
    }

    protected WebTestClient.ResponseSpec exchangePut(String path, Object body) {
        return webTestClient.put()
                .uri(path)
                .bodyValue(body)
                .exchange();
    }

    protected WebTestClient.ResponseSpec exchangeDelete(String path) {
        return webTestClient.delete()
                .uri(path)
                .exchange();
    }
}
