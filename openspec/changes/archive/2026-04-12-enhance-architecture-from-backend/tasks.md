## 1. 依赖变更与 POM 配置

- [x] 1.1 修改 pom.xml：移除 spring-boot-starter-data-jpa 依赖
- [x] 1.2 修改 pom.xml：添加 mybatis-plus-spring-boot3-starter 依赖（3.5.x）
- [x] 1.3 修改 pom.xml：添加 mapstruct + mapstruct-processor 依赖（1.5.x）
- [x] 1.4 修改 pom.xml：添加 spring-boot-starter-validation 依赖
- [x] 1.5 修改 pom.xml：添加 archunit-junit5 依赖（test scope）
- [x] 1.6 修改 pom.xml：配置 maven-compiler-plugin 的 annotationProcessorPaths（lombok + mapstruct-processor + configuration-processor）
- [x] 1.7 修改 pom.xml：移除 spring-boot-starter-data-jpa 相关的排除项

## 2. 异常体系重构

> 注意：以下异常类后来在 modularize-clients 变更中从 `entity/api/exception/` 迁移到 `common` 模块的 `org.smm.archetype.exception` 包下。

- [x] 2.1 创建 entity/api/exception/ErrorCode.java 接口（code() + message()）
- [x] 2.2 创建 entity/api/exception/BaseException.java 抽象类（继承 RuntimeException，持有 ErrorCode）
- [x] 2.3 创建 entity/api/exception/BizException.java（继承 BaseException）
- [x] 2.4 创建 entity/api/exception/ClientException.java（继承 BaseException）
- [x] 2.5 创建 entity/api/exception/SysException.java（继承 BaseException）
- [x] 2.6 创建 entity/api/exception/CommonErrorCode.java 枚举（SUCCESS/FAIL/ILLEGAL_ARGUMENT/RPC_EXCEPTION/SYS_ERROR/UNKNOWN_ERROR，合并原 ResultCode 所有值）
- [x] 2.7 删除 entity/api/BizException.java（旧异常类，被 entity/api/exception/BizException.java 替代）
- [x] 2.8 标记 entity/enums/ResultCode.java 为 @Deprecated，委托到 CommonErrorCode（保持向后兼容，引导迁移到 CommonErrorCode）
- [x] 2.9 增强 controller/global/WebExceptionAdvise.java：添加 MethodArgumentNotValidException / ConstraintViolationException / BindException / ClientException / SysException 处理，所有异常统一返回 HTTP 200 + BaseResult 封装

## 3. ScopedThreadContext 线程上下文

- [x] 3.1 创建 util/context/ScopedThreadContext.java（基于 ScopedValue，支持 userId / traceId）
- [x] 3.2 创建 util/context/ContextRunnable.java（跨线程上下文传递包装器）
- [x] 3.3 创建 util/context/ContextCallable.java（跨线程上下文传递包装器）
- [x] 3.4 删除 util/UserContext.java（被 ScopedThreadContext 替代）

## 4. ContextFillFilter 与 traceId

- [x] 4.1 创建 controller/global/ContextFillFilter.java（继承 OncePerRequestFilter，设置 traceId + userId）
- [x] 4.2 创建 config/WebConfigure.java（注册 ContextFillFilter + CORS 配置）

## 5. 统一响应增强

- [x] 5.1 修改 entity/api/BaseResult.java：添加 time 字段，success/fail 方法自动填充 traceId 和 time
- [x] 5.2 修改 entity/api/BasePageResult.java：适配 MyBatis-Plus 的 IPage 替代 JPA 的 Page
- [x] 5.3 修改 entity/api/BaseRequest.java：添加 JSR 380 校验注解
- [x] 5.4 修改 entity/api/BasePageRequest.java：pageNo 默认 1、pageSize 默认 20、pageSize @Max(100)

## 6. MyBatis-Plus 集成（BREAKING）

> 注意：BaseDO 和 LogDO 后来在后续变更中被简化/移除。

- [x] 6.1 修改 entity/dataobj/BaseDO.java：移除 JPA 注解，改用 MyBatis-Plus 注解（@TableName 基类无，@TableId(type=ASSIGN_ID)，@TableField(fill)）
- [x] 6.2 创建 util/dal/MyMetaObjectHandler.java（实现 MetaObjectHandler，自动填充审计字段，从 ScopedThreadContext 获取 userId）
- [x] 6.3 修改 entity/dataobj/LogDO.java：移除 JPA 注解（@Entity/@Column 等），改用 MyBatis-Plus 注解（@TableName/@TableField）
- [x] 6.4 创建 repository/mapper/LogMapper.java（继承 BaseMapper<LogDO>，替代原 LogRepository）
- [x] 6.5 修改 util/log/handler/persistence/DbPersistenceHandler.java：将 LogRepository（JPA）依赖替换为 LogMapper（MyBatis-Plus），调整 save 方法调用
- [x] 6.6 删除 repository/LogRepository.java（JPA Repository 被 LogMapper 替代，需在 DbPersistenceHandler 适配后删除）
- [x] 6.7 修改 WebStartLightApplication.java：添加 @MapperScan 注解，移除 @EnableJpaAuditing

## 7. MapStruct 对象转换

- [x] 7.1 删除 util/MyBeanUtils.java（被 MapStruct 替代）
- [x] 7.2 创建 repository/converter/LogConverter.java（@Mapper(componentModel="spring")，LogDO 与业务对象互转）

## 8. 配置管理体系

- [x] 8.1 拆分 application.yaml：提取公共配置到主文件
- [x] 8.2 创建 application-dev.yaml（开发环境：SQLite、debug 日志等）
- [x] 8.3 创建 application-prod.yaml（生产环境：SQLite、info 日志等）
- [x] 8.4 创建 application-optional.yaml（可选中间件：Redis、ES 等占位配置）
- [x] 8.5 创建 config/properties/ThreadPoolProperties.java（@ConfigurationProperties 线程池配置）
- [x] 8.6 创建 config/properties/CacheProperties.java（@ConfigurationProperties 缓存配置）
- [x] 8.7 创建 config/properties/OssProperties.java（@ConfigurationProperties 存储配置）
- [x] 8.8 创建 config/properties/EmailProperties.java（@ConfigurationProperties 邮件配置）
- [x] 8.9 创建 config/properties/SmsProperties.java（@ConfigurationProperties 短信配置）
- [x] 8.10 创建 config/ThreadPoolConfigure.java（四种线程池 Bean：io / cpu / daemon / scheduler）
- [x] 8.11 更新 logback-spring.xml：适配多环境 profile

## 9. 技术客户端接口

- [x] 9.1 创建 service/client/CacheClient.java 接口（put / get / remove / evict）
- [x] 9.2 创建 service/client/EmailClient.java 接口（sendTemplateEmail / sendSimpleEmail）
- [x] 9.3 创建 service/client/SmsClient.java 接口（sendTemplateSms / sendVerifyCode）
- [x] 9.4 创建 service/client/OssClient.java 接口（upload / download / getSignedUrl / delete）
- [x] 9.5 创建 service/client/SearchClient.java 接口（search / index / deleteIndex）
- [x] 9.6 创建 service/client/dto/ 包下所有 Request/Result record DTO
- [x] 9.7 创建 service/client/impl/LoggingCacheClient.java（默认日志空实现）
- [x] 9.8 创建 service/client/impl/LoggingEmailClient.java（默认日志空实现）
- [x] 9.9 创建 service/client/impl/LoggingSmsClient.java（默认日志空实现）
- [x] 9.10 创建 service/client/impl/LoggingOssClient.java（默认日志空实现）
- [x] 9.11 创建 service/client/impl/LoggingSearchClient.java（默认日志空实现）

## 10. Service 层适配

- [x] 10.1 删除 service/CacheService.java 接口（被 CacheClient 替代）
- [x] 10.2 修改 controller/test/TestController.java：不再直接注入 Repository，改为注入 Service；使用 @Valid 校验；返回 BaseResult

## 11. 测试基础设施

- [x] 11.1 创建 test/.../support/UnitTestBase.java（@ExtendWith(MockitoExtension.class)）
- [x] 11.2 创建 test/.../support/IntegrationTestBase.java（@SpringBootTest + WebTestClient）
- [x] 11.3 创建 test/.../support/basic/ApplicationStartupITest.java（启动验证集成测试，遵循 *ITest 命名规范）
- [x] 11.4 创建 test/.../support/basic/ArchitectureComplianceUTest.java（ArchUnit 三层架构规则，遵循 *UTest 命名规范）
- [x] 11.5 创建 src/test/resources/application.yaml（测试配置：SQLite）
- [x] 11.6 创建 src/test/resources/logback-test.xml（测试日志配置）

## 12. 验证与清理

- [x] 12.1 确认所有现有功能正常：启动验证、BizLog 切面（DbPersistenceHandler 使用 LogMapper）、Swagger UI
- [x] 12.2 确认 KryoSerializer 在 MyBatis-Plus 环境下正常工作（无 JPA 依赖冲突）
- [x] 12.3 确认所有测试通过（单元测试 *UTest + 集成测试 *ITest）
- [x] 12.4 确认无无用 import 和遗留代码（无 JPA 相关 import、无旧 BizException 引用）
- [x] 12.5 更新 README.md（新增依赖说明、多环境配置说明）

## 13. Phase 2 精简：日志系统简化与过度设计移除

- [x] 13.1 删除 BizLogDto.java（过度设计的日志 DTO）
- [x] 13.2 删除 util/log/handler/persistence/PersistenceHandler.java（策略接口）
- [x] 13.3 删除 util/log/handler/persistence/PersistenceType.java（策略枚举）
- [x] 13.4 删除 util/log/handler/persistence/DbPersistenceHandler.java（数据库持久化处理器）
- [x] 13.5 删除 util/log/handler/persistence/FilePersistenceHandler.java（文件持久化处理器）
- [x] 13.6 删除 util/log/handler/stringify/StringifyHandler.java（序列化策略接口）
- [x] 13.7 删除 util/log/handler/stringify/StringifyType.java（序列化策略枚举）
- [x] 13.8 删除 util/log/handler/stringify/JdkStringifyHandler.java（JDK 序列化实现）
- [x] 13.9 删除 entity/dataobj/LogDO.java（日志数据对象，不再需要数据库日志）
- [x] 13.10 删除 repository/mapper/LogMapper.java（日志 Mapper，不再需要数据库日志）
- [x] 13.11 删除 repository/converter/LogConverter.java（日志转换器）
- [x] 13.12 重写 @BizLog 注解：精简为只有 value() 属性
- [x] 13.13 重写 BizLogAspect：改为 SLF4J 日志 + Micrometer Timer/Counter 指标，去掉异步 ExecutorService 和策略模式
- [x] 13.14 重写 TestController：去掉 LogMapper 依赖和 /logs 端点
- [x] 13.15 移除 @MapperScan 注解（不再需要扫描空 mapper 包）
- [x] 13.16 恢复 ArchUnit ArchitectureComplianceUTest 原版规则（allowEmptyShould=true）
- [x] 13.17 更新 ApplicationStartupITest：验证 bizLogAspect Bean 存在
- [x] 13.18 删除 BizLogPersistenceITest（已无持久化逻辑可测）
