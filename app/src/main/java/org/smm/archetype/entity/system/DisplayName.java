package org.smm.archetype.entity.system;

/**
 * 显示名称值对象
 */
public record DisplayName(String value) {

    public DisplayName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("显示名称不能为空");
        }
    }

    public static DisplayName of(String value) {
        return new DisplayName(value.trim());
    }
}
