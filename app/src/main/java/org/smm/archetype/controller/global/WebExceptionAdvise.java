package org.smm.archetype.controller.global;

import cn.dev33.satoken.exception.NotLoginException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.entity.base.BaseResult;
import org.smm.archetype.exception.*;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 全局异常处理器 — 支持国际化
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class WebExceptionAdvise {

    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResult<Void>> handleValidationException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        Locale locale = localeResolver.resolveLocale(request);
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return ResponseEntity.ok(BaseResult.fail(CommonErrorCode.ILLEGAL_ARGUMENT, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResult<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("约束违反: {}", message);
        return ResponseEntity.ok(BaseResult.fail(CommonErrorCode.ILLEGAL_ARGUMENT, message));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<BaseResult<Void>> handleBindException(
            BindException e, HttpServletRequest request) {
        String message = e.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败: {}", message);
        return ResponseEntity.ok(BaseResult.fail(CommonErrorCode.ILLEGAL_ARGUMENT, message));
    }

    @ExceptionHandler(BizException.class)
    public ResponseEntity<BaseResult<Void>> handleBizException(
            BizException e, HttpServletRequest request) {
        Locale locale = localeResolver.resolveLocale(request);
        String message = resolveExceptionMessage(e, locale);
        log.warn("业务异常: code={}, message={}", e.getErrorCode().code(), message);
        return ResponseEntity.ok(BaseResult.fail(e.getErrorCode(), message));
    }

    @ExceptionHandler(ClientException.class)
    public ResponseEntity<BaseResult<Void>> handleClientException(
            ClientException e, HttpServletRequest request) {
        Locale locale = localeResolver.resolveLocale(request);
        String message = resolveExceptionMessage(e, locale);
        log.error("外部服务异常: code={}, message={}", e.getErrorCode().code(), message, e);
        return ResponseEntity.ok(BaseResult.fail(e.getErrorCode(), message));
    }

    @ExceptionHandler(SysException.class)
    public ResponseEntity<BaseResult<Void>> handleSysException(
            SysException e, HttpServletRequest request) {
        Locale locale = localeResolver.resolveLocale(request);
        String message = resolveExceptionMessage(e, locale);
        log.error("系统异常: code={}, message={}", e.getErrorCode().code(), message, e);
        return ResponseEntity.ok(BaseResult.fail(e.getErrorCode(), message));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<BaseResult<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("资源不存在: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BaseResult.fail(CommonErrorCode.FAIL, e.getMessage()));
    }

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<BaseResult<Void>> handleNotLoginException(
            NotLoginException e, HttpServletRequest request) {
        Locale locale = localeResolver.resolveLocale(request);
        String message = resolveMessage(CommonErrorCode.AUTH_UNAUTHORIZED.messageKey(), locale);
        log.warn("未登录或登录已过期: {}", message);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(BaseResult.fail(CommonErrorCode.AUTH_UNAUTHORIZED, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResult<Void>> handleException(Exception e) {
        log.error("未知异常", e);
        return ResponseEntity.ok(BaseResult.fail(CommonErrorCode.UNKNOWN_ERROR, e.getMessage()));
    }

    /**
     * 解析异常消息：自定义消息保持不变，默认消息走国际化
     * <p>
     * 当 BizException 使用 new BizException(ErrorCode) 构造时，
     * getMessage() 返回 ErrorCode.message()（默认消息），此时走国际化翻译。
     * 当使用 new BizException(ErrorCode, "自定义消息") 构造时，直接返回自定义消息。
     */
    private String resolveExceptionMessage(BaseException e, Locale locale) {
        String exceptionMessage = e.getMessage();
        String defaultErrorCodeMessage = e.getErrorCode().message();
        if (exceptionMessage != null && !exceptionMessage.equals(defaultErrorCodeMessage)) {
            return exceptionMessage;
        }
        return resolveMessage(e.getErrorCode().messageKey(), locale);
    }

    /**
     * 通过 MessageSource 解析国际化消息，找不到时回退到 ErrorCode 默认消息
     */
    private String resolveMessage(String key, Locale locale) {
        try {
            return messageSource.getMessage(key, null, locale);
        } catch (NoSuchMessageException ex) {
            return key;
        }
    }
}
