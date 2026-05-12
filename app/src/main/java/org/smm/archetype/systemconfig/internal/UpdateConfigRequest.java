package org.smm.archetype.systemconfig.internal;

import jakarta.validation.constraints.NotBlank;

/**
 * 更新配置请求
 */
public record UpdateConfigRequest(
        @NotBlank(message = "配置值不能为空")
        String configValue
) {}
