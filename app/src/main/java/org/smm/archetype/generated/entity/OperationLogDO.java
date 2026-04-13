package org.smm.archetype.generated.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 操作日志数据对象。
 * <p>
 * 注意：此实体不继承 BaseDO（字段结构不同，无 updateUser/createUser）。
 */
@Getter
@Setter
@TableName("operation_log")
public class OperationLogDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String traceId;
    private String userId;
    private String module;
    private String operationType;
    private String description;
    private String method;
    private String params;
    private String result;
    private Long executionTime;
    private String ip;
    private String status;
    private String errorMessage;
    private String createTime;
}
