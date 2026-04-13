package org.smm.archetype.support.basic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.UnitTestBase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证项目中不使用 @Value 注解注入配置（AGENTS.md 规范要求使用 @ConfigurationProperties）。
 */
@DisplayName("@Value 注入禁止检查")
class NoValueInjectionUTest extends UnitTestBase {

    private static final String PROJECT_ROOT = System.getProperty("user.dir", ".").replace("/app", "");

    @Test
    @DisplayName("项目中的 Java 源码不应使用 @Value 注解注入配置")
    void should_not_use_value_annotation() throws IOException {
        Path rootPath = Paths.get(PROJECT_ROOT).toAbsolutePath().normalize();

        List<String> violatingFiles;
        try (Stream<Path> walk = Files.walk(rootPath)) {
            violatingFiles = walk
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> p.toString().contains("/src/main/java/"))
                    .filter(p -> !p.toString().contains("/target/"))
                    .filter(this::hasValueAnnotation)
                    .map(p -> rootPath.relativize(p).toString())
                    .sorted()
                    .collect(Collectors.toList());
        }

        assertThat(violatingFiles)
                .as("src/main/java 下的 Java 文件不应使用 @Value 注解（AGENTS.md 规范要求使用 @ConfigurationProperties）")
                .isEmpty();
    }

    private boolean hasValueAnnotation(Path file) {
        try {
            List<String> lines = Files.readAllLines(file);
            for (String line : lines) {
                String trimmed = line.trim();
                // 匹配 @Value("${...}") 形式
                if (trimmed.startsWith("@Value(")) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }
}
