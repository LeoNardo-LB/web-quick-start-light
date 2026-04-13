package org.smm.archetype.repository.operationlog;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.smm.archetype.entity.operationlog.OperationLog;
import org.smm.archetype.entity.operationlog.OperationLogPageQuery;

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
    IPage<OperationLog> findByPage(OperationLogPageQuery query);
}
