package org.smm.archetype.repository.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.smm.archetype.entity.system.ConfigGroup;
import org.smm.archetype.entity.system.ConfigKey;
import org.smm.archetype.entity.system.SystemConfig;
import org.smm.archetype.entity.system.SystemConfigPageQuery;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置仓储接口
 */
public interface SystemConfigRepository {

    Optional<SystemConfig> findByConfigKey(ConfigKey configKey);

    List<SystemConfig> findByGroupCode(ConfigGroup groupCode);

    List<SystemConfig> findAll();

    SystemConfig save(SystemConfig config);

    /**
     * 分页查询系统配置
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    IPage<SystemConfig> findByPage(SystemConfigPageQuery query);
}
