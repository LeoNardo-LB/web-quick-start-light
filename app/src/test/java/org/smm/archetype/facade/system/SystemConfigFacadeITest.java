package org.smm.archetype.facade.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.smm.archetype.exception.BizException;
import org.smm.archetype.support.IntegrationTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SystemConfigFacade 集成测试
 * <p>
 * 使用真实 Spring 上下文 + 内存 SQLite，init.sql 加载 15 条 system_config
 */
@DisplayName("SystemConfigFacade")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SystemConfigFacadeITest extends IntegrationTestBase {

    @Autowired
    private SystemConfigFacade systemConfigFacade;

    @Nested
    @DisplayName("getAllGroups")
    class GetAllGroups {

        @Test
        @DisplayName("MFT: 返回所有配置分组，至少包含 BASIC/EMAIL/STORAGE/SECURITY")
        void should_returnAllGroups() {
            List<ConfigGroupVO> groups = systemConfigFacade.getAllGroups();

            assertThat(groups).isNotEmpty();
            assertThat(groups).hasSizeGreaterThanOrEqualTo(4);

            List<String> codes = groups.stream().map(ConfigGroupVO::code).toList();
            assertThat(codes).contains("BASIC", "EMAIL", "STORAGE", "SECURITY");
        }
    }

    @Nested
    @DisplayName("getAllConfigs")
    class GetAllConfigs {

        @Test
        @DisplayName("MFT: 返回所有配置 VO，字段映射正确")
        void should_returnAllConfigsAsVOs() {
            List<SystemConfigVO> configs = systemConfigFacade.getAllConfigs();

            assertThat(configs).isNotEmpty();
            assertThat(configs).hasSizeGreaterThanOrEqualTo(15);

            SystemConfigVO first = configs.getFirst();
            assertThat(first.configKey()).isNotBlank();
            assertThat(first.configValue()).isNotNull();
            assertThat(first.groupCode()).isNotBlank();
            assertThat(first.displayName()).isNotBlank();
            assertThat(first.id()).isNotNull();
            assertThat(first.valueType()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("getConfigByKey")
    class GetConfigByKey {

        @Test
        @DisplayName("MFT: 按 key 返回配置 VO")
        void should_returnConfigByKey() {
            SystemConfigVO config = systemConfigFacade.getConfigByKey("site.name");

            assertThat(config).isNotNull();
            assertThat(config.configKey()).isEqualTo("site.name");
            assertThat(config.configValue()).isNotBlank();
            assertThat(config.groupCode()).isEqualTo("BASIC");
        }

        @Test
        @DisplayName("DIR: 配置不存在时抛 BizException")
        void should_throwBizException_whenKeyNotFound() {
            assertThatThrownBy(() -> systemConfigFacade.getConfigByKey("nonexistent"))
                    .isInstanceOf(BizException.class);
        }
    }

    @Nested
    @DisplayName("getConfigsByGroup")
    class GetConfigsByGroup {

        @Test
        @DisplayName("MFT: 按分组返回 VO 列表，所有记录属于目标分组")
        void should_returnConfigsByGroup() {
            List<SystemConfigVO> configs = systemConfigFacade.getConfigsByGroup("BASIC");

            assertThat(configs).isNotEmpty();
            assertThat(configs).allMatch(c -> "BASIC".equals(c.groupCode()));
        }
    }

    @Nested
    @DisplayName("updateConfig")
    class UpdateConfig {

        @Test
        @DisplayName("MFT: 更新配置值后重新查询验证已更新")
        void should_updateConfigValue() {
            // 先查询当前值
            SystemConfigVO before = systemConfigFacade.getConfigByKey("site.name");

            // 用不同值更新，确保与当前值不同
            String newValue = "Updated-" + System.currentTimeMillis();
            systemConfigFacade.updateConfig(new UpdateConfigCommand("site.name", newValue));

            // 验证更新后的值
            SystemConfigVO after = systemConfigFacade.getConfigByKey("site.name");
            assertThat(after.configValue()).isEqualTo(newValue);
            assertThat(after.configValue()).isNotEqualTo(before.configValue());
        }
    }
}
