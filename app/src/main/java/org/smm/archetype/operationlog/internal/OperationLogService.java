package org.smm.archetype.operationlog.internal;

import lombok.RequiredArgsConstructor;
import org.smm.archetype.shared.pagination.PageResult;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务
 */
@Service
@RequiredArgsConstructor
class OperationLogService {

    private final OperationLogRepository operationLogRepository;

    public PageResult<OperationLog> findByPage(OperationLogPageQuery query) {
        return operationLogRepository.findByPage(query);
    }
}
