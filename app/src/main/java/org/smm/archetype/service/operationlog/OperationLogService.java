package org.smm.archetype.service.operationlog;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.smm.archetype.entity.operationlog.OperationLog;
import org.smm.archetype.entity.operationlog.OperationLogPageQuery;
import org.smm.archetype.repository.operationlog.OperationLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 操作日志服务
 */
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;

    @Transactional(readOnly = true)
    public IPage<OperationLog> findByPage(OperationLogPageQuery query) {
        return operationLogRepository.findByPage(query);
    }
}
