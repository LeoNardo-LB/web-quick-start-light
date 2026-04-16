package org.smm.archetype.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.smm.archetype.config.properties.LoggingProperties;
import org.smm.archetype.shared.aspect.operationlog.LogAspect;
import org.smm.archetype.shared.aspect.operationlog.OperationLogWriter;
import org.smm.archetype.shared.util.logging.SamplingTurboFilter;
import org.smm.archetype.shared.util.logging.SlowQueryInterceptor;
import org.smm.archetype.support.UnitTestBase;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * LoggingConfigure 日志目录验证逻辑测试。
 * <p>
 * 通过匿名 Environment 实现直接传入配置值，
 * 测试日志目录创建的分支逻辑。
 * <p>
 * 新增测试覆盖 @Bean 方法和 onApplicationEvent 未覆盖分支。
 */
@DisplayName("LoggingConfigure 日志配置")
class LoggingConfigureDirectoryUTest extends UnitTestBase {

    // ApplicationReadyEvent 不被读取任何属性，传 null 即可
    private static final org.springframework.boot.context.event.ApplicationReadyEvent STUB_EVENT = null;

    @TempDir
    Path tempDir;
    private LoggingConfigure configure;

    private Environment envWithPath(String path) {
        return new Environment() {
            @Override
            public String getProperty(String key, String defaultValue) {
                return "logging.file.path".equals(key) ? path : defaultValue;
            }

            @Override
            public String getProperty(String key) {return null;}

            @Override
            public <T> T getProperty(String key, Class<T> targetType) {return null;}

            @Override
            public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {return defaultValue;}

            @Override
            public String getRequiredProperty(String key) {return null;}

            @Override
            public <T> T getRequiredProperty(String key, Class<T> targetType) {return null;}

            @Override
            public boolean containsProperty(String key) {return "logging.file.path".equals(key);}

            @Override
            public String resolveRequiredPlaceholders(String text) {return text;}

            @Override
            public String resolvePlaceholders(String text) {return text;}

            @Override
            public String[] getActiveProfiles() {return new String[0];}

            @Override
            public String[] getDefaultProfiles() {return new String[0];}

            @Override
            public boolean acceptsProfiles(String... profiles) {return false;}

            @Override
            public boolean acceptsProfiles(org.springframework.core.env.Profiles profiles) {return false;}
        };
    }

    @Nested
    @DisplayName("onApplicationEvent")
    class OnApplicationEvent {

        @Test
        @DisplayName("目录不存在时自动创建")
        void should_create_directory_when_not_exists() {
            Path newDir = tempDir.resolve("new-logs-dir");
            configure = new LoggingConfigure(envWithPath(newDir.toString()));

            configure.onApplicationEvent(STUB_EVENT);

            assertThat(newDir.toFile()).exists();
            assertThat(newDir.toFile()).isDirectory();
        }

        @Test
        @DisplayName("目录已存在且可写时正常完成")
        void should_succeed_when_directory_exists_and_writable() {
            configure = new LoggingConfigure(envWithPath(tempDir.toString()));

            configure.onApplicationEvent(STUB_EVENT);

            assertThat(tempDir.toFile()).exists();
        }

        @Test
        @DisplayName("使用默认路径 .logs 当属性未配置时")
        void should_use_default_path_when_property_not_set() {
            configure = new LoggingConfigure(envWithPath(".logs"));

            // 不应抛出异常（.logs 目录在当前工作目录创建）
            configure.onApplicationEvent(STUB_EVENT);
        }

        @Test
        @DisplayName("使用自定义 logging.file.path 属性")
        void should_use_custom_path_from_property() {
            Path customDir = tempDir.resolve("custom-log-path");
            configure = new LoggingConfigure(envWithPath(customDir.toString()));

            configure.onApplicationEvent(STUB_EVENT);

            assertThat(customDir.toFile()).exists();
        }

        @Test
        @DisplayName("创建多级目录结构")
        void should_create_nested_directory_structure() {
            Path nestedDir = tempDir.resolve("a/b/c/logs");
            configure = new LoggingConfigure(envWithPath(nestedDir.toString()));

            configure.onApplicationEvent(STUB_EVENT);

            assertThat(nestedDir.toFile()).exists();
        }

    }

    @Nested
    @DisplayName("logAspect @Bean")
    class LogAspectBean {

        @Test
        @DisplayName("无 OperationLogWriter 时使用无参构造")
        void should_create_logAspect_without_writer() {
            LoggingConfigure configure = new LoggingConfigure(envWithPath(tempDir.toString()));

            LogAspect aspect = configure.logAspect();

            assertThat(aspect).isNotNull();
        }

        @Test
        @DisplayName("有 OperationLogWriter 时使用带参构造")
        void should_create_logAspect_with_writer() {
            OperationLogWriter writer = mock(OperationLogWriter.class);
            LoggingConfigure configure = new LoggingConfigure(envWithPath(tempDir.toString()));
            ReflectionTestUtils.setField(configure, "operationLogWriter", writer);

            LogAspect aspect = configure.logAspect();

            assertThat(aspect).isNotNull();
        }
    }

    @Nested
    @DisplayName("slowQueryInterceptor @Bean")
    class SlowQueryInterceptorBean {

        @Test
        @DisplayName("根据 LoggingProperties 创建慢查询拦截器")
        void should_create_slow_query_interceptor() {
            LoggingProperties properties = new LoggingProperties();
            properties.getSlowQuery().setThresholdMs(2000L);
            properties.getSlowQuery().setEnabled(true);
            LoggingConfigure configure = new LoggingConfigure(envWithPath(tempDir.toString()));

            SlowQueryInterceptor interceptor = configure.slowQueryInterceptor(properties);

            assertThat(interceptor).isNotNull();
        }
    }

    @Nested
    @DisplayName("samplingTurboFilter @Bean")
    class SamplingTurboFilterBean {

        @Test
        @DisplayName("根据 LoggingProperties 创建采样过滤器并设置采样率")
        void should_create_sampling_turbo_filter() {
            LoggingProperties properties = new LoggingProperties();
            properties.getSampling().setRate(0.05);
            LoggingConfigure configure = new LoggingConfigure(envWithPath(tempDir.toString()));

            SamplingTurboFilter filter = configure.samplingTurboFilter(properties);

            assertThat(filter).isNotNull();
        }
    }

    @Nested
    @DisplayName("onApplicationEvent 补充分支")
    class OnApplicationEventAdditional {

        @Test
        @DisplayName("磁盘空间不足时输出警告日志")
        void should_warn_when_disk_space_low() {
            File mockFile = mock(File.class);
            when(mockFile.getFreeSpace()).thenReturn(100L * 1024 * 1024); // 100MB < 500MB

            Path mockPath = mock(Path.class);
            when(mockPath.toFile()).thenReturn(mockFile);
            when(mockPath.toAbsolutePath()).thenReturn(mock(Path.class));

            try (var pathsMock = mockStatic(Paths.class);
                 var filesMock = mockStatic(Files.class)) {
                pathsMock.when(() -> Paths.get("low-space-path")).thenReturn(mockPath);
                filesMock.when(() -> Files.exists(mockPath)).thenReturn(true);
                filesMock.when(() -> Files.isWritable(mockPath)).thenReturn(true);

                LoggingConfigure configure = new LoggingConfigure(envWithPath("low-space-path"));

                assertThatCode(() -> configure.onApplicationEvent(STUB_EVENT)).doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("目录创建失败时捕获异常不向外抛出")
        void should_catch_exception_when_directory_creation_fails() {
            try (var pathsMock = mockStatic(Paths.class);
                 var filesMock = mockStatic(Files.class)) {
                Path mockPath = mock(Path.class);
                pathsMock.when(() -> Paths.get("invalid-path")).thenReturn(mockPath);
                filesMock.when(() -> Files.exists(mockPath)).thenReturn(false);
                filesMock.when(() -> Files.createDirectories(mockPath))
                        .thenThrow(new RuntimeException("Permission denied"));

                LoggingConfigure configure = new LoggingConfigure(envWithPath("invalid-path"));

                assertThatCode(() -> configure.onApplicationEvent(STUB_EVENT)).doesNotThrowAnyException();
            }
        }
    }

}
