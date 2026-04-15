package org.smm.archetype.component.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NoOpAuthComponent 单元测试。
 */
class NoOpAuthComponentUTest {

    private final NoOpAuthComponent client = new NoOpAuthComponent();

    @Nested
    @DisplayName("NoOpAuthComponent 默认行为")
    class NoOpBehaviorTest {

        @Test
        @DisplayName("login 返回 null")
        void loginShouldReturnNull() {
            assertNull(client.login("any-user"));
        }

        @Test
        @DisplayName("logout 无操作不抛异常")
        void logoutShouldNotThrow() {
            assertDoesNotThrow(client::logout);
        }

        @Test
        @DisplayName("getCurrentUserId 返回 null")
        void getCurrentUserIdShouldReturnNull() {
            assertNull(client.getCurrentUserId());
        }

        @Test
        @DisplayName("isLogin 返回 true")
        void isLoginShouldReturnTrue() {
            assertTrue(client.isLogin());
        }

        @Test
        @DisplayName("checkLogin 无操作不抛异常")
        void checkLoginShouldNotThrow() {
            assertDoesNotThrow(client::checkLogin);
        }
    }
}
