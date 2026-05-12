package org.smm.archetype.entity.base;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * @deprecated 被 {@code org.smm.archetype.shared.pagination.PageQuery} record 替代。
 *             record 无法继承 class，设计为独立 record + 紧凑构造器模式。将在阶段 4 删除。
 * @see org.smm.archetype.shared.pagination.PageQuery
 * @author Leonardo
 * @since 2025/7/14
 * 基础分页请求对象
 */
@Deprecated
@Getter
@Setter
public class BasePageRequest extends BaseRequest {
    
    /**
     * 当前页
     */
    @Min(1)
    private int pageNo = 1;
    
    /**
     * 页大小
     */
    @Min(1)
    @Max(100)
    private int pageSize = 20;
    
}
