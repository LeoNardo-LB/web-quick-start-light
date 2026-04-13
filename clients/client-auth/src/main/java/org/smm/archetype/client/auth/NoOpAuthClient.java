package org.smm.archetype.client.auth;

/**
 * 空操作认证客户端。
 * <p>
 * 当 Sa-Token 不在 classpath 或认证功能被禁用时使用。
 * 所有认证操作为 no-op，isLogin 始终返回 true。
 */
public class NoOpAuthClient extends AbstractAuthClient {

    @Override
    protected String doLogin(Object userId) {
        return null;
    }

    @Override
    protected void doLogout() {
        // no-op
    }

    @Override
    protected String doGetCurrentUserId() {
        return null;
    }

    @Override
    protected boolean doIsLogin() {
        return true;
    }
}
