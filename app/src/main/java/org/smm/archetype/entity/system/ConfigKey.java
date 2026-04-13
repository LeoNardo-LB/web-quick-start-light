package org.smm.archetype.entity.system;

/**
 * 配置键值对象
 */
public record ConfigKey(String value) {

    public ConfigKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("配置键不能为空");
        }
    }

    public static ConfigKey of(String value) {
        return new ConfigKey(value.trim());
    }
}
