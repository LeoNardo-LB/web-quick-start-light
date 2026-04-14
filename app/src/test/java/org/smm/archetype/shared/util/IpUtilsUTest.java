package org.smm.archetype.shared.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smm.archetype.support.UnitTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * {@link IpUtils} 单元测试
 *
 * @author Leonardo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IP 地址工具类 - IpUtils")
class IpUtilsUTest {

    @Mock
    private HttpServletRequest request;

    // ================================================================
    // getIpAddr
    // ================================================================

    @Nested
    @DisplayName("getIpAddr - 从请求中获取客户端 IP")
    class GetIpAddr {

        @Test
        @DisplayName("X-Forwarded-For 单个 IP：直接返回该 IP")
        void should_return_ip_when_xForwardedFor_has_single_ip() {
            // arrange
            when(request.getHeader("x-forwarded-for")).thenReturn("192.168.1.100");

            // act
            String result = IpUtils.getIpAddr(request);

            // assert
            assertThat(result).isEqualTo("192.168.1.100");
            verify(request, never()).getRemoteAddr();
        }

        @Test
        @DisplayName("X-Forwarded-For 多个 IP（逗号分隔）：返回完整字符串（源码不做拆分）")
        void should_return_full_string_when_xForwardedFor_has_multiple_ips() {
            // arrange
            when(request.getHeader("x-forwarded-for")).thenReturn("192.168.1.100, 10.0.0.1, 172.16.0.1");

            // act
            String result = IpUtils.getIpAddr(request);

            // assert
            assertThat(result).startsWith("192.168.1.100");
            assertThat(result).contains(", ");
            verify(request, never()).getRemoteAddr();
        }

        @Test
        @DisplayName("X-Forwarded-For 为 unknown（忽略大小写）：回退到 getRemoteAddr")
        void should_fallback_to_remoteAddr_when_xForwardedFor_is_unknown() {
            // arrange
            when(request.getHeader("x-forwarded-for")).thenReturn("Unknown");
            when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
            when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
            when(request.getHeader("HTTP_CLIENT_IP")).thenReturn(null);
            when(request.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            // act
            String result = IpUtils.getIpAddr(request);

            // assert
            assertThat(result).isEqualTo("127.0.0.1");
            verify(request).getRemoteAddr();
        }

        @Test
        @DisplayName("X-Forwarded-For 为 unknown（全小写）：回退到 getRemoteAddr")
        void should_fallback_to_remoteAddr_when_xForwardedFor_is_lowercase_unknown() {
            // arrange
            when(request.getHeader("x-forwarded-for")).thenReturn("unknown");
            when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
            when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
            when(request.getHeader("HTTP_CLIENT_IP")).thenReturn(null);
            when(request.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("10.0.0.5");

            // act
            String result = IpUtils.getIpAddr(request);

            // assert
            assertThat(result).isEqualTo("10.0.0.5");
            verify(request).getRemoteAddr();
        }

        @Test
        @DisplayName("所有 header 均为 null：使用 getRemoteAddr")
        void should_use_remoteAddr_when_all_headers_are_null() {
            // arrange
            when(request.getHeader(anyString())).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("192.168.0.1");

            // act
            String result = IpUtils.getIpAddr(request);

            // assert
            assertThat(result).isEqualTo("192.168.0.1");
            verify(request).getRemoteAddr();
        }

        @Test
        @DisplayName("X-Forwarded-For 为空字符串：回退到下一个 header")
        void should_fallback_when_xForwardedFor_is_empty() {
            // arrange
            when(request.getHeader("x-forwarded-for")).thenReturn("");
            when(request.getHeader("Proxy-Client-IP")).thenReturn("10.10.10.10");

            // act
            String result = IpUtils.getIpAddr(request);

            // assert
            assertThat(result).isEqualTo("10.10.10.10");
            verify(request).getHeader("Proxy-Client-IP");
            verify(request, never()).getRemoteAddr();
        }

        @Test
        @DisplayName("Proxy-Client-IP 有效：返回该 IP")
        void should_return_proxyClientIp_when_xForwardedFor_is_invalid() {
            // arrange
            when(request.getHeader("x-forwarded-for")).thenReturn(null);
            when(request.getHeader("Proxy-Client-IP")).thenReturn("172.16.254.1");

            // act
            String result = IpUtils.getIpAddr(request);

            // assert
            assertThat(result).isEqualTo("172.16.254.1");
            verify(request, never()).getRemoteAddr();
        }

        @Test
        @DisplayName("WL-Proxy-Client-IP 有效：前面所有 header 无效时返回该 IP")
        void should_return_wlProxyClientIp_when_previous_headers_invalid() {
            // arrange
            when(request.getHeader("x-forwarded-for")).thenReturn(null);
            when(request.getHeader("Proxy-Client-IP")).thenReturn("unknown");
            when(request.getHeader("WL-Proxy-Client-IP")).thenReturn("10.20.30.40");

            // act
            String result = IpUtils.getIpAddr(request);

            // assert
            assertThat(result).isEqualTo("10.20.30.40");
            verify(request, never()).getRemoteAddr();
        }

        @Test
        @DisplayName("HTTP_CLIENT_IP 有效：返回该 IP")
        void should_return_httpClientIp_when_previous_headers_invalid() {
            // arrange
            when(request.getHeader("x-forwarded-for")).thenReturn(null);
            when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
            when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
            when(request.getHeader("HTTP_CLIENT_IP")).thenReturn("10.30.30.30");

            // act
            String result = IpUtils.getIpAddr(request);

            // assert
            assertThat(result).isEqualTo("10.30.30.30");
            verify(request, never()).getRemoteAddr();
        }

        @Test
        @DisplayName("HTTP_X_FORWARDED_FOR 有效：返回该 IP")
        void should_return_httpXForwardedFor_when_previous_headers_invalid() {
            // arrange
            when(request.getHeader("x-forwarded-for")).thenReturn(null);
            when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
            when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
            when(request.getHeader("HTTP_CLIENT_IP")).thenReturn(null);
            when(request.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn("10.40.40.40");

            // act
            String result = IpUtils.getIpAddr(request);

            // assert
            assertThat(result).isEqualTo("10.40.40.40");
            verify(request, never()).getRemoteAddr();
        }

        @Test
        @DisplayName("异常场景：getHeader 抛出异常时返回 null")
        void should_return_null_when_exception_thrown() {
            // arrange
            when(request.getHeader("x-forwarded-for")).thenThrow(new RuntimeException("mock error"));

            // act
            String result = IpUtils.getIpAddr(request);

            // assert
            assertThat(result).isNull();
            verify(request).getHeader("x-forwarded-for");
        }
    }
}
