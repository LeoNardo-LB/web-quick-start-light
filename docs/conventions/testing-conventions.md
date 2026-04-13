# 测试规范

> 🔵 Constraint 轨 — 团队共识，文档驱动代码

## 📋 目录

- [规范目的](#规范目的)
- [规则](#规则)
- [常见违规场景](#常见违规场景)
- [检查清单](#检查清单)
- [相关文档](#相关文档)

## 规范目的

确保测试分类清晰、命名统一、覆盖率达标，通过自动化测试保障代码质量和重构安全性。

## 规则

### 规则 1: 测试分类命名

**⛔ MUST**

测试分为单元测试和集成测试两类，通过文件名后缀区分：

| 类型 | 后缀 | 基类 | 扩展 | 运行环境 | 速度 |
|------|------|------|------|----------|------|
| 单元测试 | `*UTest` | `UnitTestBase` | `@ExtendWith(MockitoExtension.class)` | 纯内存，Mock 依赖 | 毫秒级 |
| 集成测试 | `*ITest` | `IntegrationTestBase` | `@SpringBootTest` + `@ActiveProfiles("test")` | 完整 Spring 上下文 + SQLite | 秒级 |

**基类源码位置**：`app/src/test/java/org/smm/archetype/support/`

```java
// UnitTestBase — Mockito 环境
@ExtendWith(MockitoExtension.class)
public abstract class UnitTestBase {}

// IntegrationTestBase — Spring 上下文 + WebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class IntegrationTestBase {
    @LocalServerPort
    protected int port;

    @Value("${server.servlet.context-path:/}")
    protected String contextPath;

    protected WebTestClient webTestClient;

    @Autowired
    protected ApplicationContext applicationContext;
}
```

✅ 正确：
```
app/src/test/java/org/smm/archetype/cases/
├── system/
│   ├── SystemConfigServiceUTest.java        // 单元测试
│   └── SystemConfigControllerITest.java     // 集成测试
├── auth/
│   └── LoginServiceUTest.java               // 单元测试
└── operationlog/
    └── OperationLogServiceUTest.java        // 单元测试
```

❌ 错误：
```
├── SystemConfigServiceTest.java    // 缺少 U/ITest 后缀
├── SystemConfigTests.java          // 后缀不规范
└── TestSystemConfig.java           // 前缀不规范
```

> **为什么**：UTest 和 ITest 的运行环境完全不同——UTest 使用 Mockito Mock 所有依赖，毫秒级完成，适合验证纯业务逻辑；ITest 启动完整 Spring 上下文和 SQLite 数据库，秒级完成，适合验证端到端集成。统一的 `*UTest` / `*ITest` 后缀使得 `mvn test -Dtest="*UTest"` 可以精确筛选测试类型，避免开发调试时不必要地启动 Spring 上下文，大幅提升开发效率。

### 规则 2: Mock 规范

**⛔ MUST**

根据测试类型选择合适的 Mock 策略：

| 测试类型 | Mock 策略 | 适用场景 |
|----------|-----------|----------|
| `*UTest` | Mockito Mock 所有依赖 | 纯业务逻辑验证 |
| `*ITest` | 真实 Spring 上下文（含 SQLite） | 端到端集成验证 |

**UTest 要求**：
- 继承 `UnitTestBase`（自动启用 MockitoExtension）
- 使用 `@Mock` 注入 Mock 对象
- 使用 `@InjectMocks` 创建被测对象
- 不加载 Spring 上下文

**ITest 要求**：
- 继承 `IntegrationTestBase`（自动启用 SpringBootTest + test profile）
- 使用 `@Autowired` 注入真实 Bean
- 使用 `webTestClient` 发送 HTTP 请求
- 不使用 `@Mock`（所有 Bean 都是真实的）

✅ 正确（UTest）：
```java
class SystemConfigServiceUTest extends UnitTestBase {

    @Mock
    private SystemConfigRepository systemConfigRepository;

    @InjectMocks
    private SystemConfigService systemConfigService;

    @Test
    void getByKey_shouldReturnConfig() {
        // Arrange
        SystemConfig config = SystemConfig.builder()
                .configKey("test.key").configValue("test.value").build();
        when(systemConfigRepository.getByKey("test.key")).thenReturn(config);

        // Act
        SystemConfig result = systemConfigService.getByKey("test.key");

        // Assert
        assertThat(result.getConfigValue()).isEqualTo("test.value");
    }
}
```

✅ 正确（ITest）：
```java
class SystemConfigControllerITest extends IntegrationTestBase {

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Test
    void getConfigs_shouldReturnList() {
        webTestClient.get()
                .uri("/api/system/configs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(1000);
    }
}
```

❌ 错误：
```java
// UTest 中加载 Spring 上下文
@SpringBootTest  // 禁止！UTest 不应加载 Spring
class SystemConfigServiceUTest extends UnitTestBase {}

// ITest 中使用 @Mock
@Mock  // 禁止！ITest 应使用真实 Bean
private SystemConfigRepository repository;
```

> **为什么**：UTest 的核心价值在于隔离性——Mock 所有依赖可以精确控制被测方法的输入输出，且无需等待 Spring 上下文启动（通常 2-5 秒）。如果在 UTest 中加载 Spring 上下文，不仅失去速度优势，还引入了 Spring 配置错误干扰业务逻辑测试的风险。反之，ITest 中使用 `@Mock` 则使集成测试失去验证真实 Bean 装配和数据库交互的意义。

### 规则 3: 覆盖率标准

**⛔ MUST**

覆盖率是代码质量的硬性指标，必须满足以下阈值：

| 指标 | 最低阈值 | 目标 |
|------|----------|------|
| Instruction 覆盖率 | ≥ 95% | 100% |
| Branch 覆盖率 | ≥ 90% | 100% |

> 此标准与 `post-dev-verification` 技能的质量门禁一致。

当前 JaCoCo 配置 `haltOnFailure=false`（仅警告），达标后改为 `true` 启用门禁。

> **为什么**：覆盖率低于阈值意味着关键业务路径（如异常分支、边界条件、空值处理）未被测试覆盖，这些未覆盖的代码恰好是 bug 的高发区域。Instruction ≥ 95% 确保几乎所有代码行都被执行过；Branch ≥ 90% 确保条件分支的各路径都被验证过。两个指标互补——高行覆盖但低分支覆盖说明 if/else 分支没测全。

### 规则 4: 覆盖率报告

**⚠️ SHOULD**

生成覆盖率报告的命令和查看方式：

```bash
# 运行测试 + 生成覆盖率报告
mvn clean verify

# 查看聚合覆盖率报告
# 浏览器打开：app/target/site/jacoco-aggregate/index.html
```

JaCoCo 配置说明：
- **版本管理**：根 POM `<jacoco.version>` + `<pluginManagement>`
- **prepare-agent + report**：根 POM 配置，所有子模块自动继承
- **report-aggregate + check**：app 模块配置，聚合报告输出到 `app/target/site/jacoco-aggregate/`

### 规则 5: 测试命令

**⚠️ SHOULD**

| 命令 | 用途 |
|------|------|
| `mvn test` | 运行全部测试（UTest + ITest） |
| `mvn test -Dtest="*UTest"` | 仅运行单元测试 |
| `mvn test -Dtest="*ITest"` | 仅运行集成测试 |
| `mvn clean verify` | 运行测试 + 生成覆盖率报告 |
| `mvn test -pl client-cache` | 仅运行指定模块测试 |

日常开发流程建议：
1. 编写业务代码时同步编写 `*UTest`
2. 提交前运行 `mvn test` 确保全部通过
3. 合并前运行 `mvn clean verify` 检查覆盖率

### 规则 6: 条件装配测试

**💡 MAY**

客户端模块使用 `@ConditionalOnClass` + `@ConditionalOnProperty` 实现条件装配，测试时需验证以下场景：

| 场景 | 验证内容 |
|------|----------|
| 条件满足 | Bean 正确注册 |
| 条件不满足 | NoOp 默认实现生效 |
| 属性缺失 | 使用默认值 |

示例（client-email 条件装配）：
```java
// 条件满足 — 实际实现注册
@SpringBootTest(classes = EmailAutoConfiguration.class)
@ActiveProfiles("test")
class EmailAutoConfigurationITest extends IntegrationTestBase {
    @Autowired
    private EmailClient emailClient;

    @Test
    void shouldRegisterRealClient_whenMailJarPresent() {
        assertThat(emailClient).isNotInstanceOf(NoOpEmailClient.class);
    }
}
```

## 常见违规场景

### 场景 1: 在 UTest 中使用 @SpringBootTest

❌ 错误做法：
```java
@SpringBootTest  // 禁止！UTest 不应启动 Spring 上下文
@ActiveProfiles("test")
class SystemConfigServiceUTest extends UnitTestBase {

    @Autowired  // 禁止！UTest 应使用 @InjectMocks
    private SystemConfigService systemConfigService;

    @Test
    void getByKey_shouldReturnConfig() {
        // 每次运行都要等 2-5 秒启动 Spring 上下文
        // 且依赖数据库状态，测试不稳定
    }
}
```

✅ 正确做法：
```java
class SystemConfigServiceUTest extends UnitTestBase {

    @Mock
    private SystemConfigRepository systemConfigRepository;

    @InjectMocks
    private SystemConfigService systemConfigService;

    @Test
    void getByKey_shouldReturnConfig() {
        // Arrange — 精确控制 Mock 行为
        SystemConfig config = SystemConfig.builder()
                .configKey("test.key").configValue("test.value").build();
        when(systemConfigRepository.getByKey("test.key")).thenReturn(config);

        // Act
        SystemConfig result = systemConfigService.getByKey("test.key");

        // Assert
        assertThat(result.getConfigValue()).isEqualTo("test.value");
    }
}
```

> **后果**：UTest 中启动 Spring 上下文导致测试套件执行时间从秒级变为分钟级，开发体验急剧下降。且 Spring 上下文中的 Bean 依赖关系可能导致测试间相互影响，产生难以复现的 flaky test。

### 场景 2: 测试方法名不遵循 shouldXxxWhenYyy 命名

❌ 错误做法：
```java
@Test
void testGetByKey() { }         // 含义模糊，不知道测试什么场景

@Test
void test1() { }                // 完全无意义

@Test
void getByKey() { }             // 看起来像业务方法，不像测试
```

✅ 正确做法：
```java
@Test
void getByKey_shouldReturnConfig_whenKeyExists() {
    // 测试场景清晰：当 key 存在时，应返回对应配置
}

@Test
void getByKey_shouldThrowBizException_whenKeyNotFound() {
    // 测试场景清晰：当 key 不存在时，应抛出业务异常
}

@Test
void createConfig_shouldRejectDuplicateKey_whenKeyAlreadyExists() {
    // 测试场景清晰：当 key 已存在时，应拒绝创建
}
```

> **后果**：模糊的测试方法名使得测试失败时无法快速判断失败原因，必须阅读测试代码才能理解意图。`shouldXxxWhenYyy` 命名让测试报告本身就是可读的文档。

## 检查清单

- [ ] 单元测试命名为 `*UTest`，继承 `UnitTestBase`
- [ ] 集成测试命名为 `*ITest`，继承 `IntegrationTestBase`
- [ ] UTest 使用 Mockito Mock，不加载 Spring 上下文
- [ ] ITest 使用真实 Spring 上下文，不使用 `@Mock`
- [ ] Instruction 覆盖率 ≥ 95%，Branch 覆盖率 ≥ 90%
- [ ] 覆盖率报告可通过 `mvn clean verify` 生成
- [ ] 条件装配的客户端模块验证了装配条件

## 相关文档

- [Java 编码规范](./java-conventions.md) — Lombok / 命名规范
- [AGENTS.md](../../AGENTS.md) — 测试命令与 JVM 配置
