package org.smm.archetype.controller.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.smm.archetype.entity.api.BaseResult;
import org.smm.archetype.service.auth.LoginFacade;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 登录请求。
 */
record LoginRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password
) {}

/**
 * 登录控制器。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LoginController {

    private final LoginFacade loginFacade;

    @PostMapping("/login")
    public BaseResult<Map<String, String>> login(@RequestBody LoginRequest request) {
        String token = loginFacade.login(request.username(), request.password());
        return BaseResult.success(Map.of("token", token));
    }

    @PostMapping("/logout")
    public BaseResult<Void> logout() {
        loginFacade.logout();
        return BaseResult.success(null);
    }
}
