package org.smm.archetype.shared.pagination;

import java.util.Collections;
import java.util.List;

/**
 * 分页结果值对象（框架无关，泛型）。
 * <p>
 * 替代 MyBatis-Plus {@code IPage<T>} 作为 Repository 接口的返回类型。
 */
public record PageResult<T>(
        List<T> list,
        long total,
        int pageNo,
        int pageSize,
        int totalPages
) {
    public static <T> PageResult<T> of(List<T> list, long total, int pageNo, int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive, got: " + pageSize);
        }
        int totalPages = (int) Math.ceil((double) total / pageSize);
        return new PageResult<>(list, total, pageNo, pageSize, totalPages);
    }

    public static <T> PageResult<T> empty(int pageNo, int pageSize) {
        return new PageResult<>(Collections.emptyList(), 0, pageNo, pageSize, 0);
    }
}
