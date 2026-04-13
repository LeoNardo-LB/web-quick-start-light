## ADDED Requirements

### Requirement: OssClient 接口定义
OssClient 接口 SHALL 提供 7 个对象存储操作方法：`upload`、`download`、`delete`、`generateUrl`、`searchFiles`、`exists`、`getFileSize`。所有方法声明在 `org.smm.archetype.client.oss.OssClient` 接口中。

#### Scenario: OssClient 接口方法完整性
- **WHEN** 编译 clients 模块
- **THEN** OssClient 接口包含 7 个方法签名

### Requirement: AbstractOssClient 模板方法
AbstractOssClient SHALL 实现 OssClient 接口，所有接口方法标记为 `final`，内部调用 `doUpload`/`doDownload`/`doDelete`/`doGenerateUrl`/`doSearchFiles`/`doExists`/`doGetFileSize` 抽象扩展点方法。SHALL 在模板方法中统一执行：fileKey 空值校验、入/出日志记录、异常包装为 ClientException。

#### Scenario: fileKey 为空时抛出异常
- **WHEN** 调用 AbstractOssClient 的 download/delete/generateUrl 等方法并传入 null 或空字符串 fileKey
- **THEN** 抛出 ClientException，错误码为 OSS_OPERATION_FAILED（对应 CLIENT_OSS_001）

### Requirement: LocalOssClient 本地文件系统实现
LocalOssClient SHALL 继承 AbstractOssClient，基于本地文件系统实现。SHALL 使用日期分层存储路径 `{localPath}/yyyy/MM/{timestamp}-{filename}`。upload 和 download SHALL 使用 NIO FileChannel.transferTo() 零拷贝。searchFiles SHALL 遍历文件系统配合 glob 模式匹配。文件元数据 SHALL 使用内存 Map 索引。

#### Scenario: 上传后可下载
- **WHEN** 调用 `upload(OssUploadRequest("test.txt", inputStream, 100, "text/plain"))` 后调用 `download(fileKey)`
- **THEN** 下载的 InputStream 内容与上传内容一致

#### Scenario: 日期分层存储
- **WHEN** 在 2026-04-12 上传文件 "test.txt"
- **THEN** 文件存储路径包含 "2026/04/" 目录层级

#### Scenario: 删除后文件不存在
- **WHEN** 上传文件后调用 `delete(fileKey)` 再调用 `exists(fileKey)`
- **THEN** 返回 false

### Requirement: OssAutoConfiguration 条件装配
OssAutoConfiguration SHALL 使用 `@AutoConfiguration` + `@ConditionalOnProperty(prefix="middleware.object-storage", name="type", havingValue="local", matchIfMissing=true)` + `@EnableConfigurationProperties(OssProperties.class)`。SHALL 在 `@ConditionalOnMissingBean` 条件下注册 LocalOssClient Bean。

#### Scenario: 配置 type=local 时注册 LocalOssClient
- **WHEN** `middleware.object-storage.type=local` 或未配置
- **THEN** Spring 容器中存在 LocalOssClient 类型的 Bean

### Requirement: OssProperties 配置属性
OssProperties SHALL 使用 `@ConfigurationProperties("middleware.object-storage")`，包含字段：type(String, 默认"local")、localPath(String, 默认"./uploads")。SHALL 不使用 @Data 注解。

#### Scenario: 默认配置值
- **WHEN** 不设置任何 middleware.object-storage 配置
- **THEN** OssProperties 的 type="local", localPath="./uploads"
