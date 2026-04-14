package org.smm.archetype.support.basic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.UnitTestBase;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L3 单元测试 — 验证 app 模块中不存在已删除的冗余 Configure 类。
 * <p>
 * 覆盖能力: client-modules-replacement (architectural guard)
 */
@DisplayName("无冗余 Configure 类")
class NoRedundantConfigureUTest extends UnitTestBase {

    private static final String CONFIG_DIR =
            "src/main/java/org/smm/archetype/config";

    @Test
    @DisplayName("INV: CacheConfigure.java 不应存在")
    void should_notHaveCacheConfigure() {
        assertThat(Files.exists(Path.of(CONFIG_DIR, "CacheConfigure.java")))
                .as("CacheConfigure.java 应已被删除（client-cache AutoConfiguration 替代）")
                .isFalse();
    }

    @Test
    @DisplayName("INV: OssConfigure.java 不应存在")
    void should_notHaveOssConfigure() {
        assertThat(Files.exists(Path.of(CONFIG_DIR, "OssConfigure.java")))
                .as("OssConfigure.java 应已被删除（client-oss AutoConfiguration 替代）")
                .isFalse();
    }

    @Test
    @DisplayName("INV: NotificationConfigure.java 不应存在")
    void should_notHaveNotificationConfigure() {
        assertThat(Files.exists(Path.of(CONFIG_DIR, "NotificationConfigure.java")))
                .as("NotificationConfigure.java 应已被删除（client-email/client-sms AutoConfiguration 替代）")
                .isFalse();
    }

    @Test
    @DisplayName("INV: SearchConfigure.java 不应存在")
    void should_notHaveSearchConfigure() {
        assertThat(Files.exists(Path.of(CONFIG_DIR, "SearchConfigure.java")))
                .as("SearchConfigure.java 应已被删除（client-search AutoConfiguration 替代）")
                .isFalse();
    }

    @Test
    @DisplayName("INV: LoggingConfigure.java 应存在（从 client-log 迁移）")
    void should_haveLoggingConfigure() {
        assertThat(Files.exists(Path.of(CONFIG_DIR, "LoggingConfigure.java")))
                .as("LoggingConfigure.java 应存在（从 client-log 迁移到 app/config/）")
                .isTrue();
    }

    @Test
    @DisplayName("INV: config/logging/ 目录不应存在")
    void should_notHaveLoggingConfigDirectory() {
        assertThat(Files.exists(Path.of(CONFIG_DIR, "logging")))
                .as("config/logging/ 目录应已被删除（迁移至 shared/logging/）")
                .isFalse();
    }

    @Test
    @DisplayName("INV: config/properties/LoggingProperties.java 应存在（从 client-log 迁移）")
    void should_haveLoggingProperties() {
        assertThat(Files.exists(Path.of(CONFIG_DIR, "properties", "LoggingProperties.java")))
                .as("LoggingProperties.java 应存在（从 client-log 迁移到 app/config/properties/）")
                .isTrue();
    }

    @Test
    @DisplayName("INV: log/ 包不应存在（迁移至 client-log）")
    void should_notHaveLogPackage() {
        assertThat(Files.exists(Path.of("src/main/java/org/smm/archetype/log")))
                .as("log/ 包应已被删除（迁移至 client-log）")
                .isFalse();
    }
}
