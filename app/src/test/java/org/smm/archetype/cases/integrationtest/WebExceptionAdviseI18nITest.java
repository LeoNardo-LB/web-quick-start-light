package org.smm.archetype.cases.integrationtest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.IntegrationTestBase;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

/**
 * WebExceptionAdvise 国际化集成测试
 * <p>
 * 验证基于 Accept-Language 头的错误消息国际化能力
 */
@DisplayName("WebExceptionAdvise 国际化")
class WebExceptionAdviseI18nITest extends IntegrationTestBase {

    @Nested
    @DisplayName("BizException 国际化 — GET /api/system/configs/{nonexist}")
    class BizExceptionI18n {

        @Test
        @DisplayName("MFT: Accept-Language: en → 返回英文错误消息 'Operation failed'")
        void should_returnEnglishMessage_whenAcceptLanguageIsEn() {
            webTestClient.get().uri("/api/system/configs/nonexist-key-i18n-test")
                    .header("Accept-Language", "en")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(2000)
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.message").value(equalTo("Operation failed"));
        }

        @Test
        @DisplayName("MFT: 不携带 Accept-Language → 返回默认中文消息 '操作失败'")
        void should_returnChineseMessage_whenNoAcceptLanguage() {
            webTestClient.get().uri("/api/system/configs/nonexist-key-i18n-test")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(2000)
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.message").value(equalTo("操作失败"));
        }

        @Test
        @DisplayName("DIR: Accept-Language: fr → 回退到默认中文消息")
        void should_fallbackToDefault_whenLocaleNotFound() {
            webTestClient.get().uri("/api/system/configs/nonexist-key-i18n-test")
                    .header("Accept-Language", "fr")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(2000)
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.message").value(equalTo("操作失败"));
        }
    }

    @Nested
    @DisplayName("BizException 自定义消息不受国际化影响 — GET /api/test/exception")
    class BizExceptionCustomMessage {

        @Test
        @DisplayName("INV: 自定义消息 '测试业务异常' 在任何语言下保持不变")
        void should_keepCustomMessage_whenAcceptLanguageIsEn() {
            webTestClient.get().uri("/api/test/exception")
                    .header("Accept-Language", "en")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(2000)
                    .jsonPath("$.message").value(equalTo("测试业务异常"));
        }
    }

    @Nested
    @DisplayName("Bean Validation 校验消息国际化 — GET /api/test/validate")
    class ValidationI18n {

        @Test
        @DisplayName("MFT: Accept-Language: en + 空参数 → 英文校验消息")
        void should_returnEnglishValidationMessage_whenAcceptLanguageIsEn() {
            webTestClient.get().uri("/api/test/validate?name=")
                    .header("Accept-Language", "en")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(2001)
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.message").value(containsString("must not be blank"));
        }

        @Test
        @DisplayName("INV: 不携带 Accept-Language + 空参数 → 中文校验消息")
        void should_returnChineseValidationMessage_whenNoAcceptLanguage() {
            webTestClient.get().uri("/api/test/validate?name=")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(2001)
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.message").value(containsString("不能为空"));
        }
    }

    @Nested
    @DisplayName("ClientException 处理 — GET /api/test/client-exception")
    class ClientExceptionHandling {

        @Test
        @DisplayName("MFT: Accept-Language: en → 英文消息 'External service call failed'")
        void should_returnEnglishMessage_whenAcceptLanguageIsEn() {
            webTestClient.get().uri("/api/test/client-exception")
                    .header("Accept-Language", "en")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(2002)
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.message").value(equalTo("External service call failed"));
        }

        @Test
        @DisplayName("INV: 不携带 Accept-Language → 中文消息 '外部服务调用失败'")
        void should_returnChineseMessage_whenNoAcceptLanguage() {
            webTestClient.get().uri("/api/test/client-exception")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(2002)
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.message").value(equalTo("外部服务调用失败"));
        }
    }

    @Nested
    @DisplayName("SysException 处理 — GET /api/test/sys-exception")
    class SysExceptionHandling {

        @Test
        @DisplayName("MFT: Accept-Language: en → 英文消息 'System error'")
        void should_returnEnglishMessage_whenAcceptLanguageIsEn() {
            webTestClient.get().uri("/api/test/sys-exception")
                    .header("Accept-Language", "en")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(5000)
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.message").value(equalTo("System error"));
        }

        @Test
        @DisplayName("INV: 不携带 Accept-Language → 中文消息 '系统异常'")
        void should_returnChineseMessage_whenNoAcceptLanguage() {
            webTestClient.get().uri("/api/test/sys-exception")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(5000)
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.message").value(equalTo("系统异常"));
        }
    }

    @Nested
    @DisplayName("NoResourceFoundException 处理 — GET /api/nonexistent-path")
    class NoResourceFoundExceptionHandling {

        @Test
        @DisplayName("BVA: 访问不存在的路径 → 返回 404 + FAIL 错误码")
        void should_return404_whenResourceNotFound() {
            webTestClient.get().uri("/api/auth/nonexistent-path-for-test")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(2000)
                    .jsonPath("$.success").isEqualTo(false);
        }
    }

    @Nested
    @DisplayName("NotLoginException 处理 — GET /api/test/not-login")
    class NotLoginExceptionHandling {

        @Test
        @DisplayName("MFT: Accept-Language: en → 英文消息 'Not logged in or session expired'")
        void should_returnEnglishMessage_whenAcceptLanguageIsEn() {
            webTestClient.get().uri("/api/test/not-login")
                    .header("Accept-Language", "en")
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(6601)
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.message").value(equalTo("Not logged in or session expired"));
        }

        @Test
        @DisplayName("INV: 不携带 Accept-Language → 中文消息 '未登录或登录已过期'")
        void should_returnChineseMessage_whenNoAcceptLanguage() {
            webTestClient.get().uri("/api/test/not-login")
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(6601)
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.message").value(equalTo("未登录或登录已过期"));
        }
    }

    @Nested
    @DisplayName("通用 Exception 处理 — GET /api/test/generic-exception")
    class GenericExceptionHandling {

        @Test
        @DisplayName("BVA: 抛出 RuntimeException → 返回 UNKNOWN_ERROR 错误码 + 原始消息")
        void should_returnUnknownError_whenGenericExceptionThrown() {
            webTestClient.get().uri("/api/test/generic-exception")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(9999)
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.message").value(equalTo("测试通用异常"));
        }
    }

    @Nested
    @DisplayName("BindException 处理 — POST /api/test/bind-test")
    class BindExceptionHandling {

        @Test
        @DisplayName("MFT: Accept-Language: en + 空表单字段 → 英文校验消息")
        void should_returnEnglishBindMessage_whenAcceptLanguageIsEn() {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("name", "");
            webTestClient.post().uri("/api/test/bind-test")
                    .header("Accept-Language", "en")
                    .bodyValue(formData)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(2001)
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.message").value(containsString("must not be blank"));
        }

        @Test
        @DisplayName("INV: 不携带 Accept-Language + 空表单字段 → 中文校验消息")
        void should_returnChineseBindMessage_whenNoAcceptLanguage() {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("name", "");
            webTestClient.post().uri("/api/test/bind-test")
                    .bodyValue(formData)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(2001)
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.message").value(containsString("不能为空"));
        }
    }
}
