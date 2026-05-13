package org.smm.archetype.operationlog.internal;

import org.smm.archetype.shared.pagination.PageResult;

/**
 * 操作日志仓储接口
 */
public interface OperationLogRepository {

    /**
     * 分页查询操作日志
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    PageResult<OperationLog> findByPage(OperationLogPageQuery query);
}
