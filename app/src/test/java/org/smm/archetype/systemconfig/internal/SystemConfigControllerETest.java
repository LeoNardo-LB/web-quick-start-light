package org.smm.archetype.systemconfig.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.EndToEndTestBase;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SystemConfigController 端到端测试 — 验证 HTTP 端点完整业务流程
 * <p>
 * 与 ITest 不同，ETest 侧重跨端点编排场景（先查后改再验），
 * 通过完整 HTTP 链路验证 Controller 层端到端业务流程。
 */
@DisplayName("SystemConfigController ETest")
class SystemConfigControllerETest extends EndToEndTestBase {

    // === 业务流程编排：跨端点场景 ===

    @Nested
    @DisplayName("业务流程：配置查询全链路")
    class ConfigQueryFlow {

        @Test
        @DisplayName("FLOW: GET /groups → GET /group/{code} → GET /{key} 三级联动查询")
        void should_queryConfigViaThreeLevelFlow() {
            // Step 1: 获取分组列表
            exchangeGet("/api/system/configs/groups")
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(4);

            // Step 2: 按 BASIC 分组查询配置
            exchangeGet("/api/system/configs/group/BASIC")
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(4);

            // Step 3: 查询具体配置
            exchangeGet("/api/system/configs/site.name")
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.data.configKey").isEqualTo("site.name")
                    .jsonPath("$.data.groupCode").isEqualTo("BASIC");
        }

        @Test
        @DisplayName("FLOW: GET / 返回全部配置，数量与分页总数一致")
        void should_listAllConfigsConsistentWithPagination() {
            // 获取全量配置
            exchangeGet("/api/system/configs")
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.data.length()").isEqualTo(15);

            // 分页查询第 1 页，验证 total=15
            exchangeGet("/api/system/configs/page?pageNo=1&pageSize=100")
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.total").isEqualTo(15);
        }
    }

    @Nested
    @DisplayName("业务流程：配置更新全链路")
    class ConfigUpdateFlow {

        @Test
        @DisplayName("FLOW: 查询配置 → PUT 更新 → 查询验证 → 还原")
        void should_updateConfigAndVerifyViaHttp() {
            // Step 1: 查询当前值（用 jsonPath 断言存在）
            exchangeGet("/api/system/configs/site.description")
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.data.configKey").isEqualTo("site.description")
                    .jsonPath("$.data.configValue").exists();

            // Step 2: 更新为新值
            String updatedValue = "ETest-Updated-" + System.currentTimeMillis();
            exchangePut("/api/system/configs/site.description",
                    Map.of("configValue", updatedValue))
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.data.configValue").isEqualTo(updatedValue);

            // Step 3: 通过 GET 再次查询验证
            exchangeGet("/api/system/configs/site.description")
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.data.configValue").isEqualTo(updatedValue);

            // Step 4: 还原为默认值
            exchangePut("/api/system/configs/site.description",
                    Map.of("configValue", "一个简单高效的 Java Web 快速开始脚手架"))
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000);
        }

        @Test
        @DisplayName("FLOW: PUT 空 configValue → 校验失败 → 原值不变")
        void should_rejectBlankValueAndNotAffectOriginal() {
            // 先确认当前值存在
            exchangeGet("/api/system/configs/site.name")
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.data.configValue").isNotEmpty();

            // 尝试用空值更新（应被校验拒绝）
            exchangePut("/api/system/configs/site.name",
                    Map.of("configValue", ""))
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.code").isEqualTo(2001);

            // 验证原值未被修改
            exchangeGet("/api/system/configs/site.name")
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.data.configValue").isNotEmpty();
        }
    }

    @Nested
    @DisplayName("业务流程：错误路径端到端验证")
    class ErrorPathFlow {

        @Test
        @DisplayName("FLOW: 查不存在的 key → 更新不存在的 key → 两者都返回错误")
        void should_returnErrorForNonExistentKeyAcrossEndpoints() {
            // GET 不存在的 key
            exchangeGet("/api/system/configs/nonexistent.etest.key")
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false);

            // PUT 不存在的 key
            exchangePut("/api/system/configs/nonexistent.etest.key",
                    Map.of("configValue", "test"))
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false);
        }
    }
}
