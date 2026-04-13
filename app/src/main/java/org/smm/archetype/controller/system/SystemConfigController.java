package org.smm.archetype.controller.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.entity.api.BasePageResult;
import org.smm.archetype.entity.api.BaseResult;
import org.smm.archetype.entity.system.SystemConfigPageQuery;
import org.smm.archetype.facade.system.ConfigGroupVO;
import org.smm.archetype.facade.system.SystemConfigFacade;
import org.smm.archetype.facade.system.SystemConfigVO;
import org.smm.archetype.facade.system.UpdateConfigCommand;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统配置控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/system/configs")
@RequiredArgsConstructor
@Tag(name = "系统配置")
@Validated
public class SystemConfigController {

    private final SystemConfigFacade systemConfigFacade;

    @Operation(summary = "获取所有配置")
    @GetMapping
    public BaseResult<List<SystemConfigVO>> getAllConfigs() {
        return BaseResult.success(systemConfigFacade.getAllConfigs());
    }

    @Operation(summary = "获取配置分组")
    @GetMapping("/groups")
    public BaseResult<List<ConfigGroupVO>> getAllGroups() {
        return BaseResult.success(systemConfigFacade.getAllGroups());
    }

    @Operation(summary = "分页查询系统配置")
    @GetMapping("/page")
    public BasePageResult<SystemConfigVO> findByPage(@Valid @ModelAttribute SystemConfigPageQuery query) {
        return systemConfigFacade.findByPage(query);
    }

    @Operation(summary = "按 Key 获取配置")
    @GetMapping("/{key}")
    public BaseResult<SystemConfigVO> getConfigByKey(@PathVariable String key) {
        return BaseResult.success(systemConfigFacade.getConfigByKey(key));
    }

    @Operation(summary = "按分组获取配置")
    @GetMapping("/group/{code}")
    public BaseResult<List<SystemConfigVO>> getConfigsByGroup(@PathVariable String code) {
        return BaseResult.success(systemConfigFacade.getConfigsByGroup(code));
    }

    @Operation(summary = "更新配置")
    @PutMapping("/{key}")
    public BaseResult<SystemConfigVO> updateConfig(
            @PathVariable String key,
            @Valid @RequestBody UpdateConfigRequest request) {
        systemConfigFacade.updateConfig(new UpdateConfigCommand(key, request.configValue()));
        return BaseResult.success(systemConfigFacade.getConfigByKey(key));
    }
}
