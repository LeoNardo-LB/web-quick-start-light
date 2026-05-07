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
| `ModuleArchitectureComplianceUTest.java` | 模块/架构约束 | M-01~M-04（4 条） |
| `SpringConfigComplianceUTest.java` | Spring 配置约束 | S-01（1 条） |
| `TestConventionComplianceUTest.java` | 测试规范 | T-01~T-03（3 条） |

所有文件位于 `app/src/test/java/org/smm/archetype/support/basic/`，与现有 5 个守护测试并列。

### 检测策略

| 策略 | 适用规则 | 说明 |
|------|---------|------|
| **ArchUnit API** | C-02, C-04, C-06, M-01~M-04, S-01 | 注解、字段类型、依赖、返回类型检查 |
| **源码扫描** | C-01, C-03, C-05, C-07, T-01~T-03 | Lombok 注解（编译期消除）、字符串模式 |

### 共用基础设施

- 所有测试继承 `UnitTestBase`（`@ExtendWith(MockitoExtension.class)`），纯内存运行
- 源码扫描工具类 `SourceScanner.java`：复用 `NoDataAnnotationUTest` 的 `PROJECT_ROOT` + `Files.walk` 模式，提取为可复用工具
- **不使用 FreezingArchRule**（项目规模小，直接严格检查）
- **技术栈要求**：ArchUnit 1.4.1（已声明于根 pom.xml dependencyManagement）+ Java 25。ArchUnit 1.4.1 使用 ASM 9.x，已验证支持 Java 25 的 class file format。

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
- **实现要点**：使用正则 `\bLocalDateTime\s+\w+` 匹配字段声明模式（类型名后跟空格和标识符），避免误判方法参数和局部变量。逐行扫描时跳过 `import` 行、`//` 行、以及 `/* */` 块注释内的行（由 SourceScanner 的多行注释状态机处理）。

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
      .that().resideInAPackage("..facade..")
      .and().haveSimpleNameEndingWith("VO")
      .or().haveSimpleNameEndingWith("DTO")
      .should(ArchConditions.beRecords())
  ```
- **检测逻辑**：facade 包下以 VO/DTO 结尾的类必须是 Java record
- **排除**：枚举、接口、抽象类（由 `beRecords()` 内部自动处理）
- **⚠️ SHOULD 实现方案**：ArchUnit 原生不支持 WARN 语义，采用 try-catch 策略：
  ```java
  @Test
  void dto_vo_should_use_record() {
      try {
          ArchRuleDefinition.classes()
              .that().resideInAPackage("..facade..")
              .and().haveSimpleNameEndingWith("VO")
              .or().haveSimpleNameEndingWith("DTO")
              .should(ArchConditions.beRecords())
              .check(importedClasses);
      } catch (AssertionError e) {
          logger.warn("C-06 SHOULD 违规（不阻塞 CI）: {}", e.getMessage());
      }
  }
  ```
- **ArchUnit 版本要求**：`beRecords()` 自 ArchUnit 0.18.0 起可用，当前项目使用 1.4.1 已满足

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
   - 动态发现组件列表（从 importedClasses 中提取 `component.*` 的一级子包），无需硬编码：
     ```java
     Set<String> COMPONENTS = importedClasses.stream()
         .map(c -> c.getPackageName())
         .filter(p -> p.contains(".component."))
         .map(p -> p.split("\\.component\\.")[1].split("\\.")[0])
         .collect(Collectors.toSet());
     for (String component : COMPONENTS) {
         String[] otherPackages = COMPONENTS.stream()
             .filter(c -> !c.equals(component))
             .map(c -> "..component." + c + "..")
             .toArray(String[]::new);
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
- **ArchCondition 实现骨架**：
  ```java
  private static ArchCondition<JavaClass> notReturnInternalEntity() {
      return new ArchCondition<>("not return internal entity types") {
          @Override
          public void check(JavaClass clazz, ConditionEvents events) {
              for (JavaMethod method : clazz.getMethods()) {
                  if (!method.getModifiers().contains(JavaModifier.PUBLIC)) continue;
                  JavaType returnType = method.getReturnType();
                  if (returnType.getName().equals("void")) continue;
                  String returnTypeName = returnType.getName();
                  // 检查返回类型是否在 entity 包下且不是 VO/DTO
                  if (returnTypeName.contains(".entity.") 
                      && !returnTypeName.endsWith("VO") 
                      && !returnTypeName.endsWith("DTO")) {
                      events.add(SimpleConditionEvent.violated(method,
                          String.format("%s.%s() 返回内部 Entity 类型: %s",
                              clazz.getSimpleName(), method.getName(), returnTypeName)));
                  }
              }
          }
      };
  }
  ```
- **检测逻辑**：
  - 遍历 facade 包下所有类的 public 方法
  - 检查返回类型的包名
  - 如果返回类型包名包含 `..entity.` 且不以 `VO`/`DTO` 结尾 → 违规
  - VO/DTO 类（如 SystemConfigVO）允许返回（它们是对外契约）
- **排除**：void 返回类型、private 方法
- **原因**：防止内部 Entity 泄露到 Controller 层

#### M-04：API 路径以 `/api` 开头

- **来源**：`java-conventions.md` 规则 6
- **强度**：⚠️ SHOULD
- **检测方式**：ArchUnit
- **API**：
  ```java
  ArchRuleDefinition.classes()
      .that().areAnnotatedWith("org.springframework.web.bind.annotation.RequestMapping")
      .or().areAnnotatedWith("org.springframework.web.bind.annotation.GetMapping")
      .or().areAnnotatedWith("org.springframework.web.bind.annotation.PostMapping")
      .or().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
      .should(havePathStartingWith("/api"))
  ```
- **检测逻辑**：Controller 类上的 @RequestMapping/@GetMapping/@PostMapping 等注解的 value/path 属性必须以 `/api` 开头
- **⚠️ SHOULD 实现方案**：同 C-06 的 try-catch 策略，违规时 WARN 不 FAIL
- **说明**：统一 API 前缀，方便网关路由和前端对接

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
- **实现要点**：从 `@ConfigurationProperties` 注解获取 `prefix` 属性值检查（Spring Boot 4.x 仅支持 `prefix`，`value` 别名已于 Spring Boot 3.0 移除）

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

    static final String PROJECT_ROOT = computeProjectRoot();

    /**
     * 健壮的 PROJECT_ROOT 计算：
     * 从 user.dir 向上搜索包含根 pom.xml 的目录。
     * 比 replace("/app", "") 更可靠，兼容从 IDE 或任意目录运行。
     */
    private static String computeProjectRoot() { ... }

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
     * 包括 import xxx 和 import static xxx。
     */
    static boolean isImportLine(String line) { ... }
}
```

### SourceScanner 验收标准

SourceScanner 作为 7 条源码扫描规则的共同依赖，必须通过以下单元测试：

| 测试用例 | 输入 | 预期 |
|---------|------|------|
| `isCommentLine("// comment")` | `// comment` | `true` |
| `isCommentLine("* javadoc line")` | `* javadoc` | `true` |
| `isCommentLine("code // inline")` | 含行尾注释 | `false`（代码行） |
| `isCommentLine("import foo.Bar;")` | import 行 | `false`（不是注释） |
| `isImportLine("import foo.Bar;")` | 标准导入 | `true` |
| `isImportLine("import static foo.Bar.baz;")` | 静态导入 | `true` |
| `scanMainSource` 在无匹配文件时 | 空项目 | 返回空列表 |
| `scanMainSource` 遇到非 UTF-8 文件 | GBK 文件 | 记录警告并跳过（不抛异常） |

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
2. **先实施 ArchUnit 规则**（C-02, C-04, C-06, M-01~M-04, S-01），因为 ArchUnit API 测试编写更快、更稳定
3. **再实施源码扫描规则**（C-01, C-03, C-05, C-07, T-01~T-03），复用 SourceScanner
4. **最后验证全量测试通过**

---

## 规则汇总索引

| 编号 | 规则名称 | 强度 | 检测方式 | 所在文件 |
|------|---------|------|---------|---------|
| C-01 | 时间字段统一 `Instant` | ⛔ MUST | 源码扫描 | CodingConventionComplianceUTest |
| C-02 | 禁止 JPA/Hibernate 注解 | ⛔ MUST | ArchUnit | CodingConventionComplianceUTest |
| C-03 | 禁止 `BeanUtils.copyProperties` | ⛔ MUST | 源码扫描 | CodingConventionComplianceUTest |
| C-04 | 禁止 `System.out`/`System.err` | ⛔ MUST | ArchUnit | CodingConventionComplianceUTest |
| C-05 | 禁止 `@With`（Lombok） | ⛔ MUST | 源码扫描 | CodingConventionComplianceUTest |
| C-06 | DTO/VO 使用 `record` | ⚠️ SHOULD | ArchUnit | CodingConventionComplianceUTest |
| C-07 | Properties 类禁止 `@Data` | ⛔ MUST | 源码扫描 | CodingConventionComplianceUTest |
| M-01 | `common` 模块零 Spring 依赖 | ⛔ MUST | ArchUnit | ModuleArchitectureComplianceUTest |
| M-02 | 组件模块间零互相依赖 | ⛔ MUST | ArchUnit | ModuleArchitectureComplianceUTest |
| M-03 | Facade 返回值不在 entity 包 | ⛔ MUST | ArchUnit | ModuleArchitectureComplianceUTest |
| M-04 | API 路径以 `/api` 开头 | ⚠️ SHOULD | ArchUnit | ModuleArchitectureComplianceUTest |
| S-01 | 组件 ConfigurationProperties 前缀 | ⛔ MUST | ArchUnit | SpringConfigComplianceUTest |
| T-01 | 测试文件名 `*UTest`/`*ITest` | ⛔ MUST | 源码扫描 | TestConventionComplianceUTest |
| T-02 | 禁止 UTest 中 `@SpringBootTest` | ⛔ MUST | 源码扫描 | TestConventionComplianceUTest |
| T-03 | 禁止 ITest 中 `@Mock` | ⛔ MUST | 源码扫描 | TestConventionComplianceUTest |**
