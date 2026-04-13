package org.smm.archetype.cases.integrationtest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.client.cache.CacheClient;
import org.smm.archetype.client.oss.OssClient;
import org.smm.archetype.client.search.SearchClient;
import org.smm.archetype.support.IntegrationTestBase;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * L3 集成测试 — 验证技术客户端通过 AutoConfiguration 注册为 Spring Bean。
 * <p>
 * 覆盖能力: tech-client-interface
 * <p>
 * 注意：EmailClient 和 SmsClient 在测试环境中不注册（MailSender 被排除，Sms 无真实实现），
 * 这是设计如此——没有真实中间件实现就不注册 Bean，避免静默降级掩盖问题。
 */
class TechClientInterfaceITest extends IntegrationTestBase {

    @Test
    @DisplayName("CacheClient Bean 已注册（CaffeineCacheClient）")
    void should_cacheClientBeanBeAvailable() {
        CacheClient client = applicationContext.getBean(CacheClient.class);
        assertThat(client).isNotNull();
        assertThat(client.getClass().getSimpleName()).contains("Caffeine");

        assertThatNoException().isThrownBy(() -> client.put("key", "value", Duration.ofSeconds(60)));
    }

    @Test
    @DisplayName("SearchClient Bean 已注册（SimpleSearchClient）")
    void should_searchClientBeanBeAvailable() {
        SearchClient client = applicationContext.getBean(SearchClient.class);
        assertThat(client).isNotNull();
        assertThat(client.getClass().getSimpleName()).contains("Simple");
    }

    @Test
    @DisplayName("OssClient Bean 已注册（LocalOssClient）")
    void should_ossClientBeanBeAvailable() {
        OssClient client = applicationContext.getBean(OssClient.class);
        assertThat(client).isNotNull();
        assertThat(client.getClass().getSimpleName()).contains("Local");
    }
}
