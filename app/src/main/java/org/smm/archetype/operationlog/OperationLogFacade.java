package org.smm.archetype.operationlog;

import org.smm.archetype.operationlog.internal.OperationLogPageQuery;
import org.smm.archetype.operationlog.internal.OperationLogVO;
import org.smm.archetype.shared.result.BasePageResult;

/**
 * 操作日志 Facade 接口
 * <p>
 * 提供给 Controller 层调用的统一门面，封装 Entity→VO 转换逻辑
 */
public interface OperationLogFacade {

    /**
     * 分页查询操作日志
     *
     * @param query 分页查询参数
     * @return 分页结果（OperationLogVO）
     */
    BasePageResult<OperationLogVO> findByPage(OperationLogPageQuery query);
}
