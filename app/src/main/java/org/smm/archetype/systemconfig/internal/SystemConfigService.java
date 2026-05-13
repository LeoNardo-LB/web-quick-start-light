package org.smm.archetype.systemconfig.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.shared.event.DomainEventPublisher;
import org.smm.archetype.shared.pagination.PageResult;
import org.smm.archetype.systemconfig.ConfigChangedEvent;
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
    private final DomainEventPublisher eventPublisher;

    /**
     * 获取所有配置分组（返回枚举值，由 Facade 层转换为 VO）
     */
    @Transactional(readOnly = true)
    public List<ConfigGroup> getAllGroups() {
        return Arrays.stream(ConfigGroup.values()).toList();
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
        String oldValue = config.getConfigValue() != null ? config.getConfigValue().value() : null;
        config.updateValue(ConfigValue.of(command.configValue()));
        systemConfigRepository.save(config);
        log.info("Config updated: {}", command.configKey());

        // 发布配置变更事件
        eventPublisher.publish(ConfigChangedEvent.of(command.configKey(), oldValue, command.configValue()));
    }

    /**
     * 分页查询系统配置
     *
     * @param query 分页查询参数
     * @return 分页结果（框架无关）
     */
    @Transactional(readOnly = true)
    public PageResult<SystemConfig> findByPage(SystemConfigPageQuery query) {
        return systemConfigRepository.findByPage(query);
    }
}
