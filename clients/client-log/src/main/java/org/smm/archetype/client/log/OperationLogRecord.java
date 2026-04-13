package org.smm.archetype.client.log;

/**
 * 操作日志记录，用于在 LogAspect 和 OperationLogWriter 之间传递日志数据。
 */
public record OperationLogRecord(
        String traceId,
        String userId,
        String module,
        String operationType,
        String description,
        String method,
        String params,
        String result,
        long executionTime,
        String ip,
        String status,
        String errorMessage
) {}
