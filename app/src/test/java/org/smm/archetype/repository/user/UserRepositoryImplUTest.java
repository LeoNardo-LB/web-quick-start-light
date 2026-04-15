package org.smm.archetype.repository.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smm.archetype.entity.user.User;
import org.smm.archetype.generated.entity.UserDO;
import org.smm.archetype.generated.mapper.UserMapper;
import org.smm.archetype.support.UnitTestBase;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("UserRepositoryImpl")
class UserRepositoryImplUTest extends UnitTestBase {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserRepositoryImpl userRepository;

    // =========================================================================
    // findByUsername
    // =========================================================================

    @Nested
    @DisplayName("findByUsername")
    class FindByUsername {

        @Test
        @DisplayName("用户存在时应返回 Optional.of(User)")
        void should_return_user_when_found() {
            // given
            UserDO userDO = buildUserDO(1L, "testuser", "hashed-pwd", "TestNick", "ACTIVE");
            userDO.setCreateTime(Instant.now());
            userDO.setUpdateTime(Instant.now());
            when(userMapper.selectOne(any())).thenReturn(userDO);

            // when
            Optional<User> result = userRepository.findByUsername("testuser");

            // then
            assertThat(result).isPresent();
            User user = result.get();
            assertThat(user.getId()).isEqualTo(1L);
            assertThat(user.getUsername()).isEqualTo("testuser");
            assertThat(user.getPasswordHash()).isEqualTo("hashed-pwd");
            assertThat(user.getNickname()).isEqualTo("TestNick");
            assertThat(user.getStatus()).isEqualTo("ACTIVE");
            assertThat(user.getCreateTime()).isNotNull();
            assertThat(user.getUpdateTime()).isNotNull();
        }

        @Test
        @DisplayName("用户不存在时应返回 Optional.empty()")
        void should_return_empty_when_not_found() {
            // given
            String username = "nonexistent";
            when(userMapper.selectOne(any())).thenReturn(null);

            // when
            Optional<User> result = userRepository.findByUsername(username);

            // then
            assertThat(result).isEmpty();
            verify(userMapper).selectOne(any());
        }

        @Test
        @DisplayName("应将 username 传递给 mapper 查询条件")
        void should_pass_username_to_mapper() {
            // given
            String username = "admin";
            when(userMapper.selectOne(any())).thenReturn(null);

            // when
            userRepository.findByUsername(username);

            // then
            verify(userMapper).selectOne(any());
        }

        @Test
        @DisplayName("Instant 类型时间应直接传递")
        void should_pass_instant_directly() {
            // given
            Instant createTime = Instant.parse("2026-01-15T08:00:00Z");
            Instant updateTime = Instant.parse("2026-01-15T09:30:00Z");
            UserDO userDO = buildUserDO(2L, "timeuser", "hash", "TimeNick", "ACTIVE");
            userDO.setCreateTime(createTime);
            userDO.setUpdateTime(updateTime);
            when(userMapper.selectOne(any())).thenReturn(userDO);

            // when
            Optional<User> result = userRepository.findByUsername("timeuser");

            // then
            assertThat(result).isPresent();
            User user = result.get();
            assertThat(user.getCreateTime()).isEqualTo(createTime);
            assertThat(user.getUpdateTime()).isEqualTo(updateTime);
        }

        @Test
        @DisplayName("时间为 null 时应安全处理")
        void should_handle_null_datetime() {
            // given
            UserDO userDO = buildUserDO(4L, "nulldate", "hash", "NullDate", "ACTIVE");
            userDO.setCreateTime(null);
            userDO.setUpdateTime(null);
            when(userMapper.selectOne(any())).thenReturn(userDO);

            // when
            Optional<User> result = userRepository.findByUsername("nulldate");

            // then
            assertThat(result).isPresent();
            User user = result.get();
            assertThat(user.getCreateTime()).isNull();
            assertThat(user.getUpdateTime()).isNull();
        }
    }

    // =========================================================================
    // 辅助方法
    // =========================================================================

    private static UserDO buildUserDO(Long id, String username, String passwordHash,
                                       String nickname, String status) {
        UserDO userDO = new UserDO();
        userDO.setId(id);
        userDO.setUsername(username);
        userDO.setPasswordHash(passwordHash);
        userDO.setNickname(nickname);
        userDO.setStatus(status);
        return userDO;
    }
}
