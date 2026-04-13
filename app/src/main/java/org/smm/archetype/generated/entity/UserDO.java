package org.smm.archetype.generated.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户 DO（Data Object）。
 * <p>
 * 对应数据库 user 表，由代码生成器生成或手动维护。
 *
 * @since 2026/4/13
 */
@Getter
@Setter
@TableName("user")
public class UserDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String passwordHash;

    private String nickname;

    private String status;

    private String createTime;

    private String updateTime;

    private String createUser;

    private String updateUser;
}
