package org.smm.archetype.component.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.exception.BizException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * NoOpAuthComponent 单元测试。
 * <p>
 * 验证空操作认证组件的默认行为：所有认证操作为 no-op，isLogin 始终返回 true。
 */
class NoOpAuthComponentUTest {

    private final NoOpAuthComponent client = new NoOpAuthComponent();

    @Nested
    @DisplayName("NoOpAuthComponent 默认行为")
    class NoOpBehaviorTest {

        @Test
        @DisplayName("login 返回 null（接受任意 userId 类型）")
        void loginShouldReturnNull() {
            assertThat(client.login("any-user")).isNull();
        }

        @Test
        @DisplayName("login 接受不同类型的 userId 均返回 null")
        void loginShouldAcceptDifferentUserIdTypes() {
            assertThat(client.login("string-user")).isNull();
            assertThat(client.login(12345L)).isNull();
            assertThat(client.login(1)).isNull();
        }

        @Test
        @DisplayName("login 传入 null userId 应抛出 BizException")
        void loginShouldThrowWhenUserIdIsNull() {
            assertThatThrownBy(() -> client.login(null))
                    .isInstanceOf(BizException.class);
        }

        @Test
        @DisplayName("logout 无操作不抛异常")
        void logoutShouldNotThrow() {
            assertThatNoException().isThrownBy(client::logout);
        }

        @Test
        @DisplayName("logout 多次调用均安全无异常")
        void logoutShouldBeIdempotent() {
            assertThatNoException().isThrownBy(() -> {
                client.logout();
                client.logout();
                client.logout();
            });
        }

        @Test
        @DisplayName("getCurrentUserId 返回 null")
        void getCurrentUserIdShouldReturnNull() {
            assertThat(client.getCurrentUserId()).isNull();
        }

        @Test
        @DisplayName("getCurrentUserId 多次调用返回一致的 null")
        void getCurrentUserIdShouldReturnConsistentNull() {
            for (int i = 0; i < 5; i++) {
                assertThat(client.getCurrentUserId()).isNull();
            }
        }

        @Test
        @DisplayName("isLogin 返回 true")
        void isLoginShouldReturnTrue() {
            assertThat(client.isLogin()).isTrue();
        }

        @Test
        @DisplayName("isLogin 多次调用返回一致的 true")
        void isLoginShouldReturnConsistentTrue() {
            for (int i = 0; i < 5; i++) {
                assertThat(client.isLogin()).isTrue();
            }
        }

        @Test
        @DisplayName("checkLogin 无操作不抛异常（因 isLogin 返回 true）")
        void checkLoginShouldNotThrow() {
            assertThatNoException().isThrownBy(client::checkLogin);
        }
    }
}
