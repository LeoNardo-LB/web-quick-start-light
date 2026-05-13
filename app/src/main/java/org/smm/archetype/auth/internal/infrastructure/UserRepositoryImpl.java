package org.smm.archetype.auth.internal.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.smm.archetype.auth.internal.User;
import org.smm.archetype.auth.internal.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;
    private final UserConverter userConverter;

    @Override
    public Optional<User> findByUsername(String username) {
        UserDO userDO = userMapper.selectOne(
                new LambdaQueryWrapper<UserDO>().eq(UserDO::getUsername, username)
        );
        return Optional.ofNullable(userDO).map(userConverter::toModel);
    }
}
