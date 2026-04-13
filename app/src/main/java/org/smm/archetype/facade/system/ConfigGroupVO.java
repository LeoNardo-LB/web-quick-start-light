package org.smm.archetype.facade.system;

/**
 * 配置分组 VO
 */
public record ConfigGroupVO(
        String code,
        String displayName,
        String icon,
        String color
) {}
