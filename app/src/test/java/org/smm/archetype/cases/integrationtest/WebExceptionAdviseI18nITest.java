package org.smm.archetype.cases.integrationtest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.IntegrationTestBase;

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
}
