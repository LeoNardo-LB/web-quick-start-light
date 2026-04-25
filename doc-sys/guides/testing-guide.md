# 测试指南 (Testing Guide)

> **职责**: 定义测试策略、命名规范和最佳实践
> **轨道**: Constraint
> **维护者**: Human

---

## 目录

- [概述](#概述)
- [编码规则](#编码规则)
  - [测试分层规则](#测试分层规则)
  - [测试命名规则](#测试命名规则)
  - [测试标签规则](#测试标签规则)
  - [测试结构规则](#测试结构规则)
  - [测试基类规则](#测试基类规则)
  - [断言规则](#断言规则)
- [代码示例](#代码示例)
  - [单元测试示例](#单元测试示例)
  - [集成测试示例](#集成测试示例)
  - [参数化测试示例](#参数化测试示例)
  - [嵌套分组示例](#嵌套分组示例)
  - [边界值测试示例](#边界值测试示例)
- [违规示例](#违规示例)
- [检查方法](#检查方法)
  - [JaCoCo 覆盖率检查](#jacoco-覆盖率检查)
  - [测试分类执行](#测试分类执行)
  - [架构守护测试](#架构守护测试)
- [检查清单](#检查清单)
- [相关文档](#相关文档)
- [变更历史](#变更历史)

---

## 概述

项目采用 **三层测试金字塔** 策略，总计 47 个测试文件、~200+ 测试用例：

```
        ┌──────────────────┐
        │  L3 集成测试 (14) │  ← *ITest.java — 完整 Spring 上下文
        ├──────────────────┤
        │  L2 单元测试 (23) │  ← *UTest.java — Mockito 隔离
        ├──────────────────┤
        │  L1 架构守护 (5)  │  ← ArchUnit — 自动化规则
        └──────────────────┘
          Support (5) = 基类 + 配置
```

### 测试框架栈

| 框架 | 用途 | 版本 |
|------|------|------|
| JUnit 5 | 测试引擎 | Boot 4.x 管理 |
| Mockito | Mock 框架 | Boot 4.x 管理 |
| AssertJ | 流式断言 | Boot 4.x 管理 |
| Spring Boot Test | 集成测试容器 | Boot 4.x 管理 |
| WebTestClient | HTTP 客户端（集成测试） | Boot 4.x 管理 |
| Testcontainers | Docker 容器（Jaeger） | 独立版本 |
| ArchUnit | 架构规则验证 | 1.4.1 |

### 测试覆盖分布

| 层级 | 单元测试 (UTest) | 集成测试 (ITest) | 合计 |
|------|:-:|:-:|:-:|
| Controller | 1 | 6 | 7 |
| Facade | 1 | 3 | 4 |
| Repository | 2 | 3 | 5 |
| Entity | 4 | 0 | 4 |
| Exception | 2 | 0 | 2 |
| Config | 3 | 0 | 3 |
| Service/Auth | 0 | 1 | 1 |
| AOP 切面 | 13 | 1 | 14 |
| 工具类 | 10 | 0 | 10 |
| 架构守护 | 5 | 0 | 5 |
| **合计** | **41** | **14** | **55** |

---

## 编码规则

### 测试分层规则

| # | 规则 | 说明 |
|---|------|------|
| T1 | 单元测试继承 `UnitTestBase` | 启用 `@ExtendWith(MockitoExtension.class)`，不启动 Spring |
| T2 | 集成测试继承 `IntegrationTestBase` | 启用 `@SpringBootTest(RANDOM_PORT)` + `@ActiveProfiles("test")` |
| T3 | 单元测试文件名以 `UTest` 结尾 | 如 `SystemConfigFacadeImplUTest.java` |
| T4 | 集成测试文件名以 `ITest` 结尾 | 如 `SystemConfigControllerITest.java` |
| T5 | 边界值测试文件名包含 `Boundary` | 如 `OperationLogPaginationBoundaryITest.java` |

### 测试命名规则

| # | 规则 | 格式 |
|---|------|------|
| T6 | 测试方法使用行为驱动描述 | `should_{预期行为}_when_{条件}` |

**命名模板**：

| 场景 | 格式 | 示例 |
|------|------|------|
| 正常流程 | `should_returnXxx_when_validInput` | `should_returnSuccessResult_when_validConfigKey` |
| 异常流程 | `should_throwXxx_when_invalidInput` | `should_throwBizException_when_configKeyNotFound` |
| 边界条件 | `should_returnXxx_when_boundaryCondition` | `should_returnEmptyData_when_pageNoExceedsTotal` |
| 空值处理 | `should_handleXxx_when_inputIsNull` | `should_returnNull_when_contextNotBound` |

### 测试标签规则

| 标签 | 全称 | 用途 | 使用位置 |
|------|------|------|---------|
| `MFT` | Must-Feature Test | 核心功能验证（正向路径） | `@Test` |
| `DIR` | Directional Test | 方向性验证（异常路径） | `@Test` |
| `BND` | Boundary Test | 边界值测试 | `@Test` |
| `E2E` | End-to-End Test | 端到端测试 | `@Test` |

**使用方式**：标签体现在 `@DisplayName` 注解中，不使用 JUnit Tags：

```java
@Test
@DisplayName("MFT: 应返回成功结果当输入有效")
void should_returnSuccessResult_when_validInput() { ... }
```

### 测试结构规则

| # | 规则 | 说明 |
|---|------|------|
| T7 | 使用 AAA 模式 | Arrange（准备）→ Act（执行）→ Assert（断言） |
| T8 | 使用 `@Nested` 分组 | 正常路径 / 错误路径 / 边界路径分组 |
| T9 | 使用中文 `@DisplayName` | 所有测试方法必须有中文描述 |
| T10 | 集成测试使用 `@BeforeEach` 准备数据 | 避免测试间数据依赖 |

### 测试基类规则

| # | 规则 | 说明 |
|---|------|------|
| T11 | 单元测试必须继承 `UnitTestBase` | 自动注入 `@Mock`/`@InjectMocks` |
| T12 | 集成测试必须继承 `IntegrationTestBase` | 自动配置 `WebTestClient` + `@ActiveProfiles("test")` |
| T13 | 不允许直接使用 `@SpringBootTest` | 必须通过基类间接使用 |

### 断言规则

| # | 规则 | 说明 |
|---|------|------|
| T14 | 使用 AssertJ 流式断言 | `assertThat(x).isEqualTo(y)` 而非 `assertEquals(x, y)` |
| T15 | 异常断言使用 `assertThatThrownBy` | `assertThatThrownBy(() -> ...).isInstanceOf(...)` |
| T16 | 验证 Mock 使用 `verify` | `verify(mock).method(args)` 验证方法调用 |

---

## 代码示例

### 单元测试示例

```java
// SystemConfigFacadeImplUTest.java — 单元测试
class SystemConfigFacadeImplUTest extends UnitTestBase {

    @Mock
    private SystemConfigService systemConfigService;

    @InjectMocks
    private SystemConfigFacadeImpl systemConfigFacade;

    @Test
    @DisplayName("MFT: 应返回所有分组当 Service 正常返回")
    void should_returnAllGroups_when_serviceReturnsGroups() {
        // Arrange
        when(systemConfigService.getAllGroups()).thenReturn(List.of("BASIC", "EMAIL"));

        // Act
        List<String> groups = systemConfigFacade.getAllGroups();

        // Assert
        assertThat(groups).containsExactly("BASIC", "EMAIL");
        verify(systemConfigService).getAllGroups();
    }

    @Test
    @DisplayName("DIR: 应抛出 BizException 当配置 Key 不存在")
    void should_throwBizException_when_configKeyNotFound() {
        // Arrange
        when(systemConfigService.findByConfigKey(any())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> systemConfigFacade.getConfigByKey(ConfigKey.of("NOT_EXIST")))
                .isInstanceOf(BizException.class);
    }
}
```

### 集成测试示例

```java
// SystemConfigControllerITest.java — 集成测试
class SystemConfigControllerITest extends IntegrationTestBase {

    @Test
    @DisplayName("MFT: 应返回所有配置当查询全部配置")
    void should_returnAllConfigs_when_queryAll() {
        // Act
        webTestClient.get()
                .uri("/api/system/configs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(15);
    }

    @Test
    @DisplayName("DIR: 应返回错误当更新值为空")
    void should_returnError_when_updateValueIsEmpty() {
        // Act & Assert
        webTestClient.put()
                .uri("/api/system/configs/site.name")
                .bodyValue(Map.of("configValue", ""))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(2001); // 参数校验失败
    }
}
```

### 参数化测试示例

```java
// ExceptionHierarchyUTest.java — 参数化测试
class ExceptionHierarchyUTest extends UnitTestBase {

    @ParameterizedTest
    @MethodSource("exceptionProvider")
    @DisplayName("INV: 应持有正确的 ErrorCode 当通过 ErrorCode 构造")
    void should_holdCorrectErrorCode_when_constructedWithErrorCode(
            Class<? extends BaseException> exceptionClass) {
        // Act
        BaseException exception = createException(exceptionClass, CommonErrorCode.ILLEGAL_ARGUMENT);

        // Assert
        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
    }

    static Stream<Arguments> exceptionProvider() {
        return Stream.of(
                arguments(BizException.class),
                arguments(ClientException.class),
                arguments(SysException.class)
        );
    }
}
```

### 嵌套分组示例

```java
class AuthE2EITest extends IntegrationTestBase {

    @Nested
    @DisplayName("正常路径")
    class HappyPathTest {

        @Test
        @DisplayName("MFT: 应登录成功当凭据正确")
        void should_loginSuccessfully_when_credentialsAreCorrect() { ... }

        @Test
        @DisplayName("MFT: 应注销成功当已登录")
        void should_logoutSuccessfully_when_loggedIn() { ... }
    }

    @Nested
    @DisplayName("错误与边界路径")
    class ErrorAndBoundaryPathTest {

        @Test
        @DisplayName("DIR: 应返回用户不存在错误当用户名无效")
        void should_returnUserNotFound_when_usernameIsInvalid() { ... }

        @Test
        @DisplayName("DIR: 应返回密码错误当密码不匹配")
        void should_returnBadCredentials_when_passwordMismatch() { ... }
    }
}
```

### 边界值测试示例

```java
// SystemConfigPaginationBoundaryITest.java — 边界值测试
class SystemConfigPaginationBoundaryITest extends IntegrationTestBase {

    @Test
    @DisplayName("BND: 应返回空数据当 pageNo 超出范围")
    void should_returnEmptyData_when_pageNoExceedsTotal() {
        webTestClient.get()
                .uri("/api/system/configs/page?pageNo=100&pageSize=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data").isEmpty();
    }

    @Test
    @DisplayName("BND: 应返回恰好 1 条当 pageSize=1")
    void should_returnExactlyOne_when_pageSizeIsOne() {
        webTestClient.get()
                .uri("/api/system/configs/page?pageNo=1&pageSize=1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(1);
    }

    @Test
    @DisplayName("BND: 应返回全部 15 条当 pageSize=100")
    void should_returnAll15_when_pageSizeIs100() {
        webTestClient.get()
                .uri("/api/system/configs/page?pageNo=1&pageSize=100")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(15);
    }
}
```

---

## 违规示例

### 违规 1：集成测试不继承基类

```java
// ❌ 违规 T12/T13：直接使用 @SpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MyControllerITest { ... }

// ✅ 正确：继承 IntegrationTestBase
class MyControllerITest extends IntegrationTestBase { ... }
```

### 违规 2：不使用 AAA 模式

```java
// ❌ 违规 T7：准备、执行、断言混合
@Test
void testMethod() {
    List<String> result = service.getAll();
    when(service.getAll()).thenReturn(List.of("A"));
    assertThat(result).containsExactly("A");
}

// ✅ 正确：AAA 模式
@Test
void should_returnAll_when_serviceReturnsData() {
    // Arrange
    when(service.getAll()).thenReturn(List.of("A"));

    // Act
    List<String> result = service.getAll();

    // Assert
    assertThat(result).containsExactly("A");
}
```

### 违规 3：使用 JUnit 断言

```java
// ❌ 违规 T14：使用 JUnit assertEquals
assertEquals(expected, actual);
assertTrue(result.isEmpty());

// ✅ 正确：使用 AssertJ 流式断言
assertThat(actual).isEqualTo(expected);
assertThat(result).isEmpty();
```

### 违规 4：缺少中文 DisplayName

```java
// ❌ 违规 T9：无 DisplayName 或英文 DisplayName
@Test
void shouldReturnSuccessWhenValidInput() { ... }

// ✅ 正确：中文 DisplayName
@Test
@DisplayName("MFT: 应返回成功当输入有效")
void should_returnSuccess_when_validInput() { ... }
```

---

## 检查方法

### JaCoCo 覆盖率检查

项目使用 JaCoCo 三阶段覆盖率管理：

```bash
# 1. 全局 prepare-agent（根 POM）
mvn prepare-agent

# 2. app 模块 merge + report-aggregate
mvn verify -pl app

# 3. 查看报告
# 位置: app/target/site/jacoco-aggregate/index.html
```

**排除范围**：Test 类、generated 包、Application 启动类、Properties 类、Configuration 类

### 测试分类执行

```bash
# 运行所有单元测试
mvn test -pl app -Dtest="*UTest"

# 运行所有集成测试
mvn test -pl app -Dtest="*ITest"

# 运行架构守护测试
mvn test -pl app -Dtest="ArchitectureComplianceUTest,NoDataAnnotationUTest,NoRedundantConfigureUTest,NoValueInjectionUTest"

# 运行单个测试类
mvn test -pl app -Dtest="SystemConfigControllerITest"

# 运行指定标签测试（通过方法名过滤）
mvn test -pl app -Dtest="*ITest#*should_return*"
```

### 架构守护测试

| 测试文件 | 守护规则 |
|---------|---------|
| `ArchitectureComplianceUTest` | 6 条分层架构规则 |
| `NoDataAnnotationUTest` | 禁止 `@Data` 注解 |
| `NoRedundantConfigureUTest` | 禁止冗余 Configure 类 |
| `NoValueInjectionUTest` | 禁止 `@Value` 注入 |
| `ApplicationStartupITest` | 应用启动成功 + 关键 Bean 验证 |

---

## 检查清单

> 以下清单适用于 Code Review 和提交前自检。

### 测试分层

- [ ] 单元测试是否继承 `UnitTestBase`
- [ ] 集成测试是否继承 `IntegrationTestBase`
- [ ] 文件名是否以 `UTest`/`ITest` 结尾
- [ ] 边界值测试文件名是否包含 `Boundary`

### 测试命名

- [ ] 测试方法是否使用 `should_X_when_Y` 格式
- [ ] 是否有中文 `@DisplayName`
- [ ] `@DisplayName` 是否包含测试标签前缀（MFT/DIR/BND）

### 测试结构

- [ ] 是否使用 AAA 模式（注释分隔 Arrange/Act/Assert）
- [ ] 集成测试是否使用 `@BeforeEach` 准备数据
- [ ] 相关测试是否使用 `@Nested` 分组

### 断言规范

- [ ] 是否使用 AssertJ 流式断言（非 JUnit assertEquals）
- [ ] 异常断言是否使用 `assertThatThrownBy`
- [ ] Mock 验证是否使用 `verify`

### 测试覆盖

- [ ] 新增代码是否有对应测试
- [ ] 正常路径是否有 MFT 测试
- [ ] 异常路径是否有 DIR 测试
- [ ] 分页接口是否有 BND 边界值测试

### 自动化检查

- [ ] `mvn test` 是否全部通过
- [ ] JaCoCo 覆盖率是否达标
- [ ] 架构守护测试是否通过

---

## 相关文档

| 文档 | 关系 | 说明 |
|------|------|------|
| 编码规范 | guides/coding-standards.md | ArchUnit 架构守护规则 |
| AOP 切面 | infrastructure/aop-aspects.md | 切面测试覆盖 |
| 配置参考 | guides/configuration-reference.md | 测试环境配置 |
| 数据库 Schema | guides/database.md | 测试数据库初始化 |

---

## 变更历史

| 版本 | 日期 | 变更内容 |
|------|------|---------|
| 1.0.0 | 2026-04-25 | 初始版本：16 条规则 + 测试金字塔 + 示例 + 检查清单 |
