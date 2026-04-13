package org.smm.archetype.entity.operationlog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 操作日志领域实体
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLog {

    private Long id;
    private String traceId;
    private String userId;
    private String module;
    private String operationType;
    private String description;
    private String method;
    private String params;
    private String result;
    private Long executionTime;
    private String ip;
    private String status;
    private String errorMessage;
    private String createTime;
}
