package org.smm.archetype.auth.internal;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

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
