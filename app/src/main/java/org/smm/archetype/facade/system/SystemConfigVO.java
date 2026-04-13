package org.smm.archetype.facade.system;

/**
 * 系统配置 VO
 */
public record SystemConfigVO(
        Long id,
        String configKey,
        String configValue,
        String valueType,
        String groupCode,
        String displayName,
        String description,
        String inputType,
        String inputConfig,
        Integer sort
) {}
