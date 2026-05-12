package org.smm.archetype.operationlog.internal;

import lombok.RequiredArgsConstructor;
import org.smm.archetype.operationlog.OperationLogFacade;
import org.smm.archetype.shared.pagination.PageResult;
import org.smm.archetype.shared.result.BasePageResult;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志 Facade 实现
 * <p>
 * 封装 Service 调用，负责 Entity→VO 转换
 */
@Service
@RequiredArgsConstructor
class OperationLogFacadeImpl implements OperationLogFacade {

    private final OperationLogService operationLogService;

    @Override
    public BasePageResult<OperationLogVO> findByPage(OperationLogPageQuery query) {
        PageResult<OperationLog> pageResult = operationLogService.findByPage(query);

        List<OperationLogVO> voList = pageResult.list().stream()
                .map(this::toVO)
                .toList();

        return BasePageResult.from(new PageResult<>(voList, pageResult.total(), pageResult.pageNo(),
                pageResult.pageSize(), pageResult.totalPages()));
    }

    private OperationLogVO toVO(OperationLog log) {
        return new OperationLogVO(
                log.getId(),
                log.getTraceId(),
                log.getUserId(),
                log.getModule(),
                log.getOperationType(),
                log.getDescription(),
                log.getMethod(),
                log.getParams(),
                log.getResult(),
                log.getExecutionTime(),
                log.getIp(),
                log.getStatus(),
                log.getErrorMessage(),
                log.getCreateTime()
        );
    }
}
