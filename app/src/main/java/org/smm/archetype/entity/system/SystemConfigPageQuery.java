package org.smm.archetype.entity.system;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 系统配置分页查询参数
 */
public record SystemConfigPageQuery(
        @Min(1)
        Integer pageNo,
        @Min(1)
        @Max(100)
        Integer pageSize,
        String groupCode
) {

    /**
     * 紧凑构造器，为缺失的分页参数设置默认值
     */
    public SystemConfigPageQuery {
        if (pageNo == null || pageNo <= 0) {
            pageNo = 1;
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = 20;
        }
    }
}
