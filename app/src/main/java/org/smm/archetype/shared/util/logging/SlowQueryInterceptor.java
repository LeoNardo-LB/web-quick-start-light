package org.smm.archetype.shared.util.logging;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.smm.archetype.config.properties.LoggingProperties;

import java.util.Properties;

@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class})
})
@Slf4j
public class SlowQueryInterceptor implements Interceptor {

    private static final Logger                      SLOW_QUERY_LOG = LoggerFactory.getLogger("SLOW_QUERY");
    private final        LoggingProperties.SlowQuery config;

    public SlowQueryInterceptor(LoggingProperties.SlowQuery config) {
        this.config = config;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!config.isEnabled())
            return invocation.proceed();
        long startTime = System.currentTimeMillis();
        try {
            Object result = invocation.proceed();
            long durationMs = System.currentTimeMillis() - startTime;
            if (durationMs >= config.getThresholdMs()) {
                MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
                Object parameter = invocation.getArgs()[1];
                String traceId = MDC.get("traceId");
                SLOW_QUERY_LOG.warn("[SLOW QUERY] SQL executed in {}ms | traceId={} | sql={} | params={}",
                        durationMs, traceId, ms.getId(), parameter);
            }
            return result;
        } finally {
            MDC.remove("sql");
            MDC.remove("sqlParams");
        }
    }

    @Override
    public Object plugin(Object target) {return Plugin.wrap(target, this);}

    @Override
    public void setProperties(Properties properties) {}

}
