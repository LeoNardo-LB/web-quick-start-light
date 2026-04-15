package org.smm.archetype.component.auth;

/**
 * 认证组件接口。
 * <p>
 * 提供登录、注销、会话查询等基础认证操作。
 */
public interface AuthComponent {

    /**
     * 登录
     *
     * @param userId 用户 ID（long 或 String）
     * @return token 值
     */
    String login(Object userId);

    /**
     * 注销当前会话
     */
    void logout();

    /**
     * 获取当前登录用户 ID
     *
     * @return 用户 ID 字符串，未登录返回 null
     */
    String getCurrentUserId();

    /**
     * 判断当前是否已登录
     *
     * @return 已登录返回 true
     */
    boolean isLogin();

    /**
     * 校验登录状态，未登录抛 BizException
     */
    void checkLogin();
}
