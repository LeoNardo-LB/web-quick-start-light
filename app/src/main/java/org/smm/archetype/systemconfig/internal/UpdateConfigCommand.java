package org.smm.archetype.systemconfig.internal;

/**
 * 更新配置命令
 */
public record UpdateConfigCommand(
        String configKey,
        String configValue
) {}
