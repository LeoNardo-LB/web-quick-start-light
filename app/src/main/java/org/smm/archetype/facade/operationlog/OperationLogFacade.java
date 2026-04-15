package org.smm.archetype.facade.operationlog;

import org.smm.archetype.entity.base.BasePageResult;
import org.smm.archetype.entity.operationlog.OperationLogPageQuery;

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
