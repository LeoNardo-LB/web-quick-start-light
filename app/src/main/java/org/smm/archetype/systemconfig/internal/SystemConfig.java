package org.smm.archetype.systemconfig.internal;

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

    /**
     * 判断配置是否可编辑。
     * <p>
     * 当配置值类型为 BOOLEAN 时不可编辑配置值本身（只能切换开关）。
     *
     * @return true 表示可编辑配置值
     */
    public boolean isEditable() {
        return this.valueType != ValueType.BOOLEAN;
    }
}
