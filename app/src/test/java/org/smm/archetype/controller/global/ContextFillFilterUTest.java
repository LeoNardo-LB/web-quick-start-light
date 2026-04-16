package org.smm.archetype.controller.global;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.smm.archetype.component.auth.AuthComponent;
import org.smm.archetype.support.UnitTestBase;

import java.io.IOException;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ContextFillFilter")
class ContextFillFilterUTest extends UnitTestBase {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private AuthComponent authComponent;

    // =========================================================================
    // resolveUserId
    // =========================================================================

    @Nested
    @DisplayName("resolveUserId")
    class ResolveUserId {

        @Test
        @DisplayName("无 AuthComponent 时应返回 SYSTEM")
        void should_return_system_when_no_auth_client() throws Exception {
            ContextFillFilter filter = new ContextFillFilter();

            Method method = ContextFillFilter.class.getDeclaredMethod("resolveUserId");
            method.setAccessible(true);
            String userId = (String) method.invoke(filter);

            assertThat(userId).isEqualTo("SYSTEM");
            assertThat(userId).isNotEmpty();
        }

        @Test
        @DisplayName("AuthComponent 返回有效 userId 时应使用该值")
        void should_return_user_id_from_auth_client() throws Exception {
            ContextFillFilter filter = new ContextFillFilter(authComponent);
            when(authComponent.getCurrentUserId()).thenReturn("user-42");

            Method method = ContextFillFilter.class.getDeclaredMethod("resolveUserId");
            method.setAccessible(true);
            String userId = (String) method.invoke(filter);

            assertThat(userId).isEqualTo("user-42");
            assertThat(userId).isNotEqualTo("ANONYMOUS");
            assertThat(userId).isNotEqualTo("SYSTEM");
        }

        @Test
        @DisplayName("AuthComponent 返回 null 时应返回 ANONYMOUS")
        void should_return_anonymous_when_auth_client_returns_null() throws Exception {
            ContextFillFilter filter = new ContextFillFilter(authComponent);
            when(authComponent.getCurrentUserId()).thenReturn(null);

            Method method = ContextFillFilter.class.getDeclaredMethod("resolveUserId");
            method.setAccessible(true);
            String userId = (String) method.invoke(filter);

            assertThat(userId).isEqualTo("ANONYMOUS");
            assertThat(userId).isNotEqualTo("SYSTEM");
        }
    }

    // =========================================================================
    // doFilterInternal
    // =========================================================================

    @Nested
    @DisplayName("doFilterInternal")
    class DoFilterInternal {

        @Test
        @DisplayName("应正常执行过滤链")
        void should_continue_filter_chain() throws Exception {
            ContextFillFilter filter = new ContextFillFilter();

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("无 AuthComponent 时应使用 SYSTEM 作为 userId 并正常完成过滤")
        void should_use_system_user_when_no_auth_client() throws Exception {
            ContextFillFilter filter = new ContextFillFilter();

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("有 AuthComponent 时应从 AuthComponent 获取 userId")
        void should_get_user_id_from_auth_component() throws Exception {
            when(authComponent.getCurrentUserId()).thenReturn("user-99");
            ContextFillFilter filter = new ContextFillFilter(authComponent);

            filter.doFilterInternal(request, response, filterChain);

            verify(authComponent).getCurrentUserId();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("AuthComponent 返回 null 时 doFilterInternal 应使用 ANONYMOUS 作为 userId")
        void should_use_anonymous_when_auth_component_returns_null() throws Exception {
            when(authComponent.getCurrentUserId()).thenReturn(null);
            ContextFillFilter filter = new ContextFillFilter(authComponent);

            filter.doFilterInternal(request, response, filterChain);

            verify(authComponent).getCurrentUserId();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("filterChain 抛出 IOException 时应包装为 RuntimeException")
        void should_wrap_io_exception_as_runtime_exception() throws Exception {
            ContextFillFilter filter = new ContextFillFilter();
            doThrow(new IOException("simulated IO error"))
                    .when(filterChain).doFilter(request, response);

            assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
                    .isInstanceOf(RuntimeException.class)
                    .hasCauseInstanceOf(IOException.class);
        }

        @Test
        @DisplayName("filterChain 抛出 ServletException 时应包装为 RuntimeException")
        void should_wrap_servlet_exception_as_runtime_exception() throws Exception {
            ContextFillFilter filter = new ContextFillFilter();
            doThrow(new ServletException("simulated servlet error"))
                    .when(filterChain).doFilter(request, response);

            assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
                    .isInstanceOf(RuntimeException.class)
                    .hasCauseInstanceOf(ServletException.class);
        }
    }

    // =========================================================================
    // 构造器
    // =========================================================================

    @Nested
    @DisplayName("构造器")
    class Constructor {

        @Test
        @DisplayName("无参构造器应创建 filter 且 resolveUserId 返回 SYSTEM")
        void should_create_filter_without_auth_client() throws Exception {
            ContextFillFilter filter = new ContextFillFilter();
            assertThat(filter).isNotNull();

            Method method = ContextFillFilter.class.getDeclaredMethod("resolveUserId");
            method.setAccessible(true);
            String userId = (String) method.invoke(filter);
            assertThat(userId).isEqualTo("SYSTEM");
        }

        @Test
        @DisplayName("有参构造器应接受 AuthComponent（可为 null）")
        void should_create_filter_with_auth_client() {
            ContextFillFilter filter = new ContextFillFilter(authComponent);
            assertThat(filter).isNotNull();
            assertThat(filter).isInstanceOf(org.springframework.web.filter.OncePerRequestFilter.class);
        }
    }
}
