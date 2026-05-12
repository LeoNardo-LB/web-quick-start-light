package org.smm.archetype.systemconfig.internal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.smm.archetype.shared.pagination.PageQuery;

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
     * 兼容无参绑定场景（Spring MVC @ModelAttribute）
     */
    public SystemConfigPageQuery() {
        this(null, null, null);
    }

    /**
     * 紧凑构造器，委托 PageQuery 做基础分页校验，为缺失的分页参数设置默认值
     */
    public SystemConfigPageQuery {
        PageQuery base = new PageQuery(
                pageNo == null || pageNo <= 0 ? 1 : pageNo,
                pageSize == null || pageSize <= 0 ? 20 : pageSize
        );
        pageNo = base.pageNo();
        pageSize = base.pageSize();
    }
}
