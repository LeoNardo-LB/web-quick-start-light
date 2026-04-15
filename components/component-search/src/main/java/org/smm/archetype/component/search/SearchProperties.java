package org.smm.archetype.component.search;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "component.search")
public class SearchProperties {
    /** 默认每页大小 */
    private int defaultPageSize = 10;
    /** 最大每页大小 */
    private int maxPageSize = 100;
}
