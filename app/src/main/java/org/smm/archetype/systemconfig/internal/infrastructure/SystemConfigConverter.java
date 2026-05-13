package org.smm.archetype.systemconfig.internal.infrastructure;

import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.smm.archetype.systemconfig.internal.ConfigGroup;
import org.smm.archetype.systemconfig.internal.ConfigKey;
import org.smm.archetype.systemconfig.internal.ConfigValue;
import org.smm.archetype.systemconfig.internal.DisplayName;
import org.smm.archetype.systemconfig.internal.InputType;
import org.smm.archetype.systemconfig.internal.SystemConfig;
import org.smm.archetype.systemconfig.internal.ValueType;
import org.smm.archetype.shared.CentralMapperConfig;

/**
 * 系统配置 DO ↔ Model 转换器（MapStruct 生成实现）
 * <p>
 * 包含值对象（ConfigKey/ConfigValue/DisplayName/ConfigGroup）的自定义映射。
 */
@Mapper(config = CentralMapperConfig.class)
public interface SystemConfigConverter {

    SystemConfig toModel(SystemConfigDO configDO);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createUser", ignore = true)
    @Mapping(target = "updateUser", ignore = true)
    @Mapping(target = "deleteTime", ignore = true)
    @Mapping(target = "deleteUser", ignore = true)
    SystemConfigDO toDO(SystemConfig config);

    // === 值对象 ↔ String 映射（MapStruct 自动使用） ===

    default String map(ConfigKey key) {
        return key != null ? key.value() : null;
    }

    default ConfigKey map(String value) {
        return value != null ? ConfigKey.of(value) : null;
    }

    default String map(ConfigValue value) {
        return value != null ? value.value() : null;
    }

    default ConfigValue toConfigValue(String value) {
        return ConfigValue.of(value);
    }

    default String map(DisplayName name) {
        return name != null ? name.value() : null;
    }

    default DisplayName toDisplayName(String value) {
        return value != null ? DisplayName.of(value) : null;
    }

    default String map(ConfigGroup group) {
        return group != null ? group.getCode() : null;
    }

    default ConfigGroup toConfigGroup(String code) {
        return ConfigGroup.fromCode(code);
    }

    default String map(ValueType type) {
        return type != null ? type.getCode() : null;
    }

    default ValueType toValueType(String code) {
        return ValueType.fromCode(code);
    }

    default String map(InputType type) {
        return type != null ? type.getCode() : null;
    }

    default InputType toInputType(String code) {
        return InputType.fromCode(code);
    }
}
