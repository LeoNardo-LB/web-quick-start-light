package org.smm.archetype.auth.internal;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
interface UserMapper extends BaseMapper<UserDO> {
}
