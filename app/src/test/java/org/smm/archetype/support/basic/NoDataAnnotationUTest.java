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
 * 验证项目中所有 Java 文件不使用 @Data 注解（AGENTS.md 规范）。
 * <p>
 * Lombok 在编译时移除 @Data 注解，因此 ArchUnit 无法在字节码层面检测。
 * 本测试通过扫描源代码文件验证。
 */
@DisplayName("@Data 注解禁止检查")
class NoDataAnnotationUTest extends UnitTestBase {

    /**
     * 项目根目录（从 app 模块的 test 资源目录向上回溯）
     */
    private static final String PROJECT_ROOT = System.getProperty("user.dir", ".").replace("/app", "");

    @Test
    @DisplayName("项目中的 Java 源码不应使用 @Data 注解")
    void should_not_use_data_annotation_in_source() throws IOException {
        // given: 扫描所有 src/main/java 下的 Java 文件
        Path rootPath = Paths.get(PROJECT_ROOT).toAbsolutePath().normalize();

        List<String> violatingFiles;
        try (Stream<Path> walk = Files.walk(rootPath)) {
            violatingFiles = walk
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> p.toString().contains("/src/main/java/"))
                    .filter(p -> !p.toString().contains("/target/"))
                    .filter(this::hasDataAnnotation)
                    .map(p -> rootPath.relativize(p).toString())
                    .sorted()
                    .collect(Collectors.toList());
        }

        assertThat(violatingFiles)
                .as("src/main/java 下的 Java 文件不应使用 @Data 注解（AGENTS.md 规范要求使用 @Getter @Setter）")
                .isEmpty();
    }

    private boolean hasDataAnnotation(Path file) {
        try {
            List<String> lines = Files.readAllLines(file);
            for (String line : lines) {
                String trimmed = line.trim();
                // 匹配独立的 @Data 注解（不是 @Data... 的其他注解如 @DataSource）
                if (trimmed.equals("@Data") || trimmed.startsWith("@Data(") || trimmed.startsWith("@Data ")) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }
}
