package org.smm.archetype.shared.pagination;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 分页请求值对象（1-based，前端友好）。
 * <p>
 * 各模块的 *PageQuery record 通过紧凑构造器复用此类的校验逻辑。
 */
public record PageQuery(
        @Min(1) int pageNo,
        @Min(1) @Max(100) int pageSize
) {
    public PageQuery {
        if (pageNo <= 0) pageNo = 1;
        if (pageSize <= 0) pageSize = 10;
        if (pageSize > 100) pageSize = 100;
    }
}
