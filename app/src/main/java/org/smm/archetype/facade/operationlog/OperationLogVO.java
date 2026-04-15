package org.smm.archetype.facade.operationlog;

import java.time.Instant;

/**
 * 操作日志 VO — 用于前端展示
 *
 * @param id            日志 ID
 * @param traceId       追踪 ID
 * @param userId        操作用户
 * @param module        模块
 * @param operationType 操作类型
 * @param description   描述
 * @param method        方法名
 * @param params        请求参数
 * @param result        返回结果
 * @param executionTime 执行时间（ms）
 * @param ip            IP 地址
 * @param status        状态
 * @param errorMessage  错误信息
 * @param createTime    创建时间
 */
public record OperationLogVO(
        Long id,
        String traceId,
        String userId,
        String module,
        String operationType,
        String description,
        String method,
        String params,
        String result,
        Long executionTime,
        String ip,
        String status,
        String errorMessage,
        Instant createTime
) {}
