package org.smm.archetype.service.auth;

/**
 * 登录门面接口。
 */
public interface LoginFacade {

    /**
     * 登录
     *
     * @param username 用户名
     * @param password 密码
     * @return token
     */
    String login(String username, String password);

    /**
     * 注销
     */
    void logout();
}
