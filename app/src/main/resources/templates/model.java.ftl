package ${modulePackage}.internal;

import java.time.Instant;

/**
 * ${tableComment}领域模型
 * <p>
 * 自动生成，禁止手动修改。
 */
public class ${entityName} {

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
    private String createUser;
    private String updateUser;

    public ${entityName}() {
    }

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

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Instant getCreateTime() { return createTime; }
    public void setCreateTime(Instant createTime) { this.createTime = createTime; }
    public Instant getUpdateTime() { return updateTime; }
    public void setUpdateTime(Instant updateTime) { this.updateTime = updateTime; }
    public String getCreateUser() { return createUser; }
    public void setCreateUser(String createUser) { this.createUser = createUser; }
    public String getUpdateUser() { return updateUser; }
    public void setUpdateUser(String updateUser) { this.updateUser = updateUser; }
}
