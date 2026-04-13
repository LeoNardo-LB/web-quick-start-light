## Why

当前 web-quick-start-light 项目作为三层架构骨架，基础能力缺失较多：无请求参数校验、异常体系单一无法区分业务/外部/系统错误、无线程上下文传递机制、无 traceId 全链路追踪、无多环境配置分离、无测试规范。同时，当前使用 Spring Data JPA 在复杂查询场景下控制力不足，缺少编译期安全的对象转换工具。参考同架构体系下的 DDD 后端项目（speccoding-fullstack/backend）的成熟设计，在不改变三层架构的前提下，将这些经过验证的基础设施能力系统性地引入。

## What Changes

- **BREAKING**: 替换 Spring Data JPA 为 MyBatis-Plus，获得更强的 SQL 控制力和复杂查询能力
- **BREAKING**: 引入 MapStruct 替代基于反射的 MyBeanUtils，实现编译期安全的对象转换
- 引入三级异常体系（BizException / ClientException / SysException）+ ErrorCode 接口化
- 引入 ScopedThreadContext（基于 ScopedValue），实现请求级线程安全的上下文传递（userId、traceId）
- 引入 ContextFillFilter，为每个 HTTP 请求自动初始化 traceId 和上下文
- 引入 `@Valid` + Bean Validation 请求参数校验，全局异常处理器覆盖校验异常
- 引入 MyBatis-Plus MyMetaObjectHandler 自动填充审计字段（createTime / updateTime / createUser / updateUser）
- 引入多环境配置体系（application-dev.yaml / application-prod.yaml / application-optional.yaml）+ @ConfigurationProperties 类型安全属性类
- 引入统一响应增加 `time` 字段
- 引入线程池配置（IO / CPU / Daemon / Scheduler 四类）
- 引入技术客户端接口（CacheClient / EmailClient / SmsClient / OssClient / SearchClient），提供端口抽象
- 引入测试基类体系（UnitTestBase / IntegrationTestBase）+ ArchUnit 架构合规测试
- 保留现有业务日志子系统（BizLog AOP + 策略模式）、KryoSerializer 等自有亮点设计

## Capabilities

### New Capabilities

- `exception-hierarchy`: 三级异常体系 + ErrorCode 接口化 + 全局异常处理器增强
- `thread-context`: ScopedThreadContext（ScopedValue）线程上下文传递 + ContextFillFilter + traceId 全链路
- `request-validation`: `@Valid` Bean Validation 请求参数校验 + 校验异常全局处理
- `mybatis-plus-integration`: MyBatis-Plus 替换 JPA + MyMetaObjectHandler 审计字段自动填充 + MapStruct 对象转换
- `config-management`: 多环境配置 + @ConfigurationProperties 类型安全属性 + 线程池配置
- `tech-client-interface`: CacheClient / EmailClient / SmsClient / OssClient / SearchClient 端口抽象接口
- `test-infrastructure`: 测试基类体系 + ArchUnit 架构合规测试 + UT/IT 分层

### Modified Capabilities

- `unified-response`: BaseResult 增加 `time` 字段，优化分页响应结构

## Impact

- **依赖变更**: 移除 spring-boot-starter-data-jpa，引入 mybatis-plus-spring-boot3-starter、mapstruct、spring-boot-starter-validation
- **数据访问层**: 所有 Repository 从 JPA Repository 改为 MyBatis-Plus Mapper
- **实体类**: BaseDO 从 JPA 注解迁移到 MyBatis-Plus 注解（@TableName, @TableId, @TableField）（注：BaseDO 后来在后续变更中被简化）
- **全局异常处理器**: WebExceptionAdvise 新增对 MethodArgumentNotValidException / ConstraintViolationException / BindException 的处理
- **配置文件**: 拆分为多环境 yaml，新增多个 @ConfigurationProperties 属性类（注：Properties 后来在 modularize-clients 变更中迁移到各 client 子模块，前缀统一为 middleware.*）
- **新增 Filter**: ContextFillFilter 注册到 FilterChain
- **新增测试**: test 目录下新增测试基类和架构合规测试
- **BizLog 子系统适配**: DbPersistenceHandler 从 JPA LogRepository 迁移到 MyBatis-Plus LogMapper（注：DbPersistenceHandler 后来在 Phase 2 中被删除）
- **旧异常类迁移**: 原 entity/api/BizException.java 删除，由 entity/api/exception/BizException.java 替代（注：异常体系后来在 modularize-clients 变更中迁移到 common 模块的 org.smm.archetype.exception 包下）；ResultCode 标记 @Deprecated 委托到 CommonErrorCode
