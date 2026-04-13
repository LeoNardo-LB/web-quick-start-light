## Context

web-quick-start-light 是一个基于 Spring Boot 3.x 的三层架构 Maven Archetype 骨架项目（Controller → Service → Repository），当前存在以下技术债务：

- 使用 Spring Data JPA，复杂查询控制力不足
- 异常体系仅有单一 BizException，无法区分业务/外部/系统错误
- 无线程上下文传递机制（UserContext 硬编码返回 "system"）
- 无 HTTP 请求参数校验
- 单一 application.yaml，无环境分离
- 无测试规范和架构守护
- 对象转换基于运行时反射（MyBeanUtils）

参考项目 speccoding-fullstack/backend 采用了 DDD 四层架构，其基础设施层有许多优秀设计可以在三层架构中复用。本次变更的核心约束是**保持三层架构不变**，仅引入基础设施层面的能力提升。

## Goals / Non-Goals

**Goals:**
- 建立完善的三级异常体系和 ErrorCode 接口规范
- 引入 ScopedValue 线程上下文传递，支持 traceId 全链路追踪
- 引入请求参数校验机制，前置拦截非法参数
- 替换 JPA 为 MyBatis-Plus，提升 SQL 控制力
- 引入 MapStruct 编译期安全对象转换
- 建立多环境配置体系和类型安全属性管理
- 引入技术客户端接口抽象，为缓存、邮件、短信、存储、搜索提供端口
- 建立测试基类体系和架构合规测试
- 统一响应增加 time 字段

**Non-Goals:**
- **不改变三层架构**为 DDD/六边形/Clean Architecture
- **不引入领域事件体系**（过于重量级，三层中 ROI 不高）
- **不引入 CQRS 模式**（Service 层保持读写混合，不做 CommandService/QueryService 拆分）
- **不引入值对象模式**（三层中用标准 Entity/DTO 即可）
- **不引入事件重试调度器**（无事件体系则不需要）
- **不改变项目为多模块结构**（保持单模块骨架的简洁性）

## Decisions

### Decision 1: MyBatis-Plus 替换 Spring Data JPA

**选择**: MyBatis-Plus 3.5.x + XML Mapper

**理由**:
- SQL 控制力强，复杂查询友好（LambdaQueryWrapper、自定义 SQL）
- MyBatis-Plus 提供 BaseMapper 通用 CRUD，与 JPA Repository 等价
- MyMetaObjectHandler 自动填充审计字段比 JPA @PrePersist 更优雅
- MyBatis-Plus 在国内 Spring Boot 项目中使用率远高于 JPA
- 骨架项目面向的用户群体更熟悉 MyBatis 系列

**备选方案**:
- 保持 JPA：SQL 控制力弱，N+1 问题多，学习曲线陡峭
- JPA + QueryDSL：引入额外复杂度，不符合"轻量"定位

### Decision 2: MapStruct 替代反射式 MyBeanUtils

**选择**: MapStruct 1.5.x 编译期代码生成

**理由**:
- 编译期类型安全，IDE 友好
- 无运行时反射开销
- 参考项目中已验证其可靠性
- @Mapper(componentModel = "spring") 可直接注入 Spring Bean

**备选方案**:
- 保留 MyBeanUtils：运行时反射，性能差，无编译期安全
- BeanUtils.copyProperties：同样是反射，无转换逻辑定制能力

### Decision 3: ScopedValue 替代 ThreadLocal

**选择**: Java 21+ ScopedValue（通过 ScopedThreadContext 封装）

**理由**:
- 虚拟线程友好，ScopedValue 是 Java 21+ 推荐的上下文传递方式
- 请求级自动清理，无需手动 remove
- 参考项目已验证其可行性
- 项目已使用 Java 25，完全支持

**备选方案**:
- ThreadLocal：虚拟线程场景下有内存泄漏风险
- InheritableThreadLocal：线程池场景下传递不可靠
- TransmittableThreadLocal (阿里)：额外依赖，非 JDK 标准

### Decision 4: 三级异常体系

**选择**: BizException / ClientException / SysException + ErrorCode 接口

**理由**:
- 语义清晰：业务异常 vs 外部服务异常 vs 系统内部错误
- ErrorCode 接口化支持按模块扩展
- 全局异常处理器可按异常类型区分 HTTP 状态码
- 前端可按 code 前缀分段处理不同错误类型

**异常到 BaseResult.code 映射**（所有异常统一返回 HTTP 200，通过 BaseResult.code 区分错误类型）：
- BizException → BaseResult.code = ErrorCode.code（如 2000）
- ClientException → BaseResult.code = ErrorCode.code（如 2002）
- SysException → BaseResult.code = 5000
- MethodArgumentNotValidException → BaseResult.code = 2001
- ConstraintViolationException → BaseResult.code = 2001
- Exception(兜底) → BaseResult.code = 9999

**选择统一 HTTP 200 的理由**：前端可以统一用相同的 JSON 解析逻辑处理所有响应，无需按 HTTP 状态码分支处理。错误类型通过 BaseResult.code 区分。

### Decision 5: 技术客户端接口放在 service 包下

**选择**: 在 service/ 下新建 client/ 子包存放技术客户端接口

**理由**:
- 三层架构中 Service 层是业务逻辑的核心
- 技术客户端接口本质是"可以被替换的外部服务依赖"
- 放在 Service 层符合三层架构的分层约定（Service 依赖接口，基础设施实现接口）
- 实现类放在独立的 infrastructure/ 包或通过 @Configuration 注册 Bean

### Decision 6: 配置类集中管理

**选择**: 在 config/ 包下集中管理 @Configuration 和 @ConfigurationProperties

**理由**:
- 参考项目的 start 模块配置集中管理模式优秀
- 单模块项目中 config/ 包是最自然的配置集中点
- @ConfigurationProperties 提供类型安全配置
- 多环境 yaml 提供环境隔离

### Decision 7: 测试体系

**选择**: UnitTestBase（Mockito）+ IntegrationTestBase（SpringBootTest）+ ArchUnit

**理由**:
- UT/IT 分层是业界最佳实践
- ArchUnit 可在三层架构中守护"Controller 不直接调用 Repository"等规则
- 测试基类降低测试编写门槛

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|---------|
| JPA → MyBatis-Plus 是 BREAKING 变更，所有 Repository 需重写 | 骨架项目本身代码量小，迁移成本低；提供完整的示例代码 |
| ScopedValue 要求 Java 21+ | 项目已使用 Java 25，无兼容性问题 |
| MapStruct 需要 annotationProcessor 配置 | 在 pom.xml 中正确配置 maven-compiler-plugin |
| 技术客户端接口暂无实现 | 仅定义接口 + 默认空实现/日志实现，具体实现在使用时按需引入 |
| 引入能力过多导致骨架膨胀 | 每个能力都是独立的包/类，不影响使用时的按需裁剪 |
| 三层架构中使用 DDD 风格的端口接口可能显得过度设计 | 接口仅作为依赖倒置的手段，保持简单，不引入领域概念 |
