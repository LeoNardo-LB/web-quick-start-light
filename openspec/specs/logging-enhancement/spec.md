## MODIFIED Requirements

### Requirement: BusinessLog 注解属性扩展
BusinessLog 注解 SHALL 新增 module（String，模块名）、operation（OperationType 枚举，操作类型）、samplingRate（double，采集率 0.0-1.0，默认 1.0）属性。原 value 属性保持不变。

#### Scenario: 使用扩展属性
- **WHEN** 方法标注 `@BusinessLog(value="更新配置", module="SYSTEM", operation=OperationType.UPDATE, samplingRate=0.5)`
- **THEN** LogAspect SHALL 正确提取 module、operation、samplingRate 属性

#### Scenario: 旧代码向后兼容
- **WHEN** 方法仅使用 `@BusinessLog("描述")` 格式
- **THEN** LogAspect SHALL 正常工作，新属性使用默认值

### Requirement: LogAspect 异步 DB 写入
LogAspect SHALL 在方法执行后异步将操作日志写入 operation_log 数据库表。异步写入 SHALL 使用 ioThreadPool 线程池，通过 ContextRunnable 传递 ScopedValue 上下文。

#### Scenario: 日志异步写入数据库
- **WHEN** 标注了 @BusinessLog 的方法执行完成
- **THEN** 系统 SHALL 通过 ioThreadPool 异步写入 operation_log 表

#### Scenario: 采集率控制 DB 写入
- **WHEN** @BusinessLog 配置了 samplingRate = 0.5
- **THEN** 系统 SHALL 以约 50% 的概率写入 DB，Micrometer 指标仍全量记录
