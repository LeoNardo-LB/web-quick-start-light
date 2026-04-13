package org.smm.archetype.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.client.auth.AuthClient;
import org.smm.archetype.entity.user.User;
import org.smm.archetype.exception.BizException;
import org.smm.archetype.exception.CommonErrorCode;
import org.smm.archetype.repository.user.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 登录门面实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginFacadeImpl implements LoginFacade {

    private final UserRepository userRepository;
    private final AuthClient authClient;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public String login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException(CommonErrorCode.AUTH_USER_NOT_FOUND, "用户不存在"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BizException(CommonErrorCode.AUTH_BAD_CREDENTIALS, "用户名或密码错误");
        }

        log.info("用户登录成功: username={}", username);
        return authClient.login(user.getId());
    }

    @Override
    public void logout() {
        log.info("用户注销");
        authClient.logout();
    }
}
