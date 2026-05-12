package ${modulePackage}.internal;

import java.time.Instant;

/**
 * ${tableComment} VO
 * <p>
 * 自动生成，禁止手动修改。
 */
public class ${entityName}VO {

    private String id;
<#list columns as col>
    <#if col.name != "id" && col.name != "createTime" && col.name != "updateTime"
         && col.name != "createUser" && col.name != "updateUser"
         && col.name != "deleteTime" && col.name != "deleteUser">
    private ${col.type} ${col.fieldName};
    </#if>
</#list>
    private Instant createTime;
    private Instant updateTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
<#list columns as col>
    <#if col.name != "id" && col.name != "createTime" && col.name != "updateTime"
         && col.name != "createUser" && col.name != "updateUser"
         && col.name != "deleteTime" && col.name != "deleteUser">

    public ${col.type} get${col.capitalizedName}() {
        return ${col.fieldName};
    }

    public void set${col.capitalizedName}(${col.type} ${col.fieldName}) {
        this.${col.fieldName} = ${col.fieldName};
    }
    </#if>
</#list>

    public Instant getCreateTime() { return createTime; }
    public void setCreateTime(Instant createTime) { this.createTime = createTime; }
    public Instant getUpdateTime() { return updateTime; }
    public void setUpdateTime(Instant updateTime) { this.updateTime = updateTime; }
}
