package org.smm.archetype.shared.util.dal;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.smm.archetype.shared.util.context.ScopedThreadContext;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        String userId = ScopedThreadContext.getUserId();
        if (userId == null)
            userId = "system";

        this.strictInsertFill(metaObject, "createTime", Instant.class, Instant.now());
        this.strictInsertFill(metaObject, "updateTime", Instant.class, Instant.now());
        this.strictInsertFill(metaObject, "createUser", String.class, userId);
        this.strictInsertFill(metaObject, "updateUser", String.class, userId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        String userId = ScopedThreadContext.getUserId();
        if (userId == null)
            userId = "system";

        this.strictUpdateFill(metaObject, "updateTime", Instant.class, Instant.now());
        this.strictUpdateFill(metaObject, "updateUser", String.class, userId);
    }

}
