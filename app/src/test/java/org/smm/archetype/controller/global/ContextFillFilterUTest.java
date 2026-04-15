package org.smm.archetype.controller.global;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.smm.archetype.component.auth.AuthComponent;
import org.smm.archetype.support.UnitTestBase;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

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
    // resolveTraceId
    // =========================================================================

    @Nested
    @DisplayName("resolveTraceId")
    class ResolveTraceId {

        @Test
        @DisplayName("Header 中已有 X-Trace-Id 时应透传该值")
        void should_pass_through_trace_id_from_header() throws Exception {
            ContextFillFilter filter = new ContextFillFilter();
            when(request.getHeader("X-Trace-Id")).thenReturn("existing-trace-id");

            Method method = ContextFillFilter.class.getDeclaredMethod("resolveTraceId", HttpServletRequest.class);
            method.setAccessible(true);
            String traceId = (String) method.invoke(filter, request);

            assertThat(traceId).isEqualTo("existing-trace-id");
            assertThat(traceId).isNotEmpty();
            verify(request).getHeader("X-Trace-Id");
        }

        @Test
        @DisplayName("Header 中 X-Trace-Id 为空字符串时应自动生成")
        void should_generate_trace_id_when_header_is_empty() throws Exception {
            ContextFillFilter filter = new ContextFillFilter();
            when(request.getHeader("X-Trace-Id")).thenReturn("");

            Method method = ContextFillFilter.class.getDeclaredMethod("resolveTraceId", HttpServletRequest.class);
            method.setAccessible(true);
            String traceId = (String) method.invoke(filter, request);

            assertThat(traceId).isNotEmpty();
            assertThat(traceId).doesNotContain("-");
            assertThat(traceId).hasSize(32);
        }

        @Test
        @DisplayName("Header 中不存在 X-Trace-Id 时应自动生成")
        void should_generate_trace_id_when_header_is_missing() throws Exception {
            ContextFillFilter filter = new ContextFillFilter();
            when(request.getHeader("X-Trace-Id")).thenReturn(null);

            Method method = ContextFillFilter.class.getDeclaredMethod("resolveTraceId", HttpServletRequest.class);
            method.setAccessible(true);
            String traceId = (String) method.invoke(filter, request);

            assertThat(traceId).isNotNull();
            assertThat(traceId).isNotEmpty();
            assertThat(traceId).doesNotContain("-");
        }
    }

    // =========================================================================
    // doFilterInternal
    // =========================================================================

    @Nested
    @DisplayName("doFilterInternal")
    class DoFilterInternal {

        @Test
        @DisplayName("应将 traceId 设置到 response header 并继续过滤链")
        void should_set_trace_id_to_response_and_continue_chain() throws Exception {
            ContextFillFilter filter = new ContextFillFilter();
            when(request.getHeader("X-Trace-Id")).thenReturn("my-trace-123");

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setHeader("X-Trace-Id", "my-trace-123");
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("无 AuthComponent 时应使用 SYSTEM 作为 userId 并正常完成过滤")
        void should_use_system_user_when_no_auth_client() throws Exception {
            ContextFillFilter filter = new ContextFillFilter();
            when(request.getHeader("X-Trace-Id")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setHeader(eq("X-Trace-Id"), anyString());
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(authComponent);
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
