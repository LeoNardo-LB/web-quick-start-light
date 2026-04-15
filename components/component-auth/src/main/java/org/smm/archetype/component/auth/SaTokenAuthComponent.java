package org.smm.archetype.component.auth;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 基于 Sa-Token 的认证组件实现。
 * <p>
 * 依赖 Sa-Token 框架，使用默认的内存会话存储。
 */
public class SaTokenAuthComponent extends AbstractAuthComponent {

    @Override
    protected String doLogin(Object userId) {
        StpUtil.login(userId);
        return StpUtil.getTokenValue();
    }

    @Override
    protected void doLogout() {
        StpUtil.logout();
    }

    @Override
    protected String doGetCurrentUserId() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        return loginId != null ? loginId.toString() : null;
    }

    @Override
    protected boolean doIsLogin() {
        return StpUtil.isLogin();
    }
}
