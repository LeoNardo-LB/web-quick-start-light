package org.smm.archetype.repository.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.entity.system.ConfigGroup;
import org.smm.archetype.entity.system.ConfigKey;
import org.smm.archetype.entity.system.ConfigValue;
import org.smm.archetype.entity.system.SystemConfig;
import org.smm.archetype.entity.system.ValueType;
import org.smm.archetype.entity.system.InputType;
import org.smm.archetype.support.IntegrationTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 系统配置仓储集成测试 — 验证 CRUD 操作（真实 SQLite 数据库）
 */
class SystemConfigRepositoryITest extends IntegrationTestBase {

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Nested
    @DisplayName("findByConfigKey")
    class FindByConfigKey {

        @Test
        @DisplayName("MFT: 根据已存在的 configKey 查询返回正确配置")
        void should_returnConfig_whenKeyExists() {
            Optional<SystemConfig> result = systemConfigRepository.findByConfigKey(ConfigKey.of("site.description"));

            assertThat(result).isPresent();
            assertThat(result.get().getConfigKey().value()).isEqualTo("site.description");
            assertThat(result.get().getGroupCode()).isEqualTo(ConfigGroup.BASIC);
            assertThat(result.get().getDisplayName().value()).isEqualTo("站点描述");
        }

        @Test
        @DisplayName("DIR: 不存在的 configKey 返回 empty")
        void should_returnEmpty_whenKeyNotExists() {
            Optional<SystemConfig> result = systemConfigRepository.findByConfigKey(ConfigKey.of("nonexistent.key"));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByGroupCode")
    class FindByGroupCode {

        @Test
        @DisplayName("MFT: 按 BASIC 分组查询返回 4 条配置，按 sort 排序")
        void should_returnBasicConfigs_sortedBySort() {
            List<SystemConfig> result = systemConfigRepository.findByGroupCode(ConfigGroup.BASIC);

            assertThat(result).hasSize(4);
            assertThat(result).allMatch(c -> c.getGroupCode() == ConfigGroup.BASIC);
            // 验证按 sort 升序排列
            assertThat(result.get(0).getSort()).isLessThanOrEqualTo(result.get(1).getSort());
            assertThat(result.get(1).getSort()).isLessThanOrEqualTo(result.get(2).getSort());
            assertThat(result.get(2).getSort()).isLessThanOrEqualTo(result.get(3).getSort());
        }

        @Test
        @DisplayName("MFT: 按 EMAIL 分组查询返回 4 条配置")
        void should_returnEmailConfigs() {
            List<SystemConfig> result = systemConfigRepository.findByGroupCode(ConfigGroup.EMAIL);

            assertThat(result).hasSize(4);
            assertThat(result).allMatch(c -> c.getGroupCode() == ConfigGroup.EMAIL);
        }

        @Test
        @DisplayName("MFT: 按 SECURITY 分组查询返回 4 条配置")
        void should_returnSecurityConfigs() {
            List<SystemConfig> result = systemConfigRepository.findByGroupCode(ConfigGroup.SECURITY);

            assertThat(result).hasSize(4);
            assertThat(result).allMatch(c -> c.getGroupCode() == ConfigGroup.SECURITY);
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("MFT: 查询所有配置返回 15 条，按 groupCode + sort 排序")
        void should_returnAllConfigs_sortedByGroupAndSort() {
            List<SystemConfig> result = systemConfigRepository.findAll();

            assertThat(result).hasSize(15);
            // 验证按 groupCode 分组、组内按 sort 排序
            for (int i = 1; i < result.size(); i++) {
                SystemConfig prev = result.get(i - 1);
                SystemConfig curr = result.get(i);
                int groupCompare = prev.getGroupCode().getCode().compareTo(curr.getGroupCode().getCode());
                if (groupCompare == 0) {
                    assertThat(prev.getSort()).isLessThanOrEqualTo(curr.getSort());
                } else {
                    assertThat(groupCompare).isLessThan(0);
                }
            }
        }
    }

    @Nested
    @DisplayName("save — 新增和更新")
    class Save {

        @Test
        @DisplayName("MFT: 新增配置成功，返回含 id 的实体")
        void should_insertNewConfig_withGeneratedId() {
            SystemConfig newConfig = new SystemConfig();
            newConfig.setConfigKey(ConfigKey.of("test.new.key"));
            newConfig.setConfigValue(ConfigValue.of("test-value"));
            newConfig.setValueType(ValueType.STRING);
            newConfig.setGroupCode(ConfigGroup.BASIC);
            newConfig.setDisplayName(org.smm.archetype.entity.system.DisplayName.of("测试配置"));
            newConfig.setDescription("测试描述");
            newConfig.setInputType(InputType.TEXT);
            newConfig.setInputConfig("");
            newConfig.setSort(99);

            SystemConfig saved = systemConfigRepository.save(newConfig);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getConfigKey().value()).isEqualTo("test.new.key");
            assertThat(saved.getConfigValue().value()).isEqualTo("test-value");

            // 验证可以通过 findByConfigKey 查到
            Optional<SystemConfig> found = systemConfigRepository.findByConfigKey(ConfigKey.of("test.new.key"));
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("MFT: 更新已有配置的 configValue")
        void should_updateExistingConfig() {
            // 先查找一个已存在的配置
            Optional<SystemConfig> existing = systemConfigRepository.findByConfigKey(ConfigKey.of("site.description"));
            assertThat(existing).isPresent();

            SystemConfig config = existing.get();
            String originalValue = config.getConfigValue().value();
            config.updateValue(ConfigValue.of("Updated description"));

            SystemConfig updated = systemConfigRepository.save(config);

            assertThat(updated.getConfigValue().value()).isEqualTo("Updated description");
            assertThat(updated.getId()).isEqualTo(config.getId());

            // 验证数据库中已更新
            Optional<SystemConfig> reloaded = systemConfigRepository.findByConfigKey(ConfigKey.of("site.description"));
            assertThat(reloaded).isPresent();
            assertThat(reloaded.get().getConfigValue().value()).isEqualTo("Updated description");
        }
    }
}
