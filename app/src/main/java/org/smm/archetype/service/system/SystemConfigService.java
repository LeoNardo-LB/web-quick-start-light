package org.smm.archetype.service.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.entity.system.ConfigGroup;
import org.smm.archetype.entity.system.ConfigKey;
import org.smm.archetype.entity.system.ConfigValue;
import org.smm.archetype.entity.system.SystemConfig;
import org.smm.archetype.entity.system.SystemConfigPageQuery;
import org.smm.archetype.facade.system.ConfigGroupVO;
import org.smm.archetype.facade.system.UpdateConfigCommand;
import org.smm.archetype.repository.system.SystemConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 系统配置服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    @Transactional(readOnly = true)
    public List<ConfigGroupVO> getAllGroups() {
        return Arrays.stream(ConfigGroup.values())
                .map(g -> new ConfigGroupVO(g.getCode(), g.getDisplayName(), g.getIcon(), g.getColor()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SystemConfig> getConfigsByGroup(String groupCode) {
        ConfigGroup group = ConfigGroup.fromCode(groupCode);
        if (group == null) {
            throw new IllegalArgumentException("Invalid group: " + groupCode);
        }
        return systemConfigRepository.findByGroupCode(group);
    }

    @Transactional(readOnly = true)
    public List<SystemConfig> getAllConfigs() {
        return systemConfigRepository.findAll();
    }

    @Transactional(readOnly = true)
    public SystemConfig getConfigByKey(String key) {
        return systemConfigRepository.findByConfigKey(ConfigKey.of(key))
                .orElse(null);
    }

    @Transactional
    public void updateConfig(UpdateConfigCommand command) {
        log.info("Updating config: {}", command.configKey());
        SystemConfig config = systemConfigRepository.findByConfigKey(ConfigKey.of(command.configKey()))
                .orElseThrow(() -> new IllegalArgumentException("Config not found: " + command.configKey()));
        config.updateValue(ConfigValue.of(command.configValue()));
        systemConfigRepository.save(config);
        log.info("Config updated: {}", command.configKey());
    }

    @Transactional(readOnly = true)
    public IPage<SystemConfig> findByPage(SystemConfigPageQuery query) {
        return systemConfigRepository.findByPage(query);
    }
}
