package org.smm.archetype.client.auth;

import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.exception.BizException;
import org.smm.archetype.exception.CommonErrorCode;

/**
 * AuthClient 抽象基类，使用 Template Method 模式。
 * <p>
 * 所有公开方法标记为 final，完成参数校验与异常处理。
 * 子类实现 do* 扩展点完成具体认证操作。
 */
@Slf4j
public abstract class AbstractAuthClient implements AuthClient {

    @Override
    public final String login(Object userId) {
        if (userId == null) {
            throw new BizException(CommonErrorCode.FAIL, "userId不能为空");
        }
        log.debug("Auth login: userId={}", userId);
        return doLogin(userId);
    }

    @Override
    public final void logout() {
        log.debug("Auth logout");
        doLogout();
    }

    @Override
    public final String getCurrentUserId() {
        return doGetCurrentUserId();
    }

    @Override
    public final boolean isLogin() {
        return doIsLogin();
    }

    @Override
    public final void checkLogin() {
        if (!doIsLogin()) {
            throw new BizException(CommonErrorCode.AUTH_UNAUTHORIZED, "请先登录");
        }
    }

    // ==================== 子类扩展点 ====================

    protected abstract String doLogin(Object userId);

    protected abstract void doLogout();

    protected abstract String doGetCurrentUserId();

    protected abstract boolean doIsLogin();
}
