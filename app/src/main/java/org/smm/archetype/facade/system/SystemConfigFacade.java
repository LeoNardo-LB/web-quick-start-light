package org.smm.archetype.facade.system;

import org.smm.archetype.entity.api.BasePageResult;
import org.smm.archetype.entity.system.SystemConfigPageQuery;

import java.util.List;

/**
 * 系统配置 Facade 接口
 * <p>
 * 提供给 Controller 层调用的统一门面，封装 Entity→VO 转换逻辑
 */
public interface SystemConfigFacade {

    /**
     * 获取所有配置分组
     *
     * @return 分组列表
     */
    List<ConfigGroupVO> getAllGroups();

    /**
     * 获取所有配置
     *
     * @return 配置 VO 列表
     */
    List<SystemConfigVO> getAllConfigs();

    /**
     * 按 Key 获取配置
     *
     * @param key 配置键
     * @return 配置 VO
     * @throws org.smm.archetype.exception.BizException 配置不存在时抛出
     */
    SystemConfigVO getConfigByKey(String key);

    /**
     * 按分组获取配置
     *
     * @param groupCode 分组编码
     * @return 配置 VO 列表
     */
    List<SystemConfigVO> getConfigsByGroup(String groupCode);

    /**
     * 更新配置
     *
     * @param command 更新命令
     */
    void updateConfig(UpdateConfigCommand command);

    /**
     * 分页查询系统配置
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    BasePageResult<SystemConfigVO> findByPage(SystemConfigPageQuery query);
}
