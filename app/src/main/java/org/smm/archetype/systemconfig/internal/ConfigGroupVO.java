package org.smm.archetype.systemconfig.internal;

/**
 * 配置分组 VO
 */
public record ConfigGroupVO(
        String code,
        String displayName,
        String icon,
        String color
) {}
