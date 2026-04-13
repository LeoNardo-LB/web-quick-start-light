## ADDED Requirements

### Requirement: UnitTestBase 单元测试基类
系统 SHALL 提供 UnitTestBase 抽象类，使用 `@ExtendWith(MockitoExtension.class)` 注解，不启动 Spring 上下文。

UnitTestBase SHALL 提供：
- Mockito 的 @Mock / @InjectMocks 支持
- 通用断言辅助方法

#### Scenario: 编写纯单元测试
- **WHEN** 开发者需要测试一个 Service 类的逻辑
- **THEN** 继承 UnitTestBase，使用 @Mock 模拟依赖，使用 @InjectMocks 注入被测对象，无需启动 Spring

### Requirement: IntegrationTestBase 集成测试基类
系统 SHALL 提供 IntegrationTestBase 抽象类，使用 `@SpringBootTest` 注解，启动完整 Spring 上下文。

IntegrationTestBase SHALL 提供：
- WebTestClient 用于发送 HTTP 请求
- 测试专用的 application.yaml 配置
- 测试专用的 logback-test.xml 日志配置

#### Scenario: 编写集成测试
- **WHEN** 开发者需要测试 Controller → Service → Repository 的完整链路
- **THEN** 继承 IntegrationTestBase，使用 WebTestClient 发送请求并验证响应

### Requirement: ArchUnit 架构合规测试
系统 SHALL 提供 ArchitectureComplianceUTest，使用 ArchUnit 强制执行以下架构规则：
- Controller 层只能依赖 Service 层，不能直接依赖 Repository 层
- Service 层不能依赖 Controller 层
- Repository 层不能依赖 Service 层和 Controller 层
- Entity 层不能依赖 Spring 框架类

#### Scenario: Controller 直接调用 Repository 被检测
- **WHEN** 开发者在 Controller 中直接注入了 Repository
- **THEN** ArchUnit 测试失败，提示架构违规

#### Scenario: Service 返回 Controller 层类型被检测
- **WHEN** 开发者在 Service 中 import 了 Controller 层的类
- **THEN** ArchUnit 测试失败

### Requirement: 测试命名规范
测试类 SHALL 遵循命名规范：
- 单元测试类以 `UTest` 后缀结尾（如 SystemConfigUTest）
- 集成测试类以 `ITest` 后缀结尾（如 HealthCheckITest）
- 测试方法使用 `should_xxx_when_xxx` 格式

#### Scenario: 正确的测试类命名
- **WHEN** 创建一个 UserService 的单元测试
- **THEN** 测试类命名为 UserServiceUTest，继承 UnitTestBase
