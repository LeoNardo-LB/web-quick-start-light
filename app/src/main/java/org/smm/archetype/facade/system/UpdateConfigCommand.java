package org.smm.archetype.facade.system;

/**
 * 更新配置命令
 */
public record UpdateConfigCommand(
        String configKey,
        String configValue
) {}
