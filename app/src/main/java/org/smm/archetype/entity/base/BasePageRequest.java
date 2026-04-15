package org.smm.archetype.entity.base;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Leonardo
 * @since 2025/7/14
 * 基础分页请求对象
 */
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
