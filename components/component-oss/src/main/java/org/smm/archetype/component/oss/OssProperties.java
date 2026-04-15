package org.smm.archetype.component.oss;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "component.oss")
public class OssProperties {
    /** 存储类型：local（本地文件系统） */
    private String type = "local";
    /** 本地存储路径 */
    private String localStoragePath = "./uploads";
}
