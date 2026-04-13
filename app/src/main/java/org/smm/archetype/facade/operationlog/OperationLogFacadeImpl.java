package org.smm.archetype.facade.operationlog;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.smm.archetype.entity.api.BasePageResult;
import org.smm.archetype.entity.operationlog.OperationLog;
import org.smm.archetype.entity.operationlog.OperationLogPageQuery;
import org.smm.archetype.exception.CommonErrorCode;
import org.smm.archetype.service.operationlog.OperationLogService;
import org.smm.archetype.util.context.ScopedThreadContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * 操作日志 Facade 实现
 * <p>
 * 封装 Service 调用，负责 Entity→VO 转换
 */
@Service
@RequiredArgsConstructor
public class OperationLogFacadeImpl implements OperationLogFacade {

    private final OperationLogService operationLogService;

    @Override
    public BasePageResult<OperationLogVO> findByPage(OperationLogPageQuery query) {
        IPage<OperationLog> entityPage = operationLogService.findByPage(query);

        List<OperationLogVO> voList = entityPage.getRecords().stream()
                .map(this::toVO)
                .toList();

        BasePageResult<OperationLogVO> result = new BasePageResult<>();
        result.setTotal(entityPage.getTotal());
        result.setPageNo((int) entityPage.getCurrent());
        result.setPageSize((int) entityPage.getSize());
        result.setCode(CommonErrorCode.SUCCESS.code());
        result.setMessage(CommonErrorCode.SUCCESS.message());
        result.setData(voList);
        result.setSuccess(true);
        result.setTime(Instant.now());
        result.setTraceId(ScopedThreadContext.getTraceId());
        return result;
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
