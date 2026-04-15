package org.smm.archetype.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.component.auth.AuthComponent;
import org.smm.archetype.controller.global.ContextFillFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

@Configuration
@Slf4j
public class WebConfigure {

    @Autowired(required = false)
    private AuthComponent authComponent;

    @Bean
    public FilterRegistrationBean<ContextFillFilter> contextFillFilter() {
        FilterRegistrationBean<ContextFillFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ContextFillFilter(authComponent));
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        registration.setName("contextFillFilter");
        return registration;
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("http://localhost:*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    /**
     * LocaleResolver: 从 Accept-Language 头解析 locale，默认中文
     * <p>
     * 注意：标准 AcceptHeaderLocaleResolver 在无 Accept-Language 头时
     * 会返回 request.getLocale()（即 JVM 默认 locale），而非 defaultLocale。
     * 这里直接检查 Accept-Language 头是否存在来决定是否使用默认值。
     */
    @Bean
    public LocaleResolver localeResolver() {
        return new LocaleResolver() {
            private final Locale defaultLocale = Locale.SIMPLIFIED_CHINESE;

            @Override
            public Locale resolveLocale(HttpServletRequest request) {
                String acceptLanguage = request.getHeader("Accept-Language");
                Locale resolved;
                if (acceptLanguage == null || acceptLanguage.isBlank()) {
                    resolved = defaultLocale;
                } else {
                    resolved = Locale.forLanguageTag(acceptLanguage.trim());
                }
                log.debug("LocaleResolver: Accept-Language='{}', resolved={}", acceptLanguage, resolved);
                return resolved;
            }

            @Override
            public void setLocale(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Locale locale) {
                // 无操作：locale 由 Accept-Language 头控制
            }
        };
    }

    /**
     * MessageSource: 国际化消息源
     * <p>
     * 禁用 fallbackToSystemLocale，确保非英文 locale（如 zh_CN、fr）
     * 回退到 messages.properties（中文默认），而非 messages_en.properties。
     * <p>
     * 默认情况下，Java ResourceBundle 在找不到目标 locale 对应的 bundle 时，
     * 会回退到 JVM 默认 locale 的 bundle（如 en → messages_en.properties），
     * 而非 root bundle（messages.properties）。
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setUseCodeAsDefaultMessage(false);
        return messageSource;
    }

    /**
     * Validator: 使用 Spring MessageSource 进行校验消息国际化
     * <p>
     * 将 Hibernate Validator 的消息解析委托给 Spring MessageSource，
     * 这样校验消息也能根据 Accept-Language 头正确切换语言，
     * 且不受 JVM 默认 locale 的影响。
     */
    @Bean
    public Validator validator(MessageSource messageSource) {
        LocalValidatorFactoryBean validatorFactory = new LocalValidatorFactoryBean();
        validatorFactory.setValidationMessageSource(messageSource);
        return validatorFactory;
    }
}
