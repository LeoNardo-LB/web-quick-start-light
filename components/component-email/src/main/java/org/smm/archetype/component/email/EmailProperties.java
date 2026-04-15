package org.smm.archetype.component.email;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "component.email")
public class EmailProperties {
    /** SMTP 主机 */
    private String host;
    /** SMTP 端口 */
    private int port = 587;
    /** 用户名 */
    private String username;
    /** 密码 */
    private String password;
    /** 是否启用 SSL */
    private boolean sslEnable = true;
    /** 发件人地址 */
    private String from;
}
