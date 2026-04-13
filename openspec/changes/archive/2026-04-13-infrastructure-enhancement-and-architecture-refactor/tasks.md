## 0. 预备

- [x] 0.1 确认 test-driven-development、dispatching-parallel-agents 技能已加载（未加载则加载，已加载则跳过）

## 1. Phase 0: API 版本控制 + 统一前缀

### 1.1 API 版本控制配置

- [x] 1.1.1 RED: 编写 API 版本控制集成测试（ApiVersioningITest），覆盖：请求携带 API-Version Header 匹配特定版本方法、不携带 Header 访问无版本标注方法、携带不匹配版本返回兜底或 404
- [x] 1.1.2 GREEN: 在 application.yaml 配置 `spring.mvc.apiversion.use-header: "API-Version"`，创建 WebConfigure 中的 `configureApiVersioning` 配置（如果需要），确保基础路由可用
- [x] 1.1.3 REFACTOR: 调整版本控制配置，确保与统一前缀协同工作

### 1.2 统一 /api 前缀

- [x] 1.2.1 RED: 编写 API 前缀集成测试（ApiPrefixITest），覆盖：访问带 /api 前缀的端点返回正常、访问不带 /api 前缀的端点返回 404
- [x] 1.2.2 GREEN: 更新所有 Controller 的 @RequestMapping 添加 /api 前缀（如 SystemConfigController → /api/system/configs，TestController → /api/test）
- [x] 1.2.3 REFACTOR: 更新所有集成测试中的 URL 路径以匹配 /api 前缀

### 1.3 Phase 0 测试收尾

- [x] 1.3.1 运行全部测试确认通过：`mvn test`

## 2. Phase 0: client-ratelimit 限流模块

### 2.1 模块骨架

- [x] 2.1.1 创建 clients/client-ratelimit 模块（pom.xml + 目录结构），Bucket4j 为 optional 依赖

### 2.2 @RateLimit 注解 + 枚举

- [x] 2.2.1 RED: 编写 RateLimit 注解单元测试（RateLimitAnnotationUTest），覆盖：注解属性默认值（capacity=10, refillTokens=10, fallback=REJECT）、自定义属性值、注解可标注在方法上
- [x] 2.2.2 GREEN: 创建 @RateLimit 注解（capacity, refillTokens, refillDuration, refillUnit, key, fallback）和 LimitFallback 枚举（REJECT, WAIT, FALLBACK）

### 2.3 RateLimitAspect 切面

- [x] 2.3.1 RED: 编写 RateLimitAspect 单元测试（RateLimitAspectUTest），覆盖：桶内有令牌时方法正常执行、桶内无令牌且 fallback=REJECT 时抛 BizException、桶内无令牌且 fallback=WAIT 时阻塞等待后执行、桶内无令牌且 fallback=FALLBACK 时执行降级方法、SpEL 表达式提取 Key 按维度分别限流
- [x] 2.3.2 GREEN: 实现 RateLimitAspect（AOP @Around），使用 Bucket4j tryConsume + Caffeine 存储 Bucket 实例，SpEL 解析 key 表达式
- [x] 2.3.3 REFACTOR: 提取 BucketFactory（Caffeine Cache 管理 Bucket 实例），SpEL 解析器封装

### 2.4 条件装配 + AutoConfiguration

- [x] 2.4.1 RED: 编写条件装配测试（RateLimitAutoConfigurationUTest），覆盖：Bucket4j 在 classpath 且 enabled=true 时 RateLimitAspect Bean 存在、Bucket4j 不在 classpath 时无 RateLimitAspect Bean
- [x] 2.4.2 GREEN: 创建 RateLimitAutoConfiguration（@ConditionalOnClass + @ConditionalOnProperty）、RateLimitProperties（middleware.ratelimit.*）

### 2.5 Phase 2 测试收尾

- [x] 2.5.1 运行 client-ratelimit 模块全部测试：`mvn test -pl clients/client-ratelimit`

## 3. Phase 0: client-idempotent 幂等模块

### 3.1 模块骨架

- [x] 3.1.1 创建 clients/client-idempotent 模块（pom.xml + 目录结构），依赖 client-cache（Caffeine）

### 3.2 @Idempotent 注解

- [x] 3.2.1 RED: 编写 Idempotent 注解单元测试（IdempotentAnnotationUTest），覆盖：注解属性默认值（timeout=3000, timeUnit=MILLISECONDS, field="", message="请勿重复操作"）、自定义属性值
- [x] 3.2.2 GREEN: 创建 @Idempotent 注解（timeout, timeUnit, field, message）

### 3.3 IdempotentAspect 切面

- [x] 3.3.1 RED: 编写 IdempotentAspect 单元测试（IdempotentAspectUTest），覆盖：首次调用正常执行、幂等窗口内相同 Key 重复调用抛 BizException、幂等窗口过期后可正常调用、field SpEL 表达式提取入参字段作为 Key、field 为空时使用方法签名+参数 hash 作为 Key、不同参数值的调用互不影响
- [x] 3.3.2 GREEN: 实现 IdempotentAspect（AOP @Around），使用 Caffeine Cache 存储 Key，SpEL 解析 field 表达式，Key 组成为 className.methodName(fieldValue)
- [x] 3.3.3 REFACTOR: 提取 IdempotentKeyResolver（SpEL + 默认 hash 策略），封装 Key 生成逻辑

### 3.4 条件装配 + AutoConfiguration

- [x] 3.4.1 RED: 编写条件装配测试（IdempotentAutoConfigurationUTest），覆盖：enabled=true 时 IdempotentAspect Bean 存在、enabled=false 时无 Bean
- [x] 3.4.2 GREEN: 创建 IdempotentAutoConfiguration、IdempotentProperties（middleware.idempotent.*）

### 3.5 Phase 3 测试收尾

- [x] 3.5.1 运行 client-idempotent 模块全部测试：`mvn test -pl clients/client-idempotent`

## 4. Phase 1: Facade 四层架构改造

### 4.1 创建 facade 包结构

- [x] 4.1.1 创建 `app/.../facade/system/` 包目录

### 4.2 SystemConfigFacade 接口 + 实现

- [x] 4.2.1 RED: 编写 SystemConfigFacade 单元测试（SystemConfigFacadeUTest），覆盖：getAllGroups 返回分组列表、getAllConfigs 调用 Service 并将 Entity 转为 VO、getConfigByKey 返回 VO、getConfigByKey 配置不存在时抛 BizException、getConfigsByGroup 按分组返回 VO 列表、updateConfig 调用 Service 更新
- [x] 4.2.2 GREEN: 创建 SystemConfigFacade 接口和 SystemConfigFacadeImpl（注入 SystemConfigService，负责 Entity→VO 转换和业务校验）

### 4.3 VO/Command 迁移

- [x] 4.3.1 将 SystemConfigVO、ConfigGroupVO、UpdateConfigCommand 从 service.system 包移到 facade.system 包（更新所有 import）
- [x] 4.3.2 更新 SystemConfigService：移除 toVO() 方法，方法返回 Entity（SystemConfig）而非 VO

### 4.4 Controller 切换到 Facade

- [x] 4.4.1 RED: 更新 SystemConfigControllerITest，验证 Controller 通过 Facade 正常工作（而非直接调用 Service）
- [x] 4.4.2 GREEN: 更新 SystemConfigController，将 SystemConfigService 依赖替换为 SystemConfigFacade。移除 Controller 中的业务判断逻辑（null check + 失败响应构造移到 Facade）

### 4.5 ArchUnit 规则更新

- [x] 4.5.1 RED: 编写四层架构 ArchUnit 测试（ArchitectureComplianceUTest 新增规则），覆盖：Controller 禁止直接依赖 Service（必须通过 Facade）、Facade 禁止直接依赖 Repository/Mapper
- [x] 4.5.2 GREEN: 在 ArchitectureComplianceUTest 中新增 Facade 层约束规则

### 4.6 Phase 4 测试收尾

- [x] 4.6.1 运行全部测试确认通过：`mvn test`（含 ArchUnit 架构合规性测试）

## 5. Phase 1: client-auth 认证模块

### 5.1 模块骨架

- [x] 5.1.1 创建 clients/client-auth 模块（pom.xml + 目录结构），sa-token-spring-boot4-starter 为 optional 依赖

### 5.2 AuthClient 接口 + AbstractAuthClient

- [x] 5.2.1 RED: 编写 AuthClient 接口契约测试（AuthClientUTest），覆盖：login 返回 token 信息、logout 清除会话、getCurrentUserId 返回用户 ID、isLogin 返回登录状态、checkLogin 未登录时抛异常
- [x] 5.2.2 GREEN: 创建 AuthClient 接口（login/logout/getCurrentUserId/isLogin/checkLogin）、AbstractAuthClient（Template Method，公开方法 final + 参数校验）

### 5.3 NoOpAuthClient

- [x] 5.3.1 RED: 编写 NoOpAuthClient 测试（NoOpAuthClientUTest），覆盖：login 返回 null、logout 无操作、getCurrentUserId 返回 null、isLogin 返回 true、checkLogin 无操作
- [x] 5.3.2 GREEN: 创建 NoOpAuthClient

### 5.4 SaTokenAuthClient

- [x] 5.4.1 RED: 编写 SaTokenAuthClient 集成测试（SaTokenAuthClientITest），覆盖：login 调用 StpUtil.login 返回 token、logout 调用 StpUtil.logout、getCurrentUserId 从 StpUtil.getLoginIdAsString 获取、isLogin 调用 StpUtil.isLogin、checkLogin 调用 StpUtil.checkLogin
- [x] 5.4.2 GREEN: 创建 SaTokenAuthClient（委托 StpUtil 实现认证逻辑）

### 5.5 条件装配 + 路由拦截

- [x] 5.5.1 RED: 编写认证条件装配测试（AuthAutoConfigurationUTest），覆盖：Sa-Token 在 classpath 且 enabled=true 时 SaTokenAuthClient Bean 存在、Sa-Token 不在 classpath 时 NoOpAuthClient Bean 存在、enabled=false 时不加载任何 AuthClient
- [x] 5.5.2 GREEN: 创建 AuthAutoConfiguration（@ConditionalOnClass + @ConditionalOnProperty + @ConditionalOnMissingBean）、AuthProperties（middleware.auth.*）、AuthInterceptorConfigurer（SaInterceptor 路由拦截配置）

### 5.6 SaInterceptor 路由拦截行为

- [x] 5.6.1 RED: 编写 SaInterceptor 路由拦截集成测试（AuthInterceptorITest），覆盖：未登录访问受保护端点（GET /api/system/configs）返回未认证错误、已登录访问受保护端点正常通过、登录接口（POST /api/auth/login）无需认证放行、测试接口（GET /api/test/hello）无需认证放行

### 5.7 ContextFillFilter 改造

- [x] 5.7.1 RED: 编写 ContextFillFilter 改造测试（ContextFillFilterITest），覆盖：已登录请求 ScopedThreadContext.getUserId() 返回真实 userId、未登录请求 userId 为 "ANONYMOUS"、未引入 Sa-Token 时 userId 为 "SYSTEM"
- [x] 5.7.2 GREEN: 改造 ContextFillFilter，注入 AuthClient（optional），从 AuthClient.getCurrentUserId() 获取 userId，未登录时为 "ANONYMOUS"，AuthClient 不存在时回退为 "SYSTEM"

### 5.8 用户表 + 初始化数据

- [x] 5.8.1 创建 user 表 DDL（id, username, password_hash, nickname, status, create_time, update_time）并添加到 schema.sql
- [x] 5.8.2 在 init.sql 中添加默认管理员账户（admin / BCrypt 哈希密码）

### 5.9 User 实体 + Mapper + Repository

- [x] 5.9.1 创建 UserDO（继承 BaseDO）、UserMapper（MyBatis-Plus BaseMapper，代码生成器生成）
- [x] 5.9.2 创建 User Entity（领域模型）、UserRepository 接口 + UserRepositoryImpl

### 5.10 LoginFacade + LoginController

- [x] 5.10.1 RED: 编写 LoginFacade 测试（LoginFacadeUTest），覆盖：正确用户名密码登录成功返回 token、错误密码登录失败抛 BizException、用户不存在登录失败、注销成功
- [x] 5.10.2 GREEN: 创建 LoginFacade 接口 + LoginFacadeImpl（注入 UserRepository + AuthClient），密码使用 BCrypt 校验
- [x] 5.10.3 RED: 编写 LoginController 集成测试（LoginControllerITest），覆盖：POST /api/auth/login 成功返回 token、POST /api/auth/login 失败返回错误、POST /api/auth/logout 成功
- [x] 5.10.4 GREEN: 创建 LoginController（POST /api/auth/login, POST /api/auth/logout）

### 5.11 Phase 5 测试收尾

- [x] 5.11.1 运行 client-auth 模块全部测试：`mvn test -pl clients/client-auth`
- [x] 5.11.2 运行 app 模块全部测试：`mvn test -pl app`

## 6. Phase 2: 操作日志持久化

### 6.1 OperationType 枚举 + @BusinessLog 扩展

- [x] 6.1.1 RED: 编写 OperationType 枚举测试 + BusinessLog 注解扩展测试，覆盖：OperationType 包含 CREATE/UPDATE/DELETE/QUERY/EXPORT/IMPORT、@BusinessLog 新增属性默认值（module="", operation=空, samplingRate=1.0）、旧版 @BusinessLog("描述") 格式向后兼容
- [x] 6.1.2 GREEN: 创建 OperationType 枚举，扩展 @BusinessLog 注解（新增 module, operation, samplingRate 属性）

### 6.2 operation_log 表

- [x] 6.2.1 创建 operation_log 表 DDL（id, trace_id, user_id, module, operation_type, description, method, params, result, execution_time, ip, status, error_message, create_time）并添加到 schema.sql

### 6.3 OperationLogDO + Mapper

- [x] 6.3.1 创建 OperationLogDO（继承 BaseDO）、OperationLogMapper（MyBatis-Plus BaseMapper）

### 6.4 LogAspect 异步 DB 写入

- [x] 6.4.1 RED: 编写 LogAspect 异步写入测试（LogAspectDBUTest），覆盖：方法成功执行后异步写入 status=SUCCESS 的日志记录、方法失败后异步写入 status=ERROR 的日志记录、samplingRate=1.0 时全量写入、samplingRate=0.5 时约 50% 概率写入（统计方法）、Micrometer 指标全量记录不受 samplingRate 影响
- [x] 6.4.2 GREEN: 扩展 LogAspect，在方法执行后通过 ioThreadPool + ContextRunnable 异步写入 operation_log 表。实现采样率逻辑（ThreadLocalRandom.nextDouble() < samplingRate）
- [x] 6.4.3 REFACTOR: 提取 OperationLogWriter 服务，将日志序列化、采样率判断、异步写入封装为独立 Bean

### 6.5 Phase 6 测试收尾

- [x] 6.5.1 运行 client-log 模块全部测试：`mvn test -pl clients/client-log`

## 7. Phase 2: 国际化 i18n

### 7.1 ErrorCode 扩展

- [x] 7.1.1 RED: 编写 ErrorCode 扩展测试（ErrorCodeI18nUTest），覆盖：CommonErrorCode.FAIL.messageKey() 返回 "error.2000"、message() 保持返回中文（向后兼容）、所有 CommonErrorCode 枚举值都有 messageKey
- [x] 7.1.2 GREEN: 在 ErrorCode 接口新增 `default String messageKey() { return "error." + code(); }` 默认方法

### 7.2 国际化资源文件

- [x] 7.2.1 创建 `messages.properties`（默认中文），包含所有 CommonErrorCode 翻译（key 格式 error.{code}）
- [x] 7.2.2 创建 `messages_en.properties`（英文），包含所有 CommonErrorCode 英文翻译
- [x] 7.2.3 创建 `ValidationMessages.properties`（Bean Validation 中文消息）
- [x] 7.2.4 创建 `ValidationMessages_en.properties`（Bean Validation 英文消息）

### 7.3 WebExceptionAdvise 国际化改造

- [x] 7.3.1 RED: 编写 WebExceptionAdvise 国际化测试（WebExceptionAdviseI18nITest），覆盖：Accept-Language: en 时返回英文错误消息、不携带 Accept-Language 时返回默认中文、找不到翻译时回退到默认消息、Bean Validation 校验失败时返回对应语言消息
- [x] 7.3.2 GREEN: 改造 WebExceptionAdvise，注入 MessageSource + LocaleResolver，从 Accept-Language Header 解析 Locale，使用 errorCode.messageKey() 查询国际化消息

### 7.4 Phase 7 测试收尾

- [x] 7.4.1 运行 app 模块全部测试：`mvn test -pl app`

## 8. Phase 3: 分页查询示例

### 8.1 操作日志分页查询

- [x] 8.1.1 创建 OperationLogPageQuery record（extends BasePageRequest，+ module, operationType, startTime, endTime）
- [x] 8.1.2 RED: 编写 OperationLogRepository 分页测试（OperationLogRepositoryITest），覆盖：无条件分页查询返回正确分页数据、按 module 筛选、按 operationType 筛选、按时间范围筛选、分页参数正确传递
- [x] 8.1.3 GREEN: 创建 OperationLogService（分页查询方法，返回 IPage<OperationLog>）、OperationLogRepository 接口 + 实现类
- [x] 8.1.4 RED: 编写 OperationLogFacade 分页测试（OperationLogFacadeUTest），覆盖：分页查询返回 BasePageResult<OperationLogVO>、筛选条件正确传递
- [x] 8.1.5 GREEN: 创建 OperationLogFacade 接口 + OperationLogFacadeImpl（调用 Service + Entity→VO 转换）、OperationLogVO record
- [x] 8.1.6 RED: 编写 OperationLogController 集成测试（OperationLogControllerITest），覆盖：GET /api/system/operation-logs 分页查询、按 module 和 operationType 筛选、按时间范围筛选、pageNo 和 pageSize 默认值、pageSize 超过最大值校验失败
- [x] 8.1.7 GREEN: 创建 OperationLogController（GET /api/system/operation-logs）

### 8.2 系统配置分页查询

- [x] 8.2.1 创建 SystemConfigPageQuery record（extends BasePageRequest，+ groupCode）
- [x] 8.2.2 RED: 编写 SystemConfigRepository 分页测试，覆盖：无条件分页、按 groupCode 筛选
- [x] 8.2.3 GREEN: 在 SystemConfigRepository 接口和实现类中新增分页查询方法
- [x] 8.2.4 RED: 编写 SystemConfigFacade 分页测试
- [x] 8.2.5 GREEN: 在 SystemConfigFacade 接口和实现类中新增分页查询方法
- [x] 8.2.6 RED: 编写 SystemConfigController 分页端点测试（覆盖 GET /api/system/configs/page）
- [x] 8.2.7 GREEN: 在 SystemConfigController 新增 `GET /api/system/configs/page` 端点

### 8.3 Phase 8 测试收尾

- [x] 8.3.1 运行全部测试确认通过：`mvn test`

## 9. Phase 3: 文档全面对齐

### 9.1 AGENTS.md 更新

- [x] 9.1.1 更新 AGENTS.md：技术栈表格新增 Sa-Token / Bucket4j、多模块结构新增 client-auth/client-ratelimit/client-idempotent、项目结构索引新增 facade 包描述、核心编码规则新增 Facade 层规范、新增 @RateLimit / @Idempotent / @BusinessLog 扩展说明

### 9.2 ARCHETYPE_README.md 更新

- [x] 9.2.1 更新 ARCHETYPE_README.md：反映四层架构、新增模块说明、新增 API 端点说明、新增认证配置说明

### 9.3 docs/ 目录文档更新

- [x] 9.3.1 更新 docs/architecture/overview.md：四层架构描述、新增模块架构图
- [x] 9.3.2 更新 docs/guides/getting-started.md：新增认证配置、API 前缀说明
- [x] 9.3.3 更新 docs/backend/coding-standards.md：Facade 层编码规范、@RateLimit / @Idempotent 使用规范
- [x] 9.3.4 更新 docs/backend/testing-guide.md：新增 client 模块测试规范

### 9.4 文档一致性验证

- [x] 9.4.1 验证 AGENTS.md 模块索引与实际模块目录完全一致
- [x] 9.4.2 验证文档中所有代码示例与实际代码一致
- [x] 9.4.3 验证技术栈表格与 pom.xml 依赖版本一致

## 10. 最终验证

- [x] 10.1 运行全部测试：`mvn clean test` → 193 tests, 0 failures
- [x] 10.2 运行覆盖率检查：`mvn clean verify` → Instruction 83%, Branch 58%（软目标偏差已记录）
- [x] 10.3 启动应用并手动验证关键端点：`mvn spring-boot:run -pl app`
- [x] 10.4 进行 artifact 文档、讨论结果的一致性检查
