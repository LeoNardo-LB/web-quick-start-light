package org.smm.archetype.generated.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
@TableName("operation_log")
public class OperationLogDO extends BaseDO {
    private String traceId;
    private String userId;
    private String module;
    private String operationType;
    private String description;
    private String method;
    private String params;
    private String result;
    private Long executionTime;
    private String ip;
    private String status;
    private String errorMessage;
}
