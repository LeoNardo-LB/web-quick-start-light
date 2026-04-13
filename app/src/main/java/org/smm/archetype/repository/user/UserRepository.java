package org.smm.archetype.repository.user;

import org.smm.archetype.entity.user.User;

import java.util.Optional;

/**
 * 用户仓储接口。
 */
public interface UserRepository {

    /**
     * 根据用户名查找用户
     *
     * @param username 用户名
     * @return 用户实体，不存在返回 Optional.empty()
     */
    Optional<User> findByUsername(String username);
}
