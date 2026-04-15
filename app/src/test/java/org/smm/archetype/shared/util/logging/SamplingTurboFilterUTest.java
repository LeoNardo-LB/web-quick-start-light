package org.smm.archetype.shared.util.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.spi.FilterReply;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SamplingTurboFilter 采样过滤器")
class SamplingTurboFilterUTest {

    private SamplingTurboFilter filter;
    private Logger              logger;

    @BeforeEach
    void setUp() {
        filter = new SamplingTurboFilter();
        logger = (Logger) LoggerFactory.getLogger(SamplingTurboFilterUTest.class);
    }

    @Nested
    @DisplayName("ERROR 级别日志始终放行")
    class ErrorLevelAlwaysPasses {

        @Test
        @DisplayName("ERROR 级别返回 NEUTRAL（不采样，直接放行）")
        void should_return_neutral_for_error_level() {
            filter.setSampleRate(0.01); // 极低采样率

            FilterReply reply = filter.decide(null, logger, Level.ERROR, "msg", null, null);

            assertThat(reply).isEqualTo(FilterReply.NEUTRAL);
        }

    }

    @Nested
    @DisplayName("sampleRate >= 1.0 全部放行")
    class FullSampleRate {

        @Test
        @DisplayName("sampleRate=1.0 时 INFO 级别返回 NEUTRAL")
        void should_return_neutral_when_sample_rate_is_1() {
            filter.setSampleRate(1.0);

            FilterReply reply = filter.decide(null, logger, Level.INFO, "msg", null, null);

            assertThat(reply).isEqualTo(FilterReply.NEUTRAL);
        }

        @Test
        @DisplayName("sampleRate=2.0 时 INFO 级别返回 NEUTRAL")
        void should_return_neutral_when_sample_rate_greater_than_1() {
            filter.setSampleRate(2.0);

            FilterReply reply = filter.decide(null, logger, Level.INFO, "msg", null, null);

            assertThat(reply).isEqualTo(FilterReply.NEUTRAL);
        }

    }

    @Nested
    @DisplayName("sampleRate=0.1 每 10 次放行 1 次")
    class SampleRate01 {

        @Test
        @DisplayName("前 9 次 INFO 级别返回 DENY")
        void should_deny_first_9_calls() {
            filter.setSampleRate(0.1);

            for (int i = 0; i < 9; i++) {
                FilterReply reply = filter.decide(null, logger, Level.INFO, "msg", null, null);
                assertThat(reply).isEqualTo(FilterReply.DENY);
            }
        }

        @Test
        @DisplayName("第 10 次返回 NEUTRAL（放行）")
        void should_neutral_on_10th_call() {
            filter.setSampleRate(0.1);

            // 前 9 次 DENY
            for (int i = 0; i < 9; i++) {
                filter.decide(null, logger, Level.INFO, "msg", null, null);
            }
            // 第 10 次 NEUTRAL
            FilterReply reply = filter.decide(null, logger, Level.INFO, "msg", null, null);
            assertThat(reply).isEqualTo(FilterReply.NEUTRAL);
        }

        @Test
        @DisplayName("每 10 次循环：第 10、20、30 次放行")
        void should_neutral_every_10th_call() {
            filter.setSampleRate(0.1);

            int neutralCount = 0;
            int denyCount = 0;
            for (int i = 0; i < 30; i++) {
                FilterReply reply = filter.decide(null, logger, Level.INFO, "msg", null, null);
                if (reply == FilterReply.NEUTRAL)
                    neutralCount++;
                else
                    denyCount++;
            }

            assertThat(neutralCount).isEqualTo(3);
            assertThat(denyCount).isEqualTo(27);
        }

    }

    @Nested
    @DisplayName("sampleRate=0.5 每 2 次放行 1 次")
    class SampleRate05 {

        @Test
        @DisplayName("第 1 次返回 DENY")
        void should_deny_first_call() {
            filter.setSampleRate(0.5);

            FilterReply reply = filter.decide(null, logger, Level.INFO, "msg", null, null);

            assertThat(reply).isEqualTo(FilterReply.DENY);
        }

        @Test
        @DisplayName("第 2 次返回 NEUTRAL")
        void should_neutral_on_2nd_call() {
            filter.setSampleRate(0.5);

            filter.decide(null, logger, Level.INFO, "msg", null, null);
            FilterReply reply = filter.decide(null, logger, Level.INFO, "msg", null, null);

            assertThat(reply).isEqualTo(FilterReply.NEUTRAL);
        }

        @Test
        @DisplayName("15 次调用中应有 7 次放行 8 次拒绝")
        void should_neutral_every_2nd_call_over_15_calls() {
            filter.setSampleRate(0.5);

            int neutralCount = 0;
            int denyCount = 0;
            for (int i = 0; i < 15; i++) {
                FilterReply reply = filter.decide(null, logger, Level.INFO, "msg", null, null);
                if (reply == FilterReply.NEUTRAL)
                    neutralCount++;
                else
                    denyCount++;
            }

            assertThat(neutralCount).isEqualTo(7);
            assertThat(denyCount).isEqualTo(8);
        }

    }

    @Nested
    @DisplayName("DEBUG 级别受采样控制")
    class DebugLevelSampling {

        @Test
        @DisplayName("低采样率下 DEBUG 级别大部分被拒绝")
        void should_deny_debug_with_low_sample_rate() {
            filter.setSampleRate(0.1);

            int neutralCount = 0;
            for (int i = 0; i < 20; i++) {
                FilterReply reply = filter.decide(null, logger, Level.DEBUG, "msg", null, null);
                if (reply == FilterReply.NEUTRAL)
                    neutralCount++;
            }

            assertThat(neutralCount).isEqualTo(2); // 每 10 次放行 1 次，20 次放行 2 次
        }

    }

    @Nested
    @DisplayName("WARN 级别受采样控制（WARN < ERROR）")
    class WarnLevelSampling {

        @Test
        @DisplayName("WARN 级别不走 ERROR 特殊路径，受采样控制")
        void should_warn_be_subject_to_sampling() {
            filter.setSampleRate(0.5);

            // 第 1 次 WARN
            FilterReply reply1 = filter.decide(null, logger, Level.WARN, "msg", null, null);
            assertThat(reply1).isEqualTo(FilterReply.DENY);

            // 第 2 次 WARN
            FilterReply reply2 = filter.decide(null, logger, Level.WARN, "msg", null, null);
            assertThat(reply2).isEqualTo(FilterReply.NEUTRAL);
        }

    }

}
