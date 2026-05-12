package org.smm.archetype.systemconfig.internal;

import org.smm.archetype.shared.pagination.PageResult;

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
     * @return 分页结果（框架无关）
     */
    PageResult<SystemConfig> findByPage(SystemConfigPageQuery query);
}
