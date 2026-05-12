package org.smm.archetype.auth.internal;

import jakarta.validation.constraints.NotBlank;

record LoginRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password
) {}
