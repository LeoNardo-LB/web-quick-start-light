package org.smm.archetype.entity.operationlog;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 操作日志分页查询参数
 *
 * @param pageNo        当前页码
 * @param pageSize      每页大小
 * @param module        模块名称（可选过滤）
 * @param operationType 操作类型（可选过滤）
 * @param startTime     开始时间（可选，ISO 8601 格式）
 * @param endTime       结束时间（可选，ISO 8601 格式）
 */
public record OperationLogPageQuery(
        @Min(1) int pageNo,
        @Min(1) @Max(100) int pageSize,
        String module,
        String operationType,
        String startTime,
        String endTime
) {
}
