package org.smm.archetype.shared.aspect.operationlog;

/**
 * 操作日志写入器接口。
 * <p>
 * 由 app 模块实现（如 MyBatisOperationLogWriter），
 * LogAspect 通过此接口将操作日志异步写入数据库。
 */
public interface OperationLogWriter {

    void write(OperationLogRecord record);

}
