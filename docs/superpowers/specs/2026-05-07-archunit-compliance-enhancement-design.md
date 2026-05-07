# ArchUnit 合规增强设计

> 日期：2026-05-07
> 状态：Draft
> 范围：web-quick-start-light 项目

## 背景与目标

项目已有 5 个"规范守护"测试（`ArchitectureComplianceUTest` 6 条 ArchUnit 规则 + `NoDataAnnotationUTest` + `NoValueInjectionUTest` + `NoRedundantConfigureUTest` + `ApplicationStartupITest`），但 `AGENTS.md` 和 `docs/conventions/` 中的大量编码规范、模块约束、测试规范 **没有被自动化守护**。

本设计新增 **15 条规则**，分布在 4 个维度，全部可由 ArchUnit API 或源码扫描实现。

## 整体架构

### 新增测试文件

| 文件 | 职责 | 规则数 |
|------|------|-------|
| `CodingConventionComplianceUTest.java` | 编码规范 | C-01~C-07（7 条） |
| `ModuleArchitectureComplianceUTest.java` | 模块/架构约束 | M-01~M-03（3 条） |
| `SpringConfigComplianceUTest.java` | Spring 配置约束 | S-01（1 条） |
| `TestConventionComplianceUTest.java` | 测试规范 | T-01~T-03（3 条） |

所有文件位于 `app/src/test/java/org/smm/archetype/support/basic/`，与现有 5 个守护测试并列。

### 检测策略

| 策略 | 适用规则 | 说明 |
|------|---------|------|
| **ArchUnit API** | C-02, C-04, C-06, C-07, M-01~M-03, S-01 | 注解、字段类型、依赖、返回类型检查 |
| **源码扫描** | C-01, C-03, C-05, T-01~T-03 | Lombok 注解（编译期消除）、字符串模式 |

### 共用基础设施

- 所有测试继承 `UnitTestBase`（`@ExtendWith(MockitoExtension.class)`），纯内存运行
- 源码扫描工具类 `SourceScanner.java`：复用 `NoDataAnnotationUTest` 的 `PROJECT_ROOT` + `Files.walk` 模式，提取为可复用工具
- **不使用 FreezingArchRule**（项目规模小，直接严格检查）

---

## 规则详细设计

### 维度一：编码规范（CodingConventionComplianceUTest）

#### C-01：时间字段统一 `Instant`

- **来源**：`java-conventions.md` 规则 2
- **强度**：⛔ MUST
- **检测方式**：源码扫描
- **扫描范围**：`src/main/java` 下 `entity/` 和 `repository/` 包
- **检测逻辑**：
  - 文件内容包含字段声明 `LocalDateTime`（非 import 行）→ 违规
  - 文件内容包含字段声明 `java.util.Date`（非 import 行）→ 违规
- **排除**：import 语句、注释（`//` 或 `/* */`）
- **实现要点**：逐行扫描，跳过 `import` 开头的行和 `//` 注释行

#### C-02：禁止 JPA/Hibernate 注解

- **来源**：`java-conventions.md` 规则 5
- **强度**：⛔ MUST
- **检测方式**：ArchUnit
- **API**：
  ```java
  ArchRuleDefinition.classes()
      .that().areAnnotatedWith("javax.persistence.Entity")
      .or().areAnnotatedWith("jakarta.persistence.Entity")
      // ... 同理覆盖 @Table, @Column, @Id, @GeneratedValue, @OneToMany, @ManyToOne 等
      .should().notExist()
  ```
- **覆盖注解**：`@Entity`, `@Table`, `@Column`, `@Id`, `@GeneratedValue`, `@OneToMany`, `@ManyToOne`, `@ManyToMany`, `@OneToOne`, `@JoinColumn`
- **说明**：项目使用 MyBatis-Plus，JPA 注解不应出现

#### C-03：禁止 `BeanUtils.copyProperties`

- **来源**：`java-conventions.md` 规则 5
- **强度**：⛔ MUST
- **检测方式**：源码扫描
- **扫描范围**：`src/main/java`
- **检测逻辑**：
  - 文件内容包含 `BeanUtils.copyProperties`（非注释行）→ 违规
  - 文件内容包含 `import org.springframework.beans.BeanUtils`（非注释行）→ 违规
- **排除**：注释行

#### C-04：禁止 `System.out` / `System.err`

- **来源**：`java-conventions.md` 规则 7
- **强度**：⛔ MUST
- **检测方式**：ArchUnit `GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS`
- **说明**：应使用 SLF4J Logger

#### C-05：禁止 `@With`（Lombok）

- **来源**：`java-conventions.md` 规则 1
- **强度**：⛔ MUST
- **检测方式**：源码扫描（`@With` 是 Lombok 注解，编译期消除）
- **扫描范围**：`src/main/java`
- **检测逻辑**：
  - 文件内容包含 `@With`（独立注解或 `@With(`）→ 违规
  - 排除注释中的 `@With`
  - 排除字符串中的 `@With`（如在测试或文档字符串中引用）

#### C-06：DTO/VO 使用 `record`

- **来源**：`java-conventions.md` 规则 3
- **强度**：⚠️ SHOULD（违规时 WARN 不 FAIL）
- **检测方式**：ArchUnit
- **API**：
  ```java
  ArchRuleDefinition.classes()
      .that().resideInAPackage("..entity.api..")
      .and().areNotInterfaces()
      .and().areNotEnums()
      .and().areNotAbstract()
      .should(beRecord())
  ```
- **检测逻辑**：`entity.api` 包下的具体类必须是 Java record
- **排除**：枚举、接口、抽象类
- **注意**：⚠️ SHOULD 规则，违规时仅打印警告不导致测试失败

#### C-07：Properties 类禁止 `@Data`

- **来源**：`configuration.md` 规则 2
- **强度**：⛔ MUST
- **检测方式**：源码扫描
- **扫描范围**：`src/main/java` 中文件名以 `Properties` 或 `Configure` 结尾的文件
- **检测逻辑**：
  - 文件名匹配 `*Properties.java` 或 `*Configure.java`
  - 文件内容包含 `@Data`（独立注解或 `@Data(`）→ 违规
- **说明**：Properties 类应使用 `@Getter` + `@Setter`

---

### 维度二：模块/架构约束（ModuleArchitectureComplianceUTest）

#### M-01：`common` 模块零 Spring 依赖

- **来源**：`module-structure.md` 设计考量
- **强度**：⛔ MUST
- **检测方式**：ArchUnit
- **API**：
  ```java
  ArchRuleDefinition.noClasses()
      .that().resideInAPackage("..org.smm.archetype.exception..")
      .should().dependOnClassesThat()
      .resideInAPackage("org.springframework..")
  ```
- **约束**：common 模块的 exception 包不得引用任何 Spring 类
- **原因**：common 是纯 Java 异常体系，保持框架无关

#### M-02：组件模块间零互相依赖

- **来源**：`module-structure.md`
- **强度**：⛔ MUST
- **检测方式**：ArchUnit
- **实现策略**：
  - 定义组件列表 = `[cache, oss, email, sms, search, auth]`
  - 对每个组件 X，检查它不依赖其他任何组件：
    ```java
    for (String component : COMPONENTS) {
        List<String> otherPackages = COMPONENTS.stream()
            .filter(c -> !c.equals(component))
            .map(c -> "..component." + c + "..")
            .toList();
        ArchRuleDefinition.noClasses()
            .that().resideInAPackage("..component." + component + "..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(otherPackages)
            .check(importedClasses);
    }
    ```
  - 每个组件单独一条规则，违规消息精确到哪个组件依赖了哪个
- **原因**：6 个 component 只依赖 common，互相耦合会破坏可插拔设计

#### M-03：Facade 方法返回值不在 entity 包

- **来源**：`AGENTS.md` 规则 1
- **强度**：⛔ MUST
- **检测方式**：ArchUnit
- **API**：自定义 `ArchCondition<JavaClass>`
- **检测逻辑**：
  - 遍历 facade 包下所有类的 public 方法
  - 检查返回类型的包名
  - 如果返回类型包名包含 `..entity.` 但不包含 `..entity.api..` → 违规
  - `entity.api` 包下是 DTO/VO（允许返回）
- **排除**：void 返回类型、private 方法
- **原因**：防止内部 Entity 泄露到 Controller 层

---

### 维度三：Spring 配置约束（SpringConfigComplianceUTest）

#### S-01：组件 `@ConfigurationProperties` 前缀一致性

- **来源**：`configuration.md` 规则 2
- **强度**：⛔ MUST
- **检测方式**：ArchUnit
- **范围**：仅 `components` 模块下的类
- **API**：
  ```java
  ArchRuleDefinition.classes()
      .that().resideInAPackage("..component..")
      .and().areAnnotatedWith(ConfigurationProperties.class)
      .should(havePrefixStartingWith("component."))
  ```
- **约束**：component 模块下的 Properties 类前缀必须以 `component.` 开头
- **排除**：app 模块的 Properties 类不受约束（`app`、`logging`、`thread-pool` 等前缀都合法）
- **实现要点**：从 `@ConfigurationProperties` 注解获取 `prefix` 或 `value` 属性值检查

---

### 维度四：测试规范（TestConventionComplianceUTest）

#### T-01：测试文件名 `*UTest` / `*ITest`

- **来源**：`testing-conventions.md` 规则 1
- **强度**：⛔ MUST
- **检测方式**：源码扫描
- **扫描范围**：`src/test/java` 下所有 `*.java` 文件
- **检测逻辑**：
  - 文件名不以 `UTest.java` 或 `ITest.java` 结尾
  - 且文件内容包含 `@Test` 或 `@ParameterizedTest` → 违规
- **排除**：
  - 基类文件（`UnitTestBase`, `IntegrationTestBase`, `*Support*` 等）
  - `package-info.java`
  - 不含 `@Test` 的纯工具类

#### T-02：禁止 UTest 中使用 `@SpringBootTest`

- **来源**：`testing-conventions.md` 规则 2
- **强度**：⛔ MUST
- **检测方式**：源码扫描
- **扫描范围**：`src/test/java` 下 `*UTest.java` 文件
- **检测逻辑**：
  - 文件名以 `UTest.java` 结尾
  - 文件内容包含 `@SpringBootTest` → 违规
- **说明**：UTest 是纯单元测试，不应启动 Spring 容器

#### T-03：禁止 ITest 中使用 `@Mock`

- **来源**：`testing-conventions.md` 规则 2
- **强度**：⛔ MUST
- **检测方式**：源码扫描
- **扫描范围**：`src/test/java` 下 `*ITest.java` 文件
- **检测逻辑**：
  - 文件名以 `ITest.java` 结尾
  - 文件内容包含 `@Mock`（独立注解，非 `@MockBean`）→ 违规
  - 排除 import 语句中的 `@Mock`
- **说明**：ITest 是集成测试，应使用真实依赖

---

## 源码扫描工具类设计

提取 `NoDataAnnotationUTest` 和 `NoValueInjectionUTest` 的共用模式为 `SourceScanner.java`：

```java
package org.smm.archetype.support.basic;

/**
 * 源码扫描工具类，用于检测编译期被 Lombok 消除的注解和字符串模式。
 * ArchUnit 无法检测这些模式，因此使用源码文件扫描。
 */
class SourceScanner {

    static final String PROJECT_ROOT = System.getProperty("user.dir", ".").replace("/app", "");

    /**
     * 扫描 src/main/java 下的所有 Java 文件，返回满足条件的违规文件列表。
     */
    static List<String> scanMainSource(Predicate<Path> fileFilter, Predicate<List<String>> contentMatcher) { ... }

    /**
     * 扫描 src/test/java 下的所有 Java 文件，返回满足条件的违规文件列表。
     */
    static List<String> scanTestSource(Predicate<Path> fileFilter, Predicate<List<String>> contentMatcher) { ... }

    /**
     * 判断一行是否是注释行。
     */
    static boolean isCommentLine(String line) { ... }

    /**
     * 判断一行是否是 import 语句。
     */
    static boolean isImportLine(String line) { ... }
}
```

---

## 与现有测试的关系

| 现有测试 | 新增规则是否有重叠 | 处理方式 |
|---------|----------------|---------|
| `ArchitectureComplianceUTest`（6 条） | 无重叠，完全是增量 | 保持不动 |
| `NoDataAnnotationUTest`（禁止 @Data） | C-07 是子集（Properties 禁止 @Data） | 保留 NoDataAnnotationUTest 不动，C-07 精确补充 |
| `NoValueInjectionUTest`（禁止 @Value） | 无重叠 | 保持不动 |
| `NoRedundantConfigureUTest` | 无重叠 | 保持不动 |
| `ApplicationStartupITest` | 无重叠 | 保持不动 |

---

## 实施优先级

1. **先实施工具类** `SourceScanner.java`（被多个规则依赖）
2. **先实施 ArchUnit 规则**（C-02, C-04, C-06, C-07, M-01~M-03, S-01），因为 ArchUnit API 测试编写更快、更稳定
3. **再实施源码扫描规则**（C-01, C-03, C-05, T-01~T-03），复用 SourceScanner
4. **最后验证全量测试通过**
