package ${modulePackage}.internal;

import org.springframework.stereotype.Component;

/**
 * ${tableComment} DO↔Model 转换器
 * <p>
 * 自动生成，禁止手动修改。
 */
@Component
class ${entityName}Converter {

    ${entityName} toModel(${entityName}DO dataObject) {
        if (dataObject == null) {
            return null;
        }
        ${entityName} model = new ${entityName}();
        model.setId(dataObject.getId());
<#list columns as col>
    <#if col.name != "id" && col.name != "createTime" && col.name != "updateTime"
         && col.name != "createUser" && col.name != "updateUser"
         && col.name != "deleteTime" && col.name != "deleteUser">
        model.set${col.capitalizedName}(dataObject.get${col.capitalizedName}());
    </#if>
</#list>
        model.setCreateTime(dataObject.getCreateTime());
        model.setUpdateTime(dataObject.getUpdateTime());
        model.setCreateUser(dataObject.getCreateUser());
        model.setUpdateUser(dataObject.getUpdateUser());
        return model;
    }

    ${entityName}DO toDO(${entityName} model) {
        if (model == null) {
            return null;
        }
        ${entityName}DO dataObject = new ${entityName}DO();
        dataObject.setId(model.getId());
<#list columns as col>
    <#if col.name != "id" && col.name != "createTime" && col.name != "updateTime"
         && col.name != "createUser" && col.name != "updateUser"
         && col.name != "deleteTime" && col.name != "deleteUser">
        dataObject.set${col.capitalizedName}(model.get${col.capitalizedName}());
    </#if>
</#list>
        return dataObject;
    }
}
