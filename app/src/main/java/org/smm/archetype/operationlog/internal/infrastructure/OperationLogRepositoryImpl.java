package org.smm.archetype.operationlog.internal.infrastructure;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.smm.archetype.operationlog.internal.OperationLog;
import org.smm.archetype.operationlog.internal.OperationLogPageQuery;
import org.smm.archetype.operationlog.internal.OperationLogRepository;
import org.smm.archetype.shared.pagination.PageResult;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * 操作日志仓储实现
 */
@Repository
@RequiredArgsConstructor
class OperationLogRepositoryImpl implements OperationLogRepository {

    private final OperationLogMapper operationLogMapper;
    private final OperationLogConverter operationLogConverter;

    @Override
    public PageResult<OperationLog> findByPage(OperationLogPageQuery query) {
        Page<OperationLogDO> page = new Page<>(query.pageNo(), query.pageSize());

        LambdaQueryWrapper<OperationLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(query.module()), OperationLogDO::getModule, query.module())
                .eq(StrUtil.isNotBlank(query.operationType()), OperationLogDO::getOperationType, query.operationType())
                .ge(StrUtil.isNotBlank(query.startTime()), OperationLogDO::getCreateTime,
                        parseInstant(query.startTime()))
                .le(StrUtil.isNotBlank(query.endTime()), OperationLogDO::getCreateTime,
                        parseInstant(query.endTime()))
                .orderByDesc(OperationLogDO::getId);

        var doPage = operationLogMapper.selectPage(page, wrapper);

        List<OperationLog> entities = doPage.getRecords().stream()
                .map(operationLogConverter::toModel)
                .toList();

        return PageResult.of(entities, doPage.getTotal(), (int) doPage.getCurrent(), (int) doPage.getSize());
    }

    /**
     * 将 ISO-8601 格式时间字符串解析为 Instant。
     */
    private Instant parseInstant(String dateTime) {
        if (dateTime == null || dateTime.isBlank()) {
            return null;
        }
        return Instant.parse(dateTime);
    }
}
