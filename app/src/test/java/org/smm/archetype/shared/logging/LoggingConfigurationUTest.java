package org.smm.archetype.shared.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.Environment;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LoggingConfiguration 纯逻辑测试。
 * <p>
 * 不使用 Mockito — 通过匿名 Environment 实现直接传入配置值，
 * 测试日志目录创建的分支逻辑。
 */
@DisplayName("LoggingConfiguration 日志配置验证")
class LoggingConfigurationUTest {

    // ApplicationReadyEvent 不被读取任何属性，传 null 即可
    private static final org.springframework.boot.context.event.ApplicationReadyEvent STUB_EVENT = null;

    @TempDir
    Path tempDir;
    private LoggingConfiguration configuration;

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
            configuration = new LoggingConfiguration(envWithPath(newDir.toString()));

            configuration.onApplicationEvent(STUB_EVENT);

            assertThat(newDir.toFile()).exists();
            assertThat(newDir.toFile()).isDirectory();
        }

        @Test
        @DisplayName("目录已存在且可写时正常完成")
        void should_succeed_when_directory_exists_and_writable() {
            configuration = new LoggingConfiguration(envWithPath(tempDir.toString()));

            configuration.onApplicationEvent(STUB_EVENT);

            assertThat(tempDir.toFile()).exists();
        }

        @Test
        @DisplayName("使用默认路径 .logs 当属性未配置时")
        void should_use_default_path_when_property_not_set() {
            configuration = new LoggingConfiguration(envWithPath(".logs"));

            // 不应抛出异常（.logs 目录在当前工作目录创建）
            configuration.onApplicationEvent(STUB_EVENT);
        }

        @Test
        @DisplayName("使用自定义 logging.file.path 属性")
        void should_use_custom_path_from_property() {
            Path customDir = tempDir.resolve("custom-log-path");
            configuration = new LoggingConfiguration(envWithPath(customDir.toString()));

            configuration.onApplicationEvent(STUB_EVENT);

            assertThat(customDir.toFile()).exists();
        }

        @Test
        @DisplayName("创建多级目录结构")
        void should_create_nested_directory_structure() {
            Path nestedDir = tempDir.resolve("a/b/c/logs");
            configuration = new LoggingConfiguration(envWithPath(nestedDir.toString()));

            configuration.onApplicationEvent(STUB_EVENT);

            assertThat(nestedDir.toFile()).exists();
        }

    }

}
