package org.smm.archetype.entity.system;

import lombok.Getter;
import lombok.Setter;

/**
 * 系统配置实体
 */
@Getter
@Setter
public class SystemConfig {

    private Long id;
    private ConfigKey configKey;
    private ConfigValue configValue;
    private ValueType valueType;
    private ConfigGroup groupCode;
    private DisplayName displayName;
    private String description;
    private InputType inputType;
    private String inputConfig;
    private Integer sort;

    /**
     * 更新配置值
     *
     * @param newValue 新的配置值
     */
    public void updateValue(ConfigValue newValue) {
        if (newValue == null) {
            throw new IllegalArgumentException("配置值不能为空");
        }
        this.configValue = newValue;
    }
}
