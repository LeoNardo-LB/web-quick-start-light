package org.smm.archetype.shared;

import org.mapstruct.MapperConfig;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct 全局配置
 * <p>
 * 所有 Converter 接口通过 config = CentralMapperConfig.class 引用此配置。
 * 使用 componentModel = SPRING 让 MapStruct 自动生成 @Component 实现类。
 */
@MapperConfig(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface CentralMapperConfig {
}
