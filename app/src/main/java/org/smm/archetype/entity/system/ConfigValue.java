package org.smm.archetype.entity.system;

/**
 * 配置值值对象
 */
public record ConfigValue(String value) {

    public ConfigValue {
        // value 允许为空字符串，不允许为 null
    }

    public static ConfigValue of(String value) {
        return new ConfigValue(value != null ? value : "");
    }
}
