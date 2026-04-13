package org.smm.archetype.repository.operationlog;

import org.smm.archetype.entity.operationlog.OperationLog;
import org.smm.archetype.generated.entity.OperationLogDO;
import org.springframework.stereotype.Component;

/**
 * 操作日志 DO → Entity 转换器
 */
@Component
public class OperationLogConverter {

    public OperationLog toEntity(OperationLogDO logDO) {
        if (logDO == null) {
            return null;
        }
        return OperationLog.builder()
                .id(logDO.getId())
                .traceId(logDO.getTraceId())
                .userId(logDO.getUserId())
                .module(logDO.getModule())
                .operationType(logDO.getOperationType())
                .description(logDO.getDescription())
                .method(logDO.getMethod())
                .params(logDO.getParams())
                .result(logDO.getResult())
                .executionTime(logDO.getExecutionTime())
                .ip(logDO.getIp())
                .status(logDO.getStatus())
                .errorMessage(logDO.getErrorMessage())
                .createTime(logDO.getCreateTime())
                .build();
    }
}
