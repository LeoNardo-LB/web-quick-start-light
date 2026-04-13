package org.smm.archetype.entity.system;

import lombok.Getter;

/**
 * 配置值类型枚举
 */
@Getter
public enum ValueType {
    STRING("STRING"),
    INTEGER("INTEGER"),
    DECIMAL("DECIMAL"),
    BOOLEAN("BOOLEAN"),
    ENUM("ENUM"),
    ARRAY("ARRAY"),
    JSON("JSON");

    private final String code;

    ValueType(String code) {
        this.code = code;
    }

    public static ValueType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ValueType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
