package org.smm.archetype.support.basic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.UnitTestBase;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SourceScanner 工具类")
class SourceScannerUTest extends UnitTestBase {

    @Nested
    @DisplayName("isCommentLine")
    class IsCommentLineTest {

        @Test
        @DisplayName("// 开头是注释行")
        void singleLineComment() {
            assertThat(SourceScanner.isCommentLine("// this is a comment")).isTrue();
        }

        @Test
        @DisplayName("* 开头是 Javadoc/块注释行")
        void javadocLine() {
            assertThat(SourceScanner.isCommentLine(" * javadoc description")).isTrue();
        }

        @Test
        @DisplayName("/* 开头是块注释开始行")
        void blockCommentStart() {
            assertThat(SourceScanner.isCommentLine("/* block comment start")).isTrue();
        }

        @Test
        @DisplayName("*/ 开头是块注释结束行")
        void blockCommentEnd() {
            assertThat(SourceScanner.isCommentLine(" */")).isTrue();
        }

        @Test
        @DisplayName("代码行（含行尾注释）不是注释行")
        void codeWithInlineComment() {
            assertThat(SourceScanner.isCommentLine("private Instant createdAt; // not LocalDateTime")).isFalse();
        }

        @Test
        @DisplayName("import 行不是注释行")
        void importLine() {
            assertThat(SourceScanner.isCommentLine("import java.time.LocalDateTime;")).isFalse();
        }

        @Test
        @DisplayName("空行不是注释行")
        void emptyLine() {
            assertThat(SourceScanner.isCommentLine("")).isFalse();
        }
    }

    @Nested
    @DisplayName("isImportLine")
    class IsImportLineTest {

        @Test
        @DisplayName("标准 import 是导入行")
        void standardImport() {
            assertThat(SourceScanner.isImportLine("import java.time.Instant;")).isTrue();
        }

        @Test
        @DisplayName("import static 是导入行")
        void staticImport() {
            assertThat(SourceScanner.isImportLine("import static org.assertj.core.api.Assertions.assertThat;")).isTrue();
        }

        @Test
        @DisplayName("普通代码不是导入行")
        void codeLine() {
            assertThat(SourceScanner.isImportLine("Instant now = Instant.now();")).isFalse();
        }
    }

    @Nested
    @DisplayName("isInBlockComment")
    class IsInBlockCommentTest {

        @Test
        @DisplayName("初始状态不在块注释中")
        void initialState() {
            SourceScanner.BlockCommentTracker tracker = new SourceScanner.BlockCommentTracker();
            assertThat(tracker.isInBlockComment()).isFalse();
        }

        @Test
        @DisplayName("遇到 /* 后进入块注释")
        void enterBlockComment() {
            SourceScanner.BlockCommentTracker tracker = new SourceScanner.BlockCommentTracker();
            tracker.processLine("/* start of block");
            assertThat(tracker.isInBlockComment()).isTrue();
        }

        @Test
        @DisplayName("遇到 */ 后退出块注释")
        void exitBlockComment() {
            SourceScanner.BlockCommentTracker tracker = new SourceScanner.BlockCommentTracker();
            tracker.processLine("/* start */");
            assertThat(tracker.isInBlockComment()).isFalse();
        }

        @Test
        @DisplayName("多行块注释中间行在块注释中")
        void multiLineBlockComment() {
            SourceScanner.BlockCommentTracker tracker = new SourceScanner.BlockCommentTracker();
            tracker.processLine("/* start");
            assertThat(tracker.isInBlockComment()).isTrue();
            tracker.processLine(" * middle line with LocalDateTime");
            assertThat(tracker.isInBlockComment()).isTrue();
            tracker.processLine(" */");
            assertThat(tracker.isInBlockComment()).isFalse();
        }
    }

    @Nested
    @DisplayName("PROJECT_ROOT")
    class ProjectRootTest {

        @Test
        @DisplayName("PROJECT_ROOT 应指向包含根 pom.xml 的目录")
        void projectRootContainsRootPom() {
            assertThat(SourceScanner.PROJECT_ROOT).isNotNull();
            assertThat(java.nio.file.Paths.get(SourceScanner.PROJECT_ROOT).resolve("pom.xml")).exists();
        }
    }
}
