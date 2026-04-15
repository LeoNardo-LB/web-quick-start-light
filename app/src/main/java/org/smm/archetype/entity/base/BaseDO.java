package org.smm.archetype.entity.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * @author Leonardo
 * @since 2025/7/14
 * 基础数据对象（Data Object 简称 do）
 */
@Getter
@Setter
public abstract class BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Instant createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updateTime;

    @TableField(fill = FieldFill.INSERT)
    private String createUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateUser;

    /**
     * 逻辑删除标记：0 = 未删除，非 0 = 删除时间戳。
     */
    private Long deleteTime;

    /**
     * 删除人ID。
     */
    private String deleteUser;

}
