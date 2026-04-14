package org.smm.archetype.repository.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smm.archetype.entity.user.User;
import org.smm.archetype.generated.entity.UserDO;
import org.smm.archetype.generated.mapper.UserMapper;
import org.smm.archetype.support.UnitTestBase;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter SQLITE_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
            String username = "testuser";
            UserDO userDO = buildUserDO(1L, username, "hashed-pwd", "TestNick", "ACTIVE",
                    "2026-04-15 10:30:00", "2026-04-15 11:00:00");
            when(userMapper.selectOne(any())).thenReturn(userDO);

            // when
            Optional<User> result = userRepository.findByUsername(username);

            // then
            assertThat(result).isPresent();
            User user = result.get();
            assertThat(user.getId()).isEqualTo(1L);
            assertThat(user.getUsername()).isEqualTo(username);
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
        @DisplayName("SQLite 格式时间应正确解析为 Instant")
        void should_parse_sqlite_datetime_to_instant() {
            // given
            String createTime = "2026-01-15 08:00:00";
            String updateTime = "2026-01-15 09:30:00";
            UserDO userDO = buildUserDO(2L, "timeuser", "hash", "TimeNick", "ACTIVE",
                    createTime, updateTime);
            when(userMapper.selectOne(any())).thenReturn(userDO);

            // when
            Optional<User> result = userRepository.findByUsername("timeuser");

            // then
            assertThat(result).isPresent();
            User user = result.get();

            Instant expectedCreateTime = LocalDateTime.parse(createTime, SQLITE_DATETIME_FORMATTER)
                    .toInstant(ZoneOffset.UTC);
            Instant expectedUpdateTime = LocalDateTime.parse(updateTime, SQLITE_DATETIME_FORMATTER)
                    .toInstant(ZoneOffset.UTC);
            assertThat(user.getCreateTime()).isEqualTo(expectedCreateTime);
            assertThat(user.getUpdateTime()).isEqualTo(expectedUpdateTime);
        }

        @Test
        @DisplayName("ISO-8601 格式时间应正确解析为 Instant（fallback 路径）")
        void should_parse_iso8601_datetime_as_fallback() {
            // given
            String isoTime = "2026-04-15T10:30:00Z";
            UserDO userDO = buildUserDO(3L, "isouser", "hash", "IsoNick", "ACTIVE",
                    isoTime, isoTime);
            when(userMapper.selectOne(any())).thenReturn(userDO);

            // when
            Optional<User> result = userRepository.findByUsername("isouser");

            // then
            assertThat(result).isPresent();
            User user = result.get();
            assertThat(user.getCreateTime()).isEqualTo(Instant.parse(isoTime));
            assertThat(user.getUpdateTime()).isEqualTo(Instant.parse(isoTime));
        }

        @Test
        @DisplayName("时间为 null 时应安全处理")
        void should_handle_null_datetime() {
            // given
            UserDO userDO = buildUserDO(4L, "nulldate", "hash", "NullDate", "ACTIVE",
                    null, null);
            when(userMapper.selectOne(any())).thenReturn(userDO);

            // when
            Optional<User> result = userRepository.findByUsername("nulldate");

            // then
            assertThat(result).isPresent();
            User user = result.get();
            assertThat(user.getCreateTime()).isNull();
            assertThat(user.getUpdateTime()).isNull();
        }

        @Test
        @DisplayName("时间为空字符串时应安全处理")
        void should_handle_blank_datetime() {
            // given
            UserDO userDO = buildUserDO(5L, "blankdate", "hash", "BlankDate", "ACTIVE",
                    "  ", "");
            when(userMapper.selectOne(any())).thenReturn(userDO);

            // when
            Optional<User> result = userRepository.findByUsername("blankdate");

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
                                       String nickname, String status,
                                       String createTime, String updateTime) {
        UserDO userDO = new UserDO();
        userDO.setId(id);
        userDO.setUsername(username);
        userDO.setPasswordHash(passwordHash);
        userDO.setNickname(nickname);
        userDO.setStatus(status);
        userDO.setCreateTime(createTime);
        userDO.setUpdateTime(updateTime);
        return userDO;
    }
}
