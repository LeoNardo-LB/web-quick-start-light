package org.smm.archetype.shared.result;

import io.opentelemetry.api.trace.Span;
import lombok.Getter;
import lombok.Setter;
import org.smm.archetype.exception.CommonErrorCode;
import org.smm.archetype.shared.pagination.PageResult;

import java.time.Instant;
import java.util.List;

/**
 * 分页结果包装（继承 BaseResult&lt;List&lt;T&gt;&gt;）。
 * <p>
 * 已去除 MyBatis-Plus {@code IPage} 依赖在公开 API 中的直接使用，
 * 仅通过 {@code from(PageResult)} 静态工厂构建。
 *
 * @param <T> 结果项类型
 */
@Getter
@Setter
public class BasePageResult<T> extends BaseResult<List<T>> {

    private long total;
    private int pageNo;
    private int pageSize;

    /**
     * 从 PageResult 构建 BasePageResult（框架无关，推荐使用）。
     *
     * @param pageResult PageResult 实例
     * @param <T>        结果项类型
     * @return BasePageResult 实例
     */
    public static <T> BasePageResult<T> from(PageResult<T> pageResult) {
        if (pageResult == null) {
            throw new IllegalArgumentException("pageResult must not be null");
        }
        BasePageResult<T> result = new BasePageResult<>();
        result.setData(pageResult.list());
        result.setTotal(pageResult.total());
        result.setPageNo(pageResult.pageNo());
        result.setPageSize(pageResult.pageSize());
        result.setCode(CommonErrorCode.SUCCESS.code());
        result.setMessage(CommonErrorCode.SUCCESS.message());
        result.setSuccess(true);
        result.setTime(Instant.now());
        result.setTraceId(Span.current().getSpanContext().getTraceId());
        return result;
    }


}
