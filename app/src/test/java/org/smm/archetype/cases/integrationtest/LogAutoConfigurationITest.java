package org.smm.archetype.cases.integrationtest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.client.log.LogAspect;
import org.smm.archetype.support.IntegrationTestBase;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L3 集成测试 — 验证 client-log starter 的 LogAutoConfiguration 自动装配。
 * <p>
 * 覆盖能力: client-log-starter
 */
@DisplayName("LogAutoConfiguration")
class LogAutoConfigurationITest extends IntegrationTestBase {

    @Test
    @DisplayName("MFT: LogAspect Bean 已通过 LogAutoConfiguration 自动注册")
    void should_logAspectBeanBeRegistered() {
        LogAspect logAspect = applicationContext.getBean(LogAspect.class);
        assertThat(logAspect).isNotNull();
    }

    @Test
    @DisplayName("DIR: SlowQueryInterceptor 默认未注册（logging.slow-query.enabled=false）")
    void should_slowQueryInterceptorNotRegistered_whenDisabled() {
        boolean present = applicationContext.containsBean("slowQueryInterceptor");
        assertThat(present)
                .as("SlowQueryInterceptor 不应在 slow-query.enabled=false 时注册")
                .isFalse();
    }

    @Test
    @DisplayName("DIR: SamplingTurboFilter 默认未注册（logging.sampling.enabled=false）")
    void should_samplingTurboFilterNotRegistered_whenDisabled() {
        boolean present = applicationContext.containsBean("samplingTurboFilter");
        assertThat(present)
                .as("SamplingTurboFilter 不应在 sampling.enabled=false 时注册")
                .isFalse();
    }
}
