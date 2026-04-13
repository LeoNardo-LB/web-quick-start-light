package org.smm.archetype.entity.user;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 用户实体。
 */
@Getter
@Setter
public class User {

    private Long id;

    private String username;

    private String passwordHash;

    private String nickname;

    private String status;

    private Instant createTime;

    private Instant updateTime;
}
