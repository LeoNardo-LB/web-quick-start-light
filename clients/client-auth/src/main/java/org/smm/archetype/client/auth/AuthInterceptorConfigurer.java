package org.smm.archetype.client.auth;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 拦截器自动配置。
 * <p>
 * 仅在 Sa-Token 在 classpath 且认证功能启用时注册。
 * 拦截所有请求，排除 excludePaths 中配置的路径。
 */
@AutoConfiguration
@ConditionalOnClass(name = "cn.dev33.satoken.stp.StpUtil")
@ConditionalOnProperty(prefix = "middleware.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class AuthInterceptorConfigurer implements WebMvcConfigurer {

    private final AuthProperties properties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        String[] excludeArray = properties.getExcludePaths().toArray(new String[0]);
        registry.addInterceptor(new SaInterceptor(handle -> {
            SaRouter.match("/**")
                    .notMatch(excludeArray)
                    .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");
    }
}
