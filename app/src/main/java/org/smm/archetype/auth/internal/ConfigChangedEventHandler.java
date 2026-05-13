package org.smm.archetype.auth.internal;

import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.systemconfig.ConfigChangedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 配置变更事件处理器。
 * <p>
 * 监听 systemconfig 模块发布的 ConfigChangedEvent，同步刷新认证相关配置。
 */
@Slf4j
@Component
class ConfigChangedEventHandler {

    @EventListener
    void on(ConfigChangedEvent event) {
        log.info("Config changed: key={}, old={}, new={}",
                event.configKey(), event.oldValue(), event.newValue());
        // 后续可在此刷新认证相关配置缓存
    }
}
