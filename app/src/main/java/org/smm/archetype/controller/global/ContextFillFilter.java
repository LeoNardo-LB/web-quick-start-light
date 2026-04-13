package org.smm.archetype.controller.global;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.client.auth.AuthClient;
import org.smm.archetype.util.context.ScopedThreadContext;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
public class ContextFillFilter extends OncePerRequestFilter {
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String ANONYMOUS_USER = "ANONYMOUS";
    private static final String SYSTEM_USER = "SYSTEM";

    private final AuthClient authClient;

    /**
     * 兼容旧调用方式：无 AuthClient 时使用 SYSTEM 作为 userId。
     */
    public ContextFillFilter() {
        this(null);
    }

    /**
     * 注入 AuthClient，用于获取当前登录用户 ID。
     *
     * @param authClient 认证客户端（可为 null）
     */
    public ContextFillFilter(@Nullable AuthClient authClient) {
        this.authClient = authClient;
    }

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response,
                                    @Nonnull FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        response.setHeader(TRACE_ID_HEADER, traceId);
        String userId = resolveUserId();
        ScopedThreadContext.runWithContext(() -> {
            try {
                filterChain.doFilter(request, response);
            } catch (IOException | ServletException e) {
                log.error("Error processing request", e);
                throw new RuntimeException(e);
            }
        }, userId, traceId);
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        return traceId;
    }

    private String resolveUserId() {
        if (authClient == null) {
            return SYSTEM_USER;
        }
        String userId = authClient.getCurrentUserId();
        return userId != null ? userId : ANONYMOUS_USER;
    }
}
