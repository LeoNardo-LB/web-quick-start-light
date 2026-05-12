package org.smm.archetype.auth.internal;

import lombok.RequiredArgsConstructor;
import org.smm.archetype.auth.AuthFacade;
import org.smm.archetype.shared.result.BaseResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class LoginController {

    private final AuthFacade authFacade;

    @PostMapping("/login")
    public BaseResult<Map<String, String>> login(@RequestBody LoginRequest request) {
        String token = authFacade.login(request.username(), request.password());
        return BaseResult.success(Map.of("token", token));
    }

    @PostMapping("/logout")
    public BaseResult<Void> logout() {
        authFacade.logout();
        return BaseResult.success(null);
    }
}
