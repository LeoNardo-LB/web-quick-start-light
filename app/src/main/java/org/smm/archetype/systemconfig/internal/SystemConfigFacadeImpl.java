package org.smm.archetype.systemconfig.internal;

import lombok.RequiredArgsConstructor;
import org.smm.archetype.exception.BizException;
import org.smm.archetype.exception.CommonErrorCode;
import org.smm.archetype.shared.pagination.PageResult;
import org.smm.archetype.shared.result.BasePageResult;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统配置 Facade 实现
 * <p>
 * 封装 Service 调用，负责 Model→VO 转换
 */
@Service
@RequiredArgsConstructor
public class SystemConfigFacadeImpl implements org.smm.archetype.systemconfig.SystemConfigFacade {

    private final SystemConfigService systemConfigService;

    @Override
    public List<ConfigGroupVO> getAllGroups() {
        // 修复：Service 返回 List<ConfigGroup>（枚举），Facade 负责转换为 VO
        return systemConfigService.getAllGroups().stream()
                .map(g -> new ConfigGroupVO(g.getCode(), g.getDisplayName(), g.getIcon(), g.getColor()))
                .toList();
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
        // 使用 PageResult 替代 IPage
        PageResult<SystemConfig> pageResult = systemConfigService.findByPage(query);

        List<SystemConfigVO> voList = pageResult.list().stream()
                .map(this::toVO)
                .toList();
        PageResult<SystemConfigVO> voPageResult = new PageResult<>(
                voList, pageResult.total(), pageResult.pageNo(), pageResult.pageSize(), pageResult.totalPages());

        return BasePageResult.from(voPageResult);
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
