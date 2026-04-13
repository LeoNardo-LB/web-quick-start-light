package org.smm.archetype.entity.system;

import lombok.Getter;

/**
 * 输入类型枚举
 */
@Getter
public enum InputType {
    TEXT("TEXT"),
    TEXTAREA("TEXTAREA"),
    NUMBER("NUMBER"),
    SWITCH("SWITCH"),
    SELECT("SELECT"),
    MULTI_SELECT("MULTI_SELECT"),
    JSON_EDITOR("JSON_EDITOR");

    private final String code;

    InputType(String code) {
        this.code = code;
    }

    public static InputType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (InputType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
