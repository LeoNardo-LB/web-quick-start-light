package org.smm.archetype.support.basic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.UnitTestBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("测试规范合规检查")
class TestConventionComplianceUTest extends UnitTestBase {

    // === T-01: 测试文件名 *UTest / *ITest / *ETest ===
    // 排除测试基础设施类（Configuration/TestAdaptor等）和 package-info

    @Test
    @DisplayName("T-01: 含 @Test 的测试文件必须以 UTest、ITest 或 ETest 结尾")
    void test_files_should_have_utest_or_itest_or_etest_suffix() {
        List<String> violations = SourceScanner.scanTestSource(
                p -> {
                    String name = p.getFileName().toString();
                    return !name.endsWith("UTest.java")
                            && !name.endsWith("ITest.java")
                            && !name.endsWith("ETest.java")
                            && !name.equals("package-info.java")
                            // 排除测试基础设施类
                            && !name.endsWith("Configuration.java")
                            && !name.endsWith("Application.java");
                },
                lines -> lines.stream().anyMatch(line ->
                        line.trim().contains("@Test") || line.trim().contains("@ParameterizedTest"))
        );

        assertThat(violations)
                .as("含 @Test 的测试文件应以 UTest.java、ITest.java 或 ETest.java 结尾")
                .isEmpty();
    }

    // === T-02: 禁止 UTest 中使用 @SpringBootTest ===
    // 使用源码扫描，但排除 import 行、注释行和字符串中的引用

    @Test
    @DisplayName("T-02: UTest 中禁止使用 @SpringBootTest")
    void utest_should_not_use_spring_boot_test() {
        List<String> violations = SourceScanner.scanTestSource(
                p -> p.getFileName().toString().endsWith("UTest.java"),
                lines -> {
                    SourceScanner.BlockCommentTracker tracker = new SourceScanner.BlockCommentTracker();
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (SourceScanner.isImportLine(trimmed)) continue;
                        if (trimmed.startsWith("//")) continue;
                        if (tracker.processLine(trimmed)) continue;
                        // 排除字符串字面量中的引用（断言消息中可能包含 @SpringBootTest）
                        if (trimmed.contains("@SpringBootTest") && !trimmed.contains("\"@SpringBootTest\"")) {
                            // 进一步排除: 在 .as() 或 assertThat 描述字符串中的引用
                            String codePart = trimmed.replaceAll("\"[^\"]*\"", "");  // 去掉字符串字面量
                            if (codePart.contains("@SpringBootTest")) {
                                return true;
                            }
                        }
                    }
                    return false;
                }
        );

        assertThat(violations)
                .as("UTest（纯单元测试）中不应使用 @SpringBootTest")
                .isEmpty();
    }

    // === T-03: 禁止 ITest 中使用 @Mock ===

    @Test
    @DisplayName("T-03: ITest 中禁止使用 @Mock 注解")
    void itest_should_not_use_mock() {
        List<String> violations = SourceScanner.scanTestSource(
                p -> p.getFileName().toString().endsWith("ITest.java"),
                lines -> {
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (SourceScanner.isImportLine(trimmed)) continue;
                        // 精确匹配 @Mock（排除 @MockBean、@MockitoBean 等）
                        if (trimmed.equals("@Mock") || trimmed.startsWith("@Mock(") || trimmed.startsWith("@Mock ")) {
                            return true;
                        }
                    }
                    return false;
                }
        );

        assertThat(violations)
                .as("ITest（集成测试）中不应使用 @Mock，应使用真实依赖")
                .isEmpty();
    }
}
