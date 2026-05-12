package org.smm.archetype.operationlog.internal;

/**
 * 操作日志 DO → Model 转换器
 */
class OperationLogConverter {

    OperationLog toModel(OperationLogDO logDO) {
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
