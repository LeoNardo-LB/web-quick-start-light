package org.smm.archetype.entity.base;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.opentelemetry.api.trace.Span;
import lombok.Getter;
import lombok.Setter;
import org.smm.archetype.exception.CommonErrorCode;

import java.time.Instant;
import java.util.List;

/**
 * @param <T> 结果类型
 * @author Leonardo
 * @since 2025/7/14
 * 分页结果
 */
@Getter
@Setter
public class BasePageResult<T> extends BaseResult<List<T>> {
    
    /**
     * 总数
     */
    private long total;
    
    /**
     * 当前页
     */
    private int pageNo;
    
    /**
     * 页大小
     */
    private int pageSize;
    
    /**
     * 从 MyBatis-Plus 分页结果转换成 BasePageResult
     *
     * @param page MyBatis-Plus 分页结果
     * @param <T>  泛型
     * @return BasePageResult
     */
    public static <T> BasePageResult<T> fromPage(IPage<T> page) {
        BasePageResult<T> basePageResult = new BasePageResult<>();
        basePageResult.setTotal(page.getTotal());
        basePageResult.setPageNo((int) page.getCurrent());
        basePageResult.setPageSize((int) page.getSize());
        basePageResult.setCode(CommonErrorCode.SUCCESS.code());
        basePageResult.setMessage(CommonErrorCode.SUCCESS.message());
        basePageResult.setData(page.getRecords());
        basePageResult.setSuccess(true);
        basePageResult.setTime(Instant.now());
        basePageResult.setTraceId(Span.current().getSpanContext().getTraceId());
        return basePageResult;
    }
    
}
