package org.smm.archetype.entity.api;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Leonardo
 * @since 2025/7/14
 * 基础请求
 */
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
