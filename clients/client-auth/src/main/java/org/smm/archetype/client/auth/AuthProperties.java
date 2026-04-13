package org.smm.archetype.client.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 认证客户端配置属性。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "middleware.auth")
public class AuthProperties {

    /** 是否启用认证（默认 true） */
    private boolean enabled = true;

    /** 不需要认证的路径列表 */
    private List<String> excludePaths = new ArrayList<>();
}
