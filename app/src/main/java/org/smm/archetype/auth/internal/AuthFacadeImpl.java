package org.smm.archetype.auth.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.auth.AuthFacade;
import org.smm.archetype.component.auth.AuthComponent;
import org.smm.archetype.exception.BizException;
import org.smm.archetype.exception.CommonErrorCode;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class AuthFacadeImpl implements AuthFacade {

    private final UserRepository userRepository;
    private final AuthComponent authComponent;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public String login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException(CommonErrorCode.AUTH_USER_NOT_FOUND, "用户不存在"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BizException(CommonErrorCode.AUTH_BAD_CREDENTIALS, "用户名或密码错误");
        }

        return authComponent.login(user.getId());
    }

    @Override
    public void logout() {
        authComponent.logout();
    }
}
