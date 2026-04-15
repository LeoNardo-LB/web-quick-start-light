package org.smm.archetype.component.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.exception.BizException;
import org.smm.archetype.exception.CommonErrorCode;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AuthComponent 接口 + AbstractAuthComponent 单元测试。
 */
class AuthComponentUTest {

    // ==================== AuthComponent 接口契约测试 ====================

    @Nested
    @DisplayName("AuthComponent 接口应包含 5 个方法")
    class AuthComponentInterfaceTest {

        @Test
        @DisplayName("接口包含 login 方法")
        void shouldHaveLoginMethod() throws NoSuchMethodException {
            Method method = AuthComponent.class.getMethod("login", Object.class);
            assertNotNull(method);
        }

        @Test
        @DisplayName("接口包含 logout 方法")
        void shouldHaveLogoutMethod() throws NoSuchMethodException {
            Method method = AuthComponent.class.getMethod("logout");
            assertNotNull(method);
        }

        @Test
        @DisplayName("接口包含 getCurrentUserId 方法")
        void shouldHaveGetCurrentUserIdMethod() throws NoSuchMethodException {
            Method method = AuthComponent.class.getMethod("getCurrentUserId");
            assertNotNull(method);
        }

        @Test
        @DisplayName("接口包含 isLogin 方法")
        void shouldHaveIsLoginMethod() throws NoSuchMethodException {
            Method method = AuthComponent.class.getMethod("isLogin");
            assertNotNull(method);
        }

        @Test
        @DisplayName("接口包含 checkLogin 方法")
        void shouldHaveCheckLoginMethod() throws NoSuchMethodException {
            Method method = AuthComponent.class.getMethod("checkLogin");
            assertNotNull(method);
        }
    }

    // ==================== AbstractAuthComponent Template Method 测试 ====================

    @Nested
    @DisplayName("AbstractAuthComponent 公开方法应为 final")
    class FinalMethodTest {

        @Test
        @DisplayName("login 方法应为 final")
        void loginShouldBeFinal() throws NoSuchMethodException {
            Method method = AbstractAuthComponent.class.getMethod("login", Object.class);
            assertTrue(Modifier.isFinal(method.getModifiers()), "login should be final");
        }

        @Test
        @DisplayName("logout 方法应为 final")
        void logoutShouldBeFinal() throws NoSuchMethodException {
            Method method = AbstractAuthComponent.class.getMethod("logout");
            assertTrue(Modifier.isFinal(method.getModifiers()), "logout should be final");
        }

        @Test
        @DisplayName("getCurrentUserId 方法应为 final")
        void getCurrentUserIdShouldBeFinal() throws NoSuchMethodException {
            Method method = AbstractAuthComponent.class.getMethod("getCurrentUserId");
            assertTrue(Modifier.isFinal(method.getModifiers()), "getCurrentUserId should be final");
        }

        @Test
        @DisplayName("isLogin 方法应为 final")
        void isLoginShouldBeFinal() throws NoSuchMethodException {
            Method method = AbstractAuthComponent.class.getMethod("isLogin");
            assertTrue(Modifier.isFinal(method.getModifiers()), "isLogin should be final");
        }

        @Test
        @DisplayName("checkLogin 方法应为 final")
        void checkLoginShouldBeFinal() throws NoSuchMethodException {
            Method method = AbstractAuthComponent.class.getMethod("checkLogin");
            assertTrue(Modifier.isFinal(method.getModifiers()), "checkLogin should be final");
        }
    }

    @Nested
    @DisplayName("AbstractAuthComponent.login 参数校验")
    class LoginValidationTest {

        private AbstractAuthComponent createStub(boolean loggedIn, String userId) {
            return new AbstractAuthComponent() {
                @Override
                protected String doLogin(Object userId) {
                    return "test-token-" + userId;
                }

                @Override
                protected void doLogout() {
                    // no-op
                }

                @Override
                protected String doGetCurrentUserId() {
                    return userId;
                }

                @Override
                protected boolean doIsLogin() {
                    return loggedIn;
                }
            };
        }

        @Test
        @DisplayName("userId 为 null 时应抛 BizException")
        void shouldThrowWhenUserIdIsNull() {
            AbstractAuthComponent client = createStub(false, null);
            BizException ex = assertThrows(BizException.class, () -> client.login(null));
            assertEquals(CommonErrorCode.FAIL.code(), ex.getErrorCode().code());
            assertTrue(ex.getMessage().contains("userId"));
        }

        @Test
        @DisplayName("userId 不为 null 时正常调用 doLogin")
        void shouldCallDoLoginWithValidUserId() {
            AbstractAuthComponent client = createStub(false, null);
            String token = client.login("user123");
            assertEquals("test-token-user123", token);
        }
    }

    @Nested
    @DisplayName("AbstractAuthComponent.checkLogin 行为")
    class CheckLoginBehaviorTest {

        @Test
        @DisplayName("未登录时 checkLogin 应抛 BizException (AUTH_UNAUTHORIZED)")
        void shouldThrowWhenNotLoggedIn() {
            AbstractAuthComponent client = new AbstractAuthComponent() {
                @Override
                protected String doLogin(Object userId) { return null; }
                @Override
                protected void doLogout() { }
                @Override
                protected String doGetCurrentUserId() { return null; }
                @Override
                protected boolean doIsLogin() { return false; }
            };
            BizException ex = assertThrows(BizException.class, client::checkLogin);
            assertEquals(CommonErrorCode.AUTH_UNAUTHORIZED.code(), ex.getErrorCode().code());
        }

        @Test
        @DisplayName("已登录时 checkLogin 不应抛异常")
        void shouldNotThrowWhenLoggedIn() {
            AbstractAuthComponent client = new AbstractAuthComponent() {
                @Override
                protected String doLogin(Object userId) { return "token"; }
                @Override
                protected void doLogout() { }
                @Override
                protected String doGetCurrentUserId() { return "user1"; }
                @Override
                protected boolean doIsLogin() { return true; }
            };
            assertDoesNotThrow(client::checkLogin);
        }
    }

    @Nested
    @DisplayName("AbstractAuthComponent 委托测试")
    class DelegateTest {

        @Test
        @DisplayName("logout 委托给 doLogout")
        void logoutDelegatesToDoLogout() {
            boolean[] called = {false};
            AbstractAuthComponent client = new AbstractAuthComponent() {
                @Override
                protected String doLogin(Object userId) { return null; }
                @Override
                protected void doLogout() { called[0] = true; }
                @Override
                protected String doGetCurrentUserId() { return null; }
                @Override
                protected boolean doIsLogin() { return false; }
            };
            client.logout();
            assertTrue(called[0], "doLogout should have been called");
        }

        @Test
        @DisplayName("getCurrentUserId 委托给 doGetCurrentUserId")
        void getCurrentUserIdDelegatesToDoGetCurrentUserId() {
            AbstractAuthComponent client = new AbstractAuthComponent() {
                @Override
                protected String doLogin(Object userId) { return null; }
                @Override
                protected void doLogout() { }
                @Override
                protected String doGetCurrentUserId() { return "user-42"; }
                @Override
                protected boolean doIsLogin() { return true; }
            };
            assertEquals("user-42", client.getCurrentUserId());
        }

        @Test
        @DisplayName("isLogin 委托给 doIsLogin")
        void isLoginDelegatesToDoIsLogin() {
            AbstractAuthComponent client = new AbstractAuthComponent() {
                @Override
                protected String doLogin(Object userId) { return null; }
                @Override
                protected void doLogout() { }
                @Override
                protected String doGetCurrentUserId() { return null; }
                @Override
                protected boolean doIsLogin() { return true; }
            };
            assertTrue(client.isLogin());
        }
    }
}
