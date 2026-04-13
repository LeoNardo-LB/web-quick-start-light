package org.smm.archetype.repository.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.smm.archetype.entity.user.User;
import org.smm.archetype.generated.entity.UserDO;
import org.smm.archetype.generated.mapper.UserMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 用户仓储实现。
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;

    @Override
    public Optional<User> findByUsername(String username) {
        UserDO userDO = userMapper.selectOne(
                new LambdaQueryWrapper<UserDO>().eq(UserDO::getUsername, username)
        );
        return Optional.ofNullable(userDO).map(this::toEntity);
    }

    private static final DateTimeFormatter SQLITE_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private User toEntity(UserDO userDO) {
        User user = new User();
        user.setId(userDO.getId());
        user.setUsername(userDO.getUsername());
        user.setPasswordHash(userDO.getPasswordHash());
        user.setNickname(userDO.getNickname());
        user.setStatus(userDO.getStatus());
        user.setCreateTime(parseDateTime(userDO.getCreateTime()));
        user.setUpdateTime(parseDateTime(userDO.getUpdateTime()));
        return user;
    }

    /**
     * 解析 SQLite datetime('now') 格式（yyyy-MM-dd HH:mm:ss）为 Instant。
     * 同时兼容 ISO-8601 格式（yyyy-MM-ddTHH:mm:ssZ）。
     */
    private Instant parseDateTime(String dateTime) {
        if (dateTime == null || dateTime.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTime, SQLITE_DATETIME_FORMATTER)
                    .toInstant(ZoneOffset.UTC);
        } catch (Exception e) {
            return Instant.parse(dateTime);
        }
    }
}
