package org.smm.archetype.controller.global;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.component.auth.AuthComponent;
import org.smm.archetype.shared.util.context.BizContext;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 上下文桥接 Filter。
 * <p>
 * 职责：
 * <ol>
 *   <li>从 AuthComponent 获取 userId</li>
 *   <li>通过 {@link BizContext#runWithContext} 同时填充：
 *       <ul>
 *         <li>ScopedValue（业务上下文：userId）</li>
 *         <li>OTel Baggage（propagated=true 的键自动同步，如 userId）</li>
 *       </ul>
 *   </li>
 * </ol>
 * <p>
 * 注意：traceId 由 OTel Span 全权负责，通过 BaseResult.traceId 返回给前端，
 * 不需要手动设置 response header。
 */
@Slf4j
public class ContextFillFilter extends OncePerRequestFilter {
    private static final String ANONYMOUS_USER = "ANONYMOUS";
    private static final String SYSTEM_USER = "SYSTEM";

    private final AuthComponent authComponent;

    public ContextFillFilter() {
        this(null);
    }

    public ContextFillFilter(@Nullable AuthComponent authComponent) {
        this.authComponent = authComponent;
    }

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response,
                                    @Nonnull FilterChain filterChain) throws ServletException, IOException {
        // 1. 解析 userId
        String userId = resolveUserId();

        // 2. runWithContext 自动处理 ScopedValue + OTel Baggage（propagated 键自动同步）
        BizContext.runWithContext(() -> {
            try {
                filterChain.doFilter(request, response);
            } catch (IOException | ServletException e) {
                throw new RuntimeException(e);
            }
        }, BizContext.Key.USER_ID, userId);
    }

    private String resolveUserId() {
        if (authComponent == null) {
            return SYSTEM_USER;
        }
        String userId = authComponent.getCurrentUserId();
        return userId != null ? userId : ANONYMOUS_USER;
    }
}
