package org.smm.archetype.facade.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.smm.archetype.entity.api.BasePageResult;
import org.smm.archetype.entity.system.SystemConfig;
import org.smm.archetype.entity.system.SystemConfigPageQuery;
import org.smm.archetype.exception.BizException;
import org.smm.archetype.exception.CommonErrorCode;
import org.smm.archetype.service.system.SystemConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统配置 Facade 实现
 * <p>
 * 封装 Service 调用，负责 Entity→VO 转换
 */
@Service
@RequiredArgsConstructor
public class SystemConfigFacadeImpl implements SystemConfigFacade {

    private final SystemConfigService systemConfigService;

    @Override
    public List<ConfigGroupVO> getAllGroups() {
        return systemConfigService.getAllGroups();
    }

    @Override
    public List<SystemConfigVO> getAllConfigs() {
        return systemConfigService.getAllConfigs().stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public SystemConfigVO getConfigByKey(String key) {
        SystemConfig config = systemConfigService.getConfigByKey(key);
        if (config == null) {
            throw new BizException(CommonErrorCode.FAIL);
        }
        return toVO(config);
    }

    @Override
    public List<SystemConfigVO> getConfigsByGroup(String groupCode) {
        return systemConfigService.getConfigsByGroup(groupCode).stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public void updateConfig(UpdateConfigCommand command) {
        systemConfigService.updateConfig(command);
    }

    @Override
    public BasePageResult<SystemConfigVO> findByPage(SystemConfigPageQuery query) {
        IPage<SystemConfig> entityPage = systemConfigService.findByPage(query);

        // 转换 IPage<SystemConfig> → IPage<SystemConfigVO>
        List<SystemConfigVO> voRecords = entityPage.getRecords().stream()
                .map(this::toVO)
                .toList();
        Page<SystemConfigVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(voRecords);

        return BasePageResult.fromPage(voPage);
    }

    private SystemConfigVO toVO(SystemConfig c) {
        return new SystemConfigVO(
                c.getId(),
                c.getConfigKey() != null ? c.getConfigKey().value() : null,
                c.getConfigValue() != null ? c.getConfigValue().value() : null,
                c.getValueType() != null ? c.getValueType().getCode() : null,
                c.getGroupCode() != null ? c.getGroupCode().getCode() : null,
                c.getDisplayName() != null ? c.getDisplayName().value() : null,
                c.getDescription(),
                c.getInputType() != null ? c.getInputType().getCode() : null,
                c.getInputConfig(),
                c.getSort()
        );
    }
}
