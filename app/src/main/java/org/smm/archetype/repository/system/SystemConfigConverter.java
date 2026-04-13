package org.smm.archetype.repository.system;

import org.smm.archetype.entity.system.ConfigGroup;
import org.smm.archetype.entity.system.ConfigKey;
import org.smm.archetype.entity.system.ConfigValue;
import org.smm.archetype.entity.system.DisplayName;
import org.smm.archetype.entity.system.InputType;
import org.smm.archetype.entity.system.SystemConfig;
import org.smm.archetype.entity.system.ValueType;
import org.smm.archetype.generated.entity.SystemConfigDO;
import org.springframework.stereotype.Component;

/**
 * 系统配置 DO ↔ Entity 转换器
 */
@Component
public class SystemConfigConverter {

    public SystemConfigDO toDataObject(SystemConfig config) {
        if (config == null) {
            return null;
        }
        SystemConfigDO configDO = new SystemConfigDO();
        configDO.setId(config.getId());
        configDO.setConfigKey(config.getConfigKey() != null ? config.getConfigKey().value() : null);
        configDO.setConfigValue(config.getConfigValue() != null ? config.getConfigValue().value() : null);
        configDO.setValueType(config.getValueType() != null ? config.getValueType().getCode() : null);
        configDO.setGroupCode(config.getGroupCode() != null ? config.getGroupCode().getCode() : null);
        configDO.setDisplayName(config.getDisplayName() != null ? config.getDisplayName().value() : null);
        configDO.setDescription(config.getDescription());
        configDO.setInputType(config.getInputType() != null ? config.getInputType().getCode() : null);
        configDO.setInputConfig(config.getInputConfig());
        configDO.setSort(config.getSort());
        return configDO;
    }

    public SystemConfig toEntity(SystemConfigDO configDO) {
        if (configDO == null) {
            return null;
        }
        SystemConfig config = new SystemConfig();
        config.setId(configDO.getId());
        config.setConfigKey(configDO.getConfigKey() != null ? ConfigKey.of(configDO.getConfigKey()) : null);
        config.setConfigValue(ConfigValue.of(configDO.getConfigValue()));
        config.setValueType(ValueType.fromCode(configDO.getValueType()));
        config.setGroupCode(ConfigGroup.fromCode(configDO.getGroupCode()));
        config.setDisplayName(configDO.getDisplayName() != null ? DisplayName.of(configDO.getDisplayName()) : null);
        config.setDescription(configDO.getDescription());
        config.setInputType(InputType.fromCode(configDO.getInputType()));
        config.setInputConfig(configDO.getInputConfig());
        config.setSort(configDO.getSort());
        return config;
    }
}
