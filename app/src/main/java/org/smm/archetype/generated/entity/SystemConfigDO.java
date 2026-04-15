package org.smm.archetype.generated.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.smm.archetype.entity.base.BaseDO;

/**
 * 此文件由代码生成器生成，禁止手动修改。
 * 如需重新生成，运行 MybatisPlusGenerator.main()
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("system_config")
public class SystemConfigDO extends BaseDO {
    private String configKey;
    private String configValue;
    private String valueType;
    private String groupCode;
    private String displayName;
    private String description;
    private String inputType;
    private String inputConfig;
    private Integer sort;
}
