package org.smm.archetype.auth.internal.infrastructure;

import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.smm.archetype.auth.internal.User;
import org.smm.archetype.shared.CentralMapperConfig;

/**
 * 用户 DO ↔ Model 转换器（MapStruct 生成实现）
 */
@Mapper(config = CentralMapperConfig.class)
interface UserConverter {

    User toModel(UserDO userDO);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createUser", ignore = true)
    @Mapping(target = "updateUser", ignore = true)
    @Mapping(target = "deleteTime", ignore = true)
    @Mapping(target = "deleteUser", ignore = true)
    UserDO toDO(User user);
}
