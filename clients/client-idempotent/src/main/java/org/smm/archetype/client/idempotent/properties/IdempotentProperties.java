package org.smm.archetype.client.idempotent.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 幂等防护配置属性。
 * <p>
 * 前缀：middleware.idempotent
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "middleware.idempotent")
public class IdempotentProperties {

    /**
     * 是否启用幂等防护，默认 false（需显式开启）
     */
    private boolean enabled = false;
}
