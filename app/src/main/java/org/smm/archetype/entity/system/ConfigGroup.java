package org.smm.archetype.entity.system;

import lombok.Getter;

/**
 * 配置分组枚举
 */
@Getter
public enum ConfigGroup {
    BASIC("BASIC", "基础配置", "SettingOutlined", "#1890ff"),
    EMAIL("EMAIL", "邮件配置", "MailOutlined", "#52c41a"),
    STORAGE("STORAGE", "存储配置", "CloudOutlined", "#faad14"),
    SECURITY("SECURITY", "安全配置", "LockOutlined", "#722ed1");

    private final String code;
    private final String displayName;
    private final String icon;
    private final String color;

    ConfigGroup(String code, String displayName, String icon, String color) {
        this.code = code;
        this.displayName = displayName;
        this.icon = icon;
        this.color = color;
    }

    public static ConfigGroup fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ConfigGroup group : values()) {
            if (group.getCode().equals(code)) {
                return group;
            }
        }
        return null;
    }
}
