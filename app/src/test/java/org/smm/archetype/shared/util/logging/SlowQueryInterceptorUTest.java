package org.smm.archetype.shared.util.logging;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Invocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.MDC;
import org.smm.archetype.config.properties.LoggingProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SlowQueryInterceptor 慢查询拦截器")
class SlowQueryInterceptorUTest {

    @Mock
    private MappedStatement mappedStatement;

    @Mock
    private Invocation invocation;

    private LoggingProperties.SlowQuery config;
    private SlowQueryInterceptor        interceptor;

    @BeforeEach
    void setUp() {
        config = new LoggingProperties.SlowQuery();
    }

    @Nested
    @DisplayName("config 禁用时")
    class WhenDisabled {

        @BeforeEach
        void setUp() {
            config.setEnabled(false);
            config.setThresholdMs(1000L);
            interceptor = new SlowQueryInterceptor(config);
        }

        @Test
        @DisplayName("直接委托给 invocation.proceed()，不计时")
        void should_delegate_directly_without_timing() throws Throwable {
            when(invocation.proceed()).thenReturn("result");

            Object result = interceptor.intercept(invocation);

            assertThat(result).isEqualTo("result");
            verify(invocation, times(1)).proceed();
        }

    }

    @Nested
    @DisplayName("config 启用时")
    class WhenEnabled {

        @BeforeEach
        void setUp() {
            config.setEnabled(true);
            config.setThresholdMs(1000L);
            interceptor = new SlowQueryInterceptor(config);
        }

        @Test
        @DisplayName("快速查询（低于阈值）不记录慢查询日志")
        void should_not_log_fast_query() throws Throwable {
            when(invocation.proceed()).thenReturn("result");

            Object result = interceptor.intercept(invocation);

            assertThat(result).isEqualTo("result");
            // 没有进入慢查询分支，不访问 MappedStatement
            verify(mappedStatement, never()).getId();
        }

        @Test
        @DisplayName("慢查询（超过阈值）记录慢查询日志")
        void should_log_slow_query_when_exceeds_threshold() throws Throwable {
            config.setThresholdMs(0L); // 阈值为 0，任何查询都是慢查询
            interceptor = new SlowQueryInterceptor(config);

            when(invocation.getArgs()).thenReturn(new Object[] {mappedStatement, "param"});
            when(invocation.proceed()).thenReturn("result");
            when(mappedStatement.getId()).thenReturn("com.example.UserMapper.selectById");

            Object result = interceptor.intercept(invocation);

            assertThat(result).isEqualTo("result");
            verify(mappedStatement, atLeastOnce()).getId();
        }

        @Test
        @DisplayName("finally 块始终清理 MDC")
        void should_always_cleanup_mdc_in_finally() throws Throwable {
            MDC.put("sql", "SELECT * FROM users");
            MDC.put("sqlParams", "{id=1}");

            when(invocation.proceed()).thenReturn("result");

            interceptor.intercept(invocation);

            assertThat(MDC.get("sql")).isNull();
            assertThat(MDC.get("sqlParams")).isNull();
        }

        @Test
        @DisplayName("查询抛异常时仍然清理 MDC")
        void should_cleanup_mdc_even_when_exception_thrown() throws Throwable {
            MDC.put("sql", "SELECT * FROM users");
            MDC.put("sqlParams", "{id=1}");

            when(invocation.proceed()).thenThrow(new RuntimeException("DB error"));

            org.assertj.core.api.Assertions.assertThatThrownBy(() -> interceptor.intercept(invocation))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB error");

            assertThat(MDC.get("sql")).isNull();
            assertThat(MDC.get("sqlParams")).isNull();
        }

    }

    @Nested
    @DisplayName("plugin() 和 setProperties()")
    class PluginAndProperties {

        @Test
        @DisplayName("plugin() 应包装目标对象")
        void should_wrap_target_in_plugin() {
            config.setEnabled(true);
            interceptor = new SlowQueryInterceptor(config);

            Object target = mock(Executor.class);
            Object wrapped = interceptor.plugin(target);

            assertThat(wrapped).isNotSameAs(target);
        }

        @Test
        @DisplayName("setProperties() 应为空操作")
        void should_set_properties_be_noop() {
            config.setEnabled(true);
            interceptor = new SlowQueryInterceptor(config);

            // 不应抛出异常
            interceptor.setProperties(new java.util.Properties());
        }

    }

}
