package org.smm.archetype.client.log;

/**
 * 操作类型枚举，用于标注业务日志的操作分类。
 */
public enum OperationType {

    CREATE("CREATE", "新增"),
    UPDATE("UPDATE", "修改"),
    DELETE("DELETE", "删除"),
    QUERY("QUERY", "查询"),
    EXPORT("EXPORT", "导出"),
    IMPORT("IMPORT", "导入");

    private final String code;
    private final String description;

    OperationType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String code() {
        return code;
    }

    public String description() {
        return description;
    }
}
