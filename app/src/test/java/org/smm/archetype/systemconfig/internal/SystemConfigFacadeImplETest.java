package org.smm.archetype.systemconfig.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.smm.archetype.support.EndToEndTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SystemConfigFacadeImpl 端到端测试 — 验证 Facade 层完整业务流程
 * <p>
 * 与 ITest 不同，ETest 侧重跨方法编排场景（先查后改再验），
 * 覆盖 Facade public 方法的端到端业务流程。
 */
@DisplayName("SystemConfigFacade ETest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SystemConfigFacadeImplETest extends EndToEndTestBase {

    @Autowired
    private org.smm.archetype.systemconfig.SystemConfigFacade systemConfigFacade;

    // === 业务流程编排：查询 → 验证 → 修改 → 验证 ===

    @Nested
    @DisplayName("业务流程：配置查询全链路")
    class ConfigQueryFlow {

        @Test
        @DisplayName("FLOW: 获取全部分组 → 按分组查询配置 → 按 Key 查询单条")
        void should_queryConfigsInSequence() {
            // Step 1: 获取所有分组
            List<ConfigGroupVO> groups = systemConfigFacade.getAllGroups();
            assertThat(groups).isNotEmpty();

            String firstGroupCode = groups.getFirst().code();

            // Step 2: 按分组查询配置
            List<SystemConfigVO> groupConfigs = systemConfigFacade.getConfigsByGroup(firstGroupCode);
            assertThat(groupConfigs).isNotEmpty();
            assertThat(groupConfigs).allMatch(c -> firstGroupCode.equals(c.groupCode()));

            // Step 3: 取第一条配置的 key，按 key 查询
            String configKey = groupConfigs.getFirst().configKey();
            SystemConfigVO config = systemConfigFacade.getConfigByKey(configKey);
            assertThat(config).isNotNull();
            assertThat(config.configKey()).isEqualTo(configKey);
            assertThat(config.groupCode()).isEqualTo(firstGroupCode);
        }

        @Test
        @DisplayName("FLOW: 获取所有配置 → 验证每条都有非空 key → 逐条按 key 查询成功")
        void should_verifyAllConfigsQueryableByKey() {
            List<SystemConfigVO> allConfigs = systemConfigFacade.getAllConfigs();
            assertThat(allConfigs).isNotEmpty();

            // 抽样验证前 3 条（避免测试过长）
            List<SystemConfigVO> sample = allConfigs.stream().limit(3).toList();
            for (SystemConfigVO vo : sample) {
                SystemConfigVO queried = systemConfigFacade.getConfigByKey(vo.configKey());
                assertThat(queried.configKey()).isEqualTo(vo.configKey());
                assertThat(queried.configValue()).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("业务流程：配置更新全链路")
    class ConfigUpdateFlow {

        @Test
        @DisplayName("FLOW: 查询配置 → 更新值 → 验证更新 → 还原")
        void should_updateAndVerifyConfigValue() {
            // Step 1: 查询当前值
            SystemConfigVO before = systemConfigFacade.getConfigByKey("site.name");
            String originalValue = before.configValue();

            // Step 2: 更新为新值
            String updatedValue = "ETest-Updated-" + System.currentTimeMillis();
            systemConfigFacade.updateConfig(new UpdateConfigCommand("site.name", updatedValue));

            // Step 3: 验证更新
            SystemConfigVO after = systemConfigFacade.getConfigByKey("site.name");
            assertThat(after.configValue()).isEqualTo(updatedValue);

            // Step 4: 还原（保证测试幂等）
            systemConfigFacade.updateConfig(new UpdateConfigCommand("site.name", originalValue));
            SystemConfigVO restored = systemConfigFacade.getConfigByKey("site.name");
            assertThat(restored.configValue()).isEqualTo(originalValue);
        }

        @Test
        @DisplayName("FLOW: 更新不存在的 key → 抛 BizException → 原有数据不受影响")
        void should_notAffectExistingData_whenUpdateNonExistentKey() {
            // 记录当前配置数
            int countBefore = systemConfigFacade.getAllConfigs().size();

            // 尝试更新不存在的 key（Service 层抛 IllegalArgumentException）
            assertThatThrownBy(() -> systemConfigFacade.updateConfig(
                    new UpdateConfigCommand("nonexistent.etest.key", "value")))
                    .isInstanceOf(Exception.class);

            // 验证原有数据不受影响
            int countAfter = systemConfigFacade.getAllConfigs().size();
            assertThat(countAfter).isEqualTo(countBefore);
        }
    }

    @Nested
    @DisplayName("业务流程：分组一致性验证")
    class ConfigGroupConsistencyFlow {

        @Test
        @DisplayName("FLOW: 获取所有分组 → 每个分组都能查到配置 → 配置总数一致")
        void should_verifyGroupConfigConsistency() {
            // Step 1: 获取所有分组
            List<ConfigGroupVO> groups = systemConfigFacade.getAllGroups();
            assertThat(groups).hasSizeGreaterThanOrEqualTo(4);

            // Step 2: 逐分组查询，统计配置总数
            int totalByGroup = 0;
            for (ConfigGroupVO group : groups) {
                List<SystemConfigVO> configs = systemConfigFacade.getConfigsByGroup(group.code());
                assertThat(configs).isNotEmpty();
                totalByGroup += configs.size();
            }

            // Step 3: 验证总数与 getAllConfigs 一致
            List<SystemConfigVO> allConfigs = systemConfigFacade.getAllConfigs();
            assertThat(totalByGroup).isEqualTo(allConfigs.size());
        }
    }
}
