package org.smm.archetype.operationlog.internal.infrastructure;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.smm.archetype.shared.dal.BaseDO;

/**
 * 操作日志数据对象（DO）
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("operation_log")
public class OperationLogDO extends BaseDO {
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
}
