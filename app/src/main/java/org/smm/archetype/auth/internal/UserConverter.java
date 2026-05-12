package org.smm.archetype.auth.internal;

class UserConverter {

    User toModel(UserDO userDO) {
        if (userDO == null) return null;
        User user = new User();
        user.setId(userDO.getId());
        user.setUsername(userDO.getUsername());
        user.setPasswordHash(userDO.getPasswordHash());
        user.setNickname(userDO.getNickname());
        user.setStatus(userDO.getStatus());
        user.setCreateTime(userDO.getCreateTime());
        user.setUpdateTime(userDO.getUpdateTime());
        return user;
    }

    UserDO toDataObject(User user) {
        if (user == null) return null;
        UserDO userDO = UserDO.builder()
                .username(user.getUsername())
                .passwordHash(user.getPasswordHash())
                .nickname(user.getNickname())
                .status(user.getStatus())
                .build();
        userDO.setId(user.getId());
        return userDO;
    }
}
