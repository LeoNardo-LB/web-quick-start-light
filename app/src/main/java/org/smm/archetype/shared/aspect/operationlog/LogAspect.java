package org.smm.archetype.shared.aspect.operationlog;

import com.alibaba.fastjson2.JSON;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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

@Aspect
public class LogAspect {

    private static final int                   MAX_LENGTH       = 2048;
    private static final String                TRUNCATED_SUFFIX = "...(truncated)";
    private static final Map<Class<?>, Logger> LOGGER_MAP       = new ConcurrentHashMap<>();

    private final    MeterRegistry      meterRegistry;
    private final    OperationLogWriter operationLogWriter;
    private volatile Timer              executionTimer;
    private volatile Counter            executionCounter;
    private volatile Counter            errorCounter;

    /**
     * 兼容旧版构造（无 OperationLogWriter）。
     */
    public LogAspect(MeterRegistry meterRegistry) {
        this(meterRegistry, null);
    }

    /**
     * 新版构造（支持 OperationLogWriter）。
     */
    public LogAspect(MeterRegistry meterRegistry, OperationLogWriter operationLogWriter) {
        this.meterRegistry = meterRegistry;
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

    private void initIfNecessary() {
        if (executionTimer == null) {
            this.executionTimer = Timer.builder("log_aspect_timer_seconds")
                                          .description("LogAspect method execution time")
                                          .register(meterRegistry);
            this.executionCounter = Counter.builder("log_aspect_counter_total")
                                            .description("Total method executions")
                                            .register(meterRegistry);
            this.errorCounter = Counter.builder("log_aspect_errors_total")
                                        .description("Total errors in methods")
                                        .register(meterRegistry);
        }
    }

    @Pointcut("@annotation(org.smm.archetype.shared.aspect.operationlog.BusinessLog)")
    public void logCut() {}

    @Around("logCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        initIfNecessary();
        Timer.Sample timerSample = Timer.start();
        executionCounter.increment();

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
            errorCounter.increment();
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

            timerSample.stop(executionTimer);

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
                "", // traceId - 由 app 模块的 writer 填充
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
