package org.smm.archetype.repository.system;

import org.smm.archetype.entity.system.ConfigGroup;
import org.smm.archetype.entity.system.ConfigKey;
import org.smm.archetype.entity.system.SystemConfig;
import org.smm.archetype.generated.entity.SystemConfigDO;
import org.smm.archetype.generated.mapper.SystemConfigMapper;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.smm.archetype.entity.system.ConfigGroup;
import org.smm.archetype.entity.system.ConfigKey;
import org.smm.archetype.entity.system.SystemConfig;
import org.smm.archetype.entity.system.SystemConfigPageQuery;
import org.smm.archetype.generated.entity.SystemConfigDO;
import org.smm.archetype.generated.mapper.SystemConfigMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置仓储实现
 */
@Repository
@RequiredArgsConstructor
public class SystemConfigRepositoryImpl implements SystemConfigRepository {

    private final SystemConfigMapper systemConfigMapper;
    private final SystemConfigConverter converter;

    @Override
    public Optional<SystemConfig> findByConfigKey(ConfigKey configKey) {
        LambdaQueryWrapper<SystemConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfigDO::getConfigKey, configKey.value());
        SystemConfigDO configDO = systemConfigMapper.selectOne(wrapper);
        return Optional.ofNullable(converter.toEntity(configDO));
    }

    @Override
    public List<SystemConfig> findByGroupCode(ConfigGroup groupCode) {
        LambdaQueryWrapper<SystemConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfigDO::getGroupCode, groupCode.getCode());
        wrapper.orderByAsc(SystemConfigDO::getSort);
        List<SystemConfigDO> configDOs = systemConfigMapper.selectList(wrapper);
        return configDOs.stream().map(converter::toEntity).toList();
    }

    @Override
    public List<SystemConfig> findAll() {
        LambdaQueryWrapper<SystemConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SystemConfigDO::getGroupCode, SystemConfigDO::getSort);
        List<SystemConfigDO> configDOs = systemConfigMapper.selectList(wrapper);
        return configDOs.stream().map(converter::toEntity).toList();
    }

    @Override
    public SystemConfig save(SystemConfig config) {
        SystemConfigDO configDO = converter.toDataObject(config);
        if (configDO.getId() == null) {
            systemConfigMapper.insert(configDO);
            config.setId(configDO.getId());
        } else {
            systemConfigMapper.updateById(configDO);
        }
        return config;
    }

    @Override
    public IPage<SystemConfig> findByPage(SystemConfigPageQuery query) {
        Page<SystemConfigDO> page = new Page<>(query.pageNo(), query.pageSize());
        LambdaQueryWrapper<SystemConfigDO> wrapper = new LambdaQueryWrapper<>();
        if (query.groupCode() != null && !query.groupCode().isBlank()) {
            wrapper.eq(SystemConfigDO::getGroupCode, query.groupCode());
        }
        wrapper.orderByAsc(SystemConfigDO::getGroupCode, SystemConfigDO::getSort);
        IPage<SystemConfigDO> doPage = systemConfigMapper.selectPage(page, wrapper);

        // 转换 IPage<DO> → IPage<Entity>
        List<SystemConfig> entities = doPage.getRecords().stream()
                .map(converter::toEntity).toList();
        Page<SystemConfig> entityPage = new Page<>(doPage.getCurrent(), doPage.getSize(), doPage.getTotal());
        entityPage.setRecords(entities);
        return entityPage;
    }
}
