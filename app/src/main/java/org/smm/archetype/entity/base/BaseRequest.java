package org.smm.archetype.entity.base;

import lombok.Getter;
import lombok.Setter;

/**
 * @deprecated traceId 由 OTel Span 自动管理，requestId 由 Filter 生成。
 *             请求 record 只需包含业务字段，不再需要此基类。将在阶段 4 删除。
 * @author Leonardo
 * @since 2025/7/14
 * 基础请求
 */
@Deprecated
@Getter
@Setter
public class BaseRequest {
    
    /**
     * 请求序列号
     */
    private String requestId;
    
    /**
     * traceId
     */
    private String traceId;
    
}
