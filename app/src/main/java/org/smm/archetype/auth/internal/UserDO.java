package org.smm.archetype.auth.internal;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import org.smm.archetype.entity.base.BaseDO;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("user")
class UserDO extends BaseDO {
    private String username;
    private String passwordHash;
    private String nickname;
    private String status;
}
