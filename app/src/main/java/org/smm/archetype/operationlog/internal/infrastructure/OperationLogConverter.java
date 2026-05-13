package org.smm.archetype.operationlog.internal.infrastructure;

import org.mapstruct.Mapper;
import org.smm.archetype.operationlog.internal.OperationLog;
import org.smm.archetype.shared.CentralMapperConfig;

/**
 * 操作日志 DO → Model 转换器（MapStruct 生成实现）
 */
@Mapper(config = CentralMapperConfig.class)
interface OperationLogConverter {

    OperationLog toModel(OperationLogDO logDO);
}
