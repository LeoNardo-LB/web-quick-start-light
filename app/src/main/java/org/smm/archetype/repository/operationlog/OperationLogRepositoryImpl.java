package org.smm.archetype.repository.operationlog;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.entity.operationlog.OperationLog;
import org.smm.archetype.entity.operationlog.OperationLogPageQuery;
import org.smm.archetype.generated.entity.OperationLogDO;
import org.smm.archetype.generated.mapper.OperationLogMapper;
import org.springframework.stereotype.Repository;

/**
 * 操作日志仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class OperationLogRepositoryImpl implements OperationLogRepository {

    private final OperationLogMapper operationLogMapper;
    private final OperationLogConverter converter;

    @Override
    public IPage<OperationLog> findByPage(OperationLogPageQuery query) {
        Page<OperationLogDO> page = new Page<>(query.pageNo(), query.pageSize());

        LambdaQueryWrapper<OperationLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(query.module()), OperationLogDO::getModule, query.module())
                .eq(StrUtil.isNotBlank(query.operationType()), OperationLogDO::getOperationType, query.operationType())
                .ge(StrUtil.isNotBlank(query.startTime()), OperationLogDO::getCreateTime, query.startTime())
                .le(StrUtil.isNotBlank(query.endTime()), OperationLogDO::getCreateTime, query.endTime())
                .orderByDesc(OperationLogDO::getId);

        IPage<OperationLogDO> doPage = operationLogMapper.selectPage(page, wrapper);

        // 转换为 Entity 分页
        Page<OperationLog> entityPage = new Page<>(doPage.getCurrent(), doPage.getSize(), doPage.getTotal());
        entityPage.setRecords(doPage.getRecords().stream()
                .map(converter::toEntity)
                .toList());
        return entityPage;
    }
}
