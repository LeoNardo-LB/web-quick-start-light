## ADDED Requirements

### Requirement: 多环境配置文件
系统 SHALL 支持多环境配置文件：
- application.yaml：主配置（公共配置）
- application-dev.yaml：开发环境配置
- application-prod.yaml：生产环境配置
- application-optional.yaml：可选中间件配置（Redis、Kafka、ES 等）

通过 `spring.profiles.active` 切换环境。

#### Scenario: 开发环境启动
- **WHEN** 使用 `-Dspring.profiles.active=dev` 启动应用
- **THEN** 加载 application.yaml + application-dev.yaml，使用 SQLite

#### Scenario: 生产环境启动
- **WHEN** 使用 `-Dspring.profiles.active=prod` 启动应用
- **THEN** 加载 application.yaml + application-prod.yaml，使用 SQLite

### Requirement: ConfigurationProperties 类型安全属性
系统 SHALL 使用 @ConfigurationProperties 定义类型安全的配置属性类，集中在 config/properties/ 包下：
- CacheProperties：缓存配置（类型、过期时间等）
- ThreadPoolProperties：线程池配置（核心线程数、最大线程数等）
- OssProperties：对象存储配置（类型、endpoint、bucket 等）
- EmailProperties：邮件配置（host、port、username 等）
- SmsProperties：短信配置（provider、templateId 等）

#### Scenario: 使用类型安全配置
- **WHEN** 需要读取缓存过期时间配置
- **THEN** 注入 CacheProperties Bean，通过 cacheProperties.getExpireSeconds() 获取，IDE 提供自动补全

### Requirement: 线程池配置
系统 SHALL 在 ThreadPoolConfigure 中配置四种线程池 Bean：
- ioThreadPool：IO 密集型任务线程池（核心线程数 = CPU 核数 × 2）
- cpuThreadPool：CPU 密集型任务线程池（核心线程数 = CPU 核数 + 1）
- daemonThreadPool：守护线程池（后台低优先级任务）
- schedulerThreadPool：定时任务线程池

每种线程池 SHALL 使用 ThreadPoolProperties 中的配置参数，SHALL 配置有意义的线程名前缀。

#### Scenario: IO 密集型任务使用指定线程池
- **WHEN** Service 层需要执行文件上传等 IO 密集型任务
- **THEN** 注入 ioThreadPool 并提交任务，线程名格式为 "io-pool-{n}"

#### Scenario: 线程池参数可配置
- **WHEN** 需要调整 IO 线程池大小
- **THEN** 在 application.yaml 中修改 thread-pool.io-core-size 配置值即可
