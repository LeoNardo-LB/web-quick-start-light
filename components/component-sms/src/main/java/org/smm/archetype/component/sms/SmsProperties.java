package org.smm.archetype.component.sms;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "component.sms")
public class SmsProperties {
    /** 服务商 */
    private String provider;
    /** AccessKey ID */
    private String accessKeyId;
    /** AccessKey Secret */
    private String accessKeySecret;
    /** 短信签名 */
    private String signName;
}
