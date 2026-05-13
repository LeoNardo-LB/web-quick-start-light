package org.smm.archetype.operationlog.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.auth.UserLoggedInEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * 用户登录事件处理器。
 * <p>
 * 监听 auth 模块发布的 UserLoggedInEvent，异步记录登录日志。
 * 使用 @ApplicationModuleListener 实现异步 + 独立事务 + 自动重试。
 */
@Slf4j
@Component
@RequiredArgsConstructor
class UserLoggedInEventHandler {

    private final OperationLogRepository operationLogRepository;

    @ApplicationModuleListener
    void on(UserLoggedInEvent event) {
        log.info("Received login event: user={}, ip={}", event.username(), event.ip());
        // 后续可在此记录登录日志到 operation_log 表
    }
}
