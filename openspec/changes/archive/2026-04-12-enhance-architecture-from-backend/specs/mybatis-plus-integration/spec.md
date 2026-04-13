## ADDED Requirements

### Requirement: MyBatis-Plus 集成
系统 SHALL 使用 MyBatis-Plus 替代 Spring Data JPA 作为 ORM 框架。

- 引入 mybatis-plus-spring-boot3-starter 依赖
- 移除 spring-boot-starter-data-jpa 依赖
- 使用 @MapperScan 扫描 Mapper 接口
- 实体类使用 MyBatis-Plus 注解（@TableName、@TableId、@TableField）
- Mapper 接口继承 BaseMapper<Entity>

#### Scenario: 基本 CRUD 操作
- **WHEN** 定义一个继承 BaseMapper 的 Mapper 接口
- **THEN** 无需编写 XML 即可使用 insert / selectById / updateById / deleteById 等方法

#### Scenario: Lambda 查询
- **WHEN** 需要按条件查询
- **THEN** 使用 LambdaQueryWrapper 构建类型安全的查询条件

### Requirement: MyMetaObjectHandler 审计字段自动填充
系统 SHALL 提供 MyMetaObjectHandler 实现 MyBatis-Plus MetaObjectHandler 接口，自动填充以下审计字段：
- createTime：插入时自动填充当前时间
- updateTime：插入和更新时自动填充当前时间
- createUser：插入时从 ScopedThreadContext 获取 userId
- updateUser：更新时从 ScopedThreadContext 获取 userId

#### Scenario: 新增记录自动填充
- **WHEN** 通过 Mapper 插入一条新记录
- **THEN** createTime 和 updateTime 自动设置为当前时间，createUser 和 updateUser 自动设置为当前 userId

#### Scenario: 更新记录自动填充
- **WHEN** 通过 Mapper 更新一条记录
- **THEN** updateTime 自动更新为当前时间，updateUser 自动更新为当前 userId

### Requirement: MapStruct 对象转换
系统 SHALL 引入 MapStruct 作为对象转换工具。

- 定义 @Mapper(componentModel = "spring") 接口
- 在 Controller 层使用 Converter 将 Request 转为 Service 入参
- 在 Repository 层使用 Converter 将 Entity 转为 DO（数据对象）
- 移除 MyBeanUtils（运行时反射工具）

#### Scenario: Entity 到 DO 的转换
- **WHEN** Service 层返回一个 Entity 对象
- **THEN** 通过 MapStruct Converter 编译期生成的代码将其转换为 DO 对象

#### Scenario: 编译期类型安全检查
- **WHEN** Converter 接口中定义了字段类型不匹配的映射
- **THEN** 编译期报错，而非运行时才发现

### Requirement: BaseDO 适配 MyBatis-Plus
BaseDO SHALL 使用 MyBatis-Plus 注解替代 JPA 注解：
- 使用 @TableId(type = IdType.ASSIGN_ID) 雪花算法生成主键
- 使用 @TableField(fill = FieldFill.INSERT) / FieldFill.INSERT_UPDATE 标记审计字段
- 移除 JPA 的 @MappedSuperclass、@PrePersist、@PreUpdate 等注解

#### Scenario: 主键自动生成
- **WHEN** 插入新记录时未设置 id
- **THEN** MyBatis-Plus 自动通过雪花算法生成 Long 类型的主键
