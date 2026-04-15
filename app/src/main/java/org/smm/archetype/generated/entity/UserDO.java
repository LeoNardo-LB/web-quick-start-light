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
@TableName("user")
public class UserDO extends BaseDO {
    private String username;
    private String passwordHash;
    private String nickname;
    private String status;
}
