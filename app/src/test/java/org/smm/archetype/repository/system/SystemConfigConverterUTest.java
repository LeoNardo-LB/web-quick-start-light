package org.smm.archetype.repository.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.entity.system.ConfigGroup;
import org.smm.archetype.entity.system.ConfigKey;
import org.smm.archetype.entity.system.ConfigValue;
import org.smm.archetype.entity.system.InputType;
import org.smm.archetype.entity.system.SystemConfig;
import org.smm.archetype.entity.system.ValueType;
import org.smm.archetype.generated.entity.SystemConfigDO;
import org.smm.archetype.support.UnitTestBase;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SystemConfigConverter")
class SystemConfigConverterUTest extends UnitTestBase {

    private final SystemConfigConverter converter = new SystemConfigConverter();

    // =========================================================================
    // toDataObject
    // =========================================================================

    @Nested
    @DisplayName("toDataObject")
    class ToDataObject {

        @Test
        @DisplayName("正常转换 — 所有字段非 null")
        void should_convert_all_fields_when_all_non_null() {
            // given
            SystemConfig config = new SystemConfig();
            config.setId(1L);
            config.setConfigKey(ConfigKey.of("site.name"));
            config.setConfigValue(ConfigValue.of("MyApp"));
            config.setValueType(ValueType.STRING);
            config.setGroupCode(ConfigGroup.BASIC);
            config.setDisplayName(org.smm.archetype.entity.system.DisplayName.of("站点名称"));
            config.setDescription("站点名称配置");
            config.setInputType(InputType.TEXT);
            config.setInputConfig("{\"maxLength\":100}");
            config.setSort(10);

            // when
            SystemConfigDO configDO = converter.toDataObject(config);

            // then
            assertThat(configDO).isNotNull();
            assertThat(configDO.getId()).isEqualTo(1L);
            assertThat(configDO.getConfigKey()).isEqualTo("site.name");
            assertThat(configDO.getConfigValue()).isEqualTo("MyApp");
            assertThat(configDO.getValueType()).isEqualTo("STRING");
            assertThat(configDO.getGroupCode()).isEqualTo("BASIC");
            assertThat(configDO.getDisplayName()).isEqualTo("站点名称");
            assertThat(configDO.getDescription()).isEqualTo("站点名称配置");
            assertThat(configDO.getInputType()).isEqualTo("TEXT");
            assertThat(configDO.getInputConfig()).isEqualTo("{\"maxLength\":100}");
            assertThat(configDO.getSort()).isEqualTo(10);
        }

        @Test
        @DisplayName("所有值对象为 null 时 DO 字段也应为 null")
        void should_set_null_when_value_objects_are_null() {
            // given
            SystemConfig config = new SystemConfig();
            config.setId(2L);
            config.setDescription("仅描述有值");

            // when
            SystemConfigDO configDO = converter.toDataObject(config);

            // then
            assertThat(configDO).isNotNull();
            assertThat(configDO.getId()).isEqualTo(2L);
            assertThat(configDO.getConfigKey()).isNull();
            assertThat(configDO.getConfigValue()).isNull();
            assertThat(configDO.getValueType()).isNull();
            assertThat(configDO.getGroupCode()).isNull();
            assertThat(configDO.getDisplayName()).isNull();
            assertThat(configDO.getInputType()).isNull();
            assertThat(configDO.getDescription()).isEqualTo("仅描述有值");
        }

        @Test
        @DisplayName("输入 null 应返回 null")
        void should_return_null_when_input_is_null() {
            SystemConfigDO configDO = converter.toDataObject(null);

            assertThat(configDO).isNull();
        }
    }

    // =========================================================================
    // toEntity
    // =========================================================================

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("正常转换 — 所有字段非 null")
        void should_convert_all_fields_when_all_non_null() {
            // given
            SystemConfigDO configDO = SystemConfigDO.builder()
                    .configKey("site.name")
                    .configValue("MyApp")
                    .valueType("STRING")
                    .groupCode("BASIC")
                    .displayName("站点名称")
                    .description("站点名称配置")
                    .inputType("TEXT")
                    .inputConfig("{\"maxLength\":100}")
                    .sort(10)
                    .build();
            configDO.setId(1L);

            // when
            SystemConfig config = converter.toEntity(configDO);

            // then
            assertThat(config).isNotNull();
            assertThat(config.getId()).isEqualTo(1L);
            assertThat(config.getConfigKey()).isNotNull();
            assertThat(config.getConfigKey().value()).isEqualTo("site.name");
            assertThat(config.getConfigValue()).isNotNull();
            assertThat(config.getConfigValue().value()).isEqualTo("MyApp");
            assertThat(config.getValueType()).isEqualTo(ValueType.STRING);
            assertThat(config.getGroupCode()).isEqualTo(ConfigGroup.BASIC);
            assertThat(config.getDisplayName()).isNotNull();
            assertThat(config.getDisplayName().value()).isEqualTo("站点名称");
            assertThat(config.getDescription()).isEqualTo("站点名称配置");
            assertThat(config.getInputType()).isEqualTo(InputType.TEXT);
            assertThat(config.getInputConfig()).isEqualTo("{\"maxLength\":100}");
            assertThat(config.getSort()).isEqualTo(10);
        }

        @Test
        @DisplayName("DO 字段为 null 时值对象应为 null")
        void should_set_null_when_do_fields_are_null() {
            // given
            SystemConfigDO configDO = SystemConfigDO.builder()
                    .description("仅描述有值")
                    .build();
            configDO.setId(2L);

            // when
            SystemConfig config = converter.toEntity(configDO);

            // then
            assertThat(config).isNotNull();
            assertThat(config.getId()).isEqualTo(2L);
            assertThat(config.getConfigKey()).isNull();
            assertThat(config.getConfigValue()).isNotNull();
            assertThat(config.getConfigValue().value()).isEmpty();
            assertThat(config.getValueType()).isNull();
            assertThat(config.getGroupCode()).isNull();
            assertThat(config.getDisplayName()).isNull();
            assertThat(config.getInputType()).isNull();
            assertThat(config.getDescription()).isEqualTo("仅描述有值");
        }

        @Test
        @DisplayName("输入 null 应返回 null")
        void should_return_null_when_input_is_null() {
            SystemConfig config = converter.toEntity(null);

            assertThat(config).isNull();
        }
    }

    // =========================================================================
    // 往返一致性
    // =========================================================================

    @Nested
    @DisplayName("往返一致性")
    class RoundTrip {

        @Test
        @DisplayName("toDataObject + toEntity 往返应保持数据一致")
        void should_preserve_data_in_round_trip() {
            // given
            SystemConfig original = new SystemConfig();
            original.setId(100L);
            original.setConfigKey(ConfigKey.of("app.theme"));
            original.setConfigValue(ConfigValue.of("dark"));
            original.setValueType(ValueType.ENUM);
            original.setGroupCode(ConfigGroup.BASIC);
            original.setDisplayName(org.smm.archetype.entity.system.DisplayName.of("主题配置"));
            original.setDescription("应用主题");
            original.setInputType(InputType.SELECT);
            original.setInputConfig("{\"options\":[\"dark\",\"light\"]}");
            original.setSort(5);

            // when — Entity → DO → Entity
            SystemConfigDO configDO = converter.toDataObject(original);
            SystemConfig restored = converter.toEntity(configDO);

            // then
            assertThat(restored).isNotNull();
            assertThat(restored.getId()).isEqualTo(original.getId());
            assertThat(restored.getConfigKey().value()).isEqualTo(original.getConfigKey().value());
            assertThat(restored.getConfigValue().value()).isEqualTo(original.getConfigValue().value());
            assertThat(restored.getValueType()).isEqualTo(original.getValueType());
            assertThat(restored.getGroupCode()).isEqualTo(original.getGroupCode());
            assertThat(restored.getDisplayName().value()).isEqualTo(original.getDisplayName().value());
            assertThat(restored.getDescription()).isEqualTo(original.getDescription());
            assertThat(restored.getInputType()).isEqualTo(original.getInputType());
            assertThat(restored.getInputConfig()).isEqualTo(original.getInputConfig());
            assertThat(restored.getSort()).isEqualTo(original.getSort());
        }
    }
}
