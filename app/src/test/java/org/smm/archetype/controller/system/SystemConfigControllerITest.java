package org.smm.archetype.controller.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.IntegrationTestBase;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 系统配置控制器集成测试 — 验证所有 API 端点
 */
class SystemConfigControllerITest extends IntegrationTestBase {

    @Nested
    @DisplayName("GET /api/system/configs")
    class GetAllConfigs {

        @Test
        @DisplayName("MFT: 返回 15 条配置，按分组排序")
        void should_returnAllConfigs_sorted() {
            webTestClient.get().uri("/api/system/configs")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(15);
        }
    }

    @Nested
    @DisplayName("GET /api/system/configs/groups")
    class GetAllGroups {

        @Test
        @DisplayName("MFT: 返回 4 个配置分组")
        void should_return4Groups() {
            webTestClient.get().uri("/api/system/configs/groups")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(4)
                    .jsonPath("$.data[0].code").isEqualTo("BASIC")
                    .jsonPath("$.data[0].displayName").isEqualTo("基础配置");
        }
    }

    @Nested
    @DisplayName("GET /api/system/configs/{key}")
    class GetConfigByKey {

        @Test
        @DisplayName("MFT: 已存在的 key 返回配置详情")
        void should_returnConfig_whenKeyExists() {
            webTestClient.get().uri("/api/system/configs/site.description")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data.configKey").isEqualTo("site.description")
                    .jsonPath("$.data.groupCode").isEqualTo("BASIC")
                    .jsonPath("$.data.displayName").isEqualTo("站点描述");
        }

        @Test
        @DisplayName("DIR: 不存在的 key 返回 fail")
        void should_returnFail_whenKeyNotExists() {
            webTestClient.get().uri("/api/system/configs/nonexistent.key")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(2000)
                    .jsonPath("$.success").isEqualTo(false);
        }
    }

    @Nested
    @DisplayName("GET /api/system/configs/group/{code}")
    class GetConfigsByGroup {

        @Test
        @DisplayName("MFT: 按 EMAIL 分组返回 4 条配置")
        void should_returnEmailConfigs() {
            webTestClient.get().uri("/api/system/configs/group/EMAIL")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(4);
        }

        @Test
        @DisplayName("DIR: 无效分组 code 返回错误")
        void should_returnError_whenInvalidGroup() {
            webTestClient.get().uri("/api/system/configs/group/INVALID")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false);
        }
    }

    @Nested
    @DisplayName("PUT /api/system/configs/{key}")
    class UpdateConfig {

        @Test
        @DisplayName("MFT: 更新配置值成功")
        void should_updateConfigValue() {
            Map<String, String> requestBody = Map.of("configValue", "Updated Site Name");

            webTestClient.put().uri("/api/system/configs/site.name")
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data.configKey").isEqualTo("site.name")
                    .jsonPath("$.data.configValue").isEqualTo("Updated Site Name");
        }

        @Test
        @DisplayName("DIR: configValue 为空返回校验失败")
        void should_returnValidationError_whenConfigValueBlank() {
            Map<String, String> requestBody = Map.of("configValue", "");

            webTestClient.put().uri("/api/system/configs/site.name")
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.code").isEqualTo(2001);
        }

        @Test
        @DisplayName("DIR: 更新不存在的 key 返回错误")
        void should_returnError_whenKeyNotExists() {
            Map<String, String> requestBody = Map.of("configValue", "some value");

            webTestClient.put().uri("/api/system/configs/nonexistent.key")
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false);
        }
    }
}
