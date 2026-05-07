package org.smm.archetype.support.basic;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * 源码扫描工具类，用于检测编译期被 Lombok 消除的注解和字符串模式。
 * ArchUnit 无法检测这些模式，因此使用源码文件扫描。
 */
class SourceScanner {

    static final String PROJECT_ROOT = computeProjectRoot();

    private static String computeProjectRoot() {
        Path dir = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        // 向上搜索包含根 pom.xml（有 <modules> 的）的目录
        for (int i = 0; i < 10; i++) {
            Path pom = dir.resolve("pom.xml");
            if (Files.exists(pom)) {
                try {
                    List<String> lines = Files.readAllLines(pom);
                    // 根 pom.xml 包含 <modules>
                    boolean isRoot = lines.stream().anyMatch(l -> l.contains("<modules>"));
                    if (isRoot) {
                        return dir.toString();
                    }
                } catch (IOException e) {
                    // ignore
                }
            }
            dir = dir.getParent();
            if (dir == null) break;
        }
        // fallback: 与现有 NoDataAnnotationUTest 保持一致
        return System.getProperty("user.dir", ".").replace("/app", "");
    }

    /**
     * 扫描所有模块的 src/main/java 下的 Java 文件，返回满足条件的违规文件列表。
     * 覆盖范围：app/src/main/java/、components/component-xxx/src/main/java/、common/src/main/java/。
     * Files.walk 从 PROJECT_ROOT 递归遍历，路径包含 "/src/main/java/" 的 .java 文件均会被匹配。
     */
    static List<String> scanMainSource(Predicate<Path> fileFilter, Predicate<List<String>> contentMatcher) {
        return scanSource("src/main/java", fileFilter, contentMatcher);
    }

    /**
     * 扫描所有模块的 src/test/java 下的 Java 文件，返回满足条件的违规文件列表。
     * 覆盖范围：app/src/test/java/、components/component-xxx/src/test/java/（如有）。
     */
    static List<String> scanTestSource(Predicate<Path> fileFilter, Predicate<List<String>> contentMatcher) {
        return scanSource("src/test/java", fileFilter, contentMatcher);
    }

    private static List<String> scanSource(String sourceDir, Predicate<Path> fileFilter, Predicate<List<String>> contentMatcher) {
        Path rootPath = Paths.get(PROJECT_ROOT).toAbsolutePath().normalize();
        List<String> violatingFiles = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(rootPath)) {
            walk.filter(p -> p.toString().endsWith(".java"))
                .filter(p -> p.toString().contains("/" + sourceDir + "/"))
                .filter(p -> !p.toString().contains("/target/"))
                .filter(p -> !p.toString().contains("/.worktrees/"))
                .filter(fileFilter)
                .filter(p -> {
                    try {
                        List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
                        return contentMatcher.test(lines);
                    } catch (MalformedInputException e) {
                        // 非 UTF-8 文件：跳过
                        return false;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .map(p -> rootPath.relativize(p).toString())
                .sorted()
                .forEach(violatingFiles::add);
        } catch (IOException e) {
            // ignore
        }
        return violatingFiles;
    }

    /**
     * 判断一行是否是注释行: 以 // 或 * 或 斜杠星 或 星斜杠 开头。
     * 注意: 不含行尾注释检测(代码行含 // 不算注释行)。
     */
    static boolean isCommentLine(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("//")
            || trimmed.startsWith("*")
            || trimmed.startsWith("/*")
            || trimmed.startsWith("*/");
    }

    /**
     * 判断一行是否是 import 语句。包括 import xxx 和 import static xxx。
     */
    static boolean isImportLine(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("import ");
    }

    /**
     * 块注释状态跟踪器。逐行处理，维护 斜杠星 到 星斜杠 的开启/关闭状态。
     */
    static class BlockCommentTracker {
        private boolean inBlockComment = false;

        /**
         * 处理一行，更新块注释状态。返回当前行是否在块注释中。
         */
        boolean processLine(String line) {
            String trimmed = line.trim();
            if (inBlockComment) {
                if (trimmed.contains("*/")) {
                    inBlockComment = false;
                }
                return true; // 当前行在块注释中
            }
            if (trimmed.contains("/*")) {
                if (!trimmed.contains("*/")) {
                    inBlockComment = true;
                }
                return true; // /* 开头的行也算在块注释中
            }
            return false;
        }

        boolean isInBlockComment() {
            return inBlockComment;
        }
    }
}
