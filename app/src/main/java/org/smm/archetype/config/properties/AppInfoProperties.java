package org.smm.archetype.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 应用信息配置属性，替代 WebStartLightApplication 中的 @Value 注入。
 * <p>
 * 注意：本类使用 @Getter @Setter 而非 @Data（遵循 AGENTS.md 规范）。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppInfoProperties {

    /**
     * 服务端口
     */
    private String port = "9201";

    /**
     * 上下文路径
     */
    private String contextPath;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * Swagger UI 路径
     */
    private String openapiUrl = "/openapi-doc.html";

    /**
     * API 文档路径
     */
    private String apiDocUrl = "/v3/api-docs";
}
