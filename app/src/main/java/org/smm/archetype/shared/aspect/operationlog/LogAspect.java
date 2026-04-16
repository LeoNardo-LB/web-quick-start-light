package org.smm.archetype.shared.aspect.operationlog;

import com.alibaba.fastjson2.JSON;
import io.opentelemetry.api.trace.Span;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @BusinessLog 注解的 AOP 切面。
 * <p>
 * 职责：
 * <ul>
 *   <li>记录业务方法执行日志（SLF4J）</li>
 *   <li>创建 OperationLogRecord（traceId 从 OTel Span 获取）</li>
 *   <li>调用 OperationLogWriter.write()（如果 writer 存在）</li>
 *   <li>执行采样率过滤（samplingRate 属性）</li>
 * </ul>
 * <p>
 * 注意：Micrometer Timer/Counter 指标已移除，由 OTel 自动 instrumentation 覆盖。
 */
@Aspect
public class LogAspect {

    private static final int                   MAX_LENGTH       = 2048;
    private static final String                TRUNCATED_SUFFIX = "...(truncated)";
    private static final Map<Class<?>, Logger> LOGGER_MAP       = new ConcurrentHashMap<>();

    private final OperationLogWriter operationLogWriter;

    /**
     * 兼容旧版构造（无 OperationLogWriter）。
     */
    public LogAspect() {
        this(null);
    }

    /**
     * 新版构造（支持 OperationLogWriter）。
     */
    public LogAspect(OperationLogWriter operationLogWriter) {
        this.operationLogWriter = operationLogWriter;
    }

    private static String toSafeJson(Object obj) {
        if (obj == null)
            return "null";
        try {
            String json = JSON.toJSONString(obj);
            if (json.length() > MAX_LENGTH) {
                return json.substring(0, MAX_LENGTH - TRUNCATED_SUFFIX.length()) + TRUNCATED_SUFFIX;
            }
            return json;
        } catch (Exception e) {
            return obj.getClass().getSimpleName() + "@" + Integer.toHexString(obj.hashCode());
        }
    }

    @Pointcut("@annotation(org.smm.archetype.shared.aspect.operationlog.BusinessLog)")
    public void logCut() {}

    @Around("logCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        BusinessLog businessLog = signature.getMethod().getAnnotation(BusinessLog.class);
        Class<?> declaringType = signature.getDeclaringType();
        String methodName = signature.getMethod().getName();
        long startTime = System.currentTimeMillis();
        String businessDesc = businessLog != null ? businessLog.value() : "-";

        Logger logger = LOGGER_MAP.computeIfAbsent(declaringType, k -> LoggerFactory.getLogger(k));

        String status = "SUCCESS";
        String errorMessage = "";
        Object result = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            status = "ERROR";
            errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw e;
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;

            if (status.equals("SUCCESS")) {
                logger.info("[方法执行] {}#{} | {} | {}ms | {} | {} | {}",
                        declaringType.getSimpleName(), methodName, businessDesc,
                        durationMs, Thread.currentThread().getName(),
                        toSafeJson(joinPoint.getArgs()), toSafeJson(result));
            } else {
                logger.error("[方法执行] {}#{} | {} | {}ms | {} | {} | ERROR",
                        declaringType.getSimpleName(), methodName, businessDesc,
                        durationMs, Thread.currentThread().getName(),
                        toSafeJson(joinPoint.getArgs()), errorMessage);
            }

            // 异步写入操作日志
            writeOperationLog(businessLog, declaringType, methodName,
                    joinPoint.getArgs(), result, durationMs, status, errorMessage);
        }
    }

    private void writeOperationLog(BusinessLog businessLog, Class<?> declaringType,
                                   String methodName, Object[] args, Object result,
                                   long durationMs, String status, String errorMessage) {
        if (operationLogWriter == null || businessLog == null) {
            return;
        }

        double samplingRate = businessLog.samplingRate();
        if (samplingRate < 1.0 && ThreadLocalRandom.current().nextDouble() >= samplingRate) {
            return;
        }

        String operationType = businessLog.operation().code();
        String module = businessLog.module();
        String description = businessLog.value();

        OperationLogRecord record = new OperationLogRecord(
                Span.current().getSpanContext().getTraceId(), // traceId - 从 OTel Span 获取
                "", // userId - 由 app 模块的 writer 填充
                module,
                operationType,
                description,
                declaringType.getSimpleName() + "#" + methodName,
                toSafeJson(args),
                toSafeJson(result),
                durationMs,
                "", // ip - 由 app 模块的 writer 填充
                status,
                errorMessage
        );

        try {
            operationLogWriter.write(record);
        } catch (Exception e) {
            Logger logger = LOGGER_MAP.getOrDefault(declaringType, LoggerFactory.getLogger(LogAspect.class));
            logger.warn("[操作日志写入失败] {} | {}", record.method(), e.getMessage());
        }
    }

}
