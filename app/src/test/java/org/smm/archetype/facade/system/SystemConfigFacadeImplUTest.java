package org.smm.archetype.facade.system;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.smm.archetype.entity.base.BasePageResult;
import org.smm.archetype.entity.system.ConfigGroup;
import org.smm.archetype.entity.system.ConfigKey;
import org.smm.archetype.entity.system.ConfigValue;
import org.smm.archetype.entity.system.InputType;
import org.smm.archetype.entity.system.SystemConfig;
import org.smm.archetype.entity.system.SystemConfigPageQuery;
import org.smm.archetype.entity.system.ValueType;
import org.smm.archetype.exception.BizException;
import org.smm.archetype.service.system.SystemConfigService;
import org.smm.archetype.support.UnitTestBase;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("SystemConfigFacadeImpl - Facade 层单元测试")
class SystemConfigFacadeImplUTest extends UnitTestBase {

    @Mock
    private SystemConfigService systemConfigService;

    @InjectMocks
    private SystemConfigFacadeImpl facade;

    // ──────────────────────────────────────────────
    // 辅助方法：构建完整的 SystemConfig 实体
    // ──────────────────────────────────────────────

    private SystemConfig buildFullConfig() {
        SystemConfig config = new SystemConfig();
        config.setId(1L);
        config.setConfigKey(ConfigKey.of("site.name"));
        config.setConfigValue(ConfigValue.of("MyApp"));
        config.setValueType(ValueType.STRING);
        config.setGroupCode(ConfigGroup.BASIC);
        config.setDisplayName(org.smm.archetype.entity.system.DisplayName.of("站点名称"));
        config.setDescription("站点显示名称");
        config.setInputType(InputType.TEXT);
        config.setInputConfig("{\"placeholder\":\"请输入站点名称\"}");
        config.setSort(1);
        return config;
    }

    private SystemConfig buildPartialNullConfig() {
        SystemConfig config = new SystemConfig();
        config.setId(2L);
        // configKey, configValue, valueType, groupCode, displayName, inputType 全为 null
        config.setDescription("partial config");
        config.setSort(2);
        return config;
    }

    // ──────────────────────────────────────────────
    // getAllGroups
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("getAllGroups")
    class GetAllGroups {

        @Test
        @DisplayName("应委托 Service 返回分组列表")
        void should_delegate_to_service() {
            // given
            List<ConfigGroupVO> expected = List.of(
                    new ConfigGroupVO("BASIC", "基础配置", "SettingOutlined", "#1890ff")
            );
            when(systemConfigService.getAllGroups()).thenReturn(expected);

            // when
            List<ConfigGroupVO> result = facade.getAllGroups();

            // then
            assertThat(result).isEqualTo(expected);
            verify(systemConfigService).getAllGroups();
        }
    }

    // ──────────────────────────────────────────────
    // getAllConfigs
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("getAllConfigs")
    class GetAllConfigs {

        @Test
        @DisplayName("应将 Entity 列表转换为 VO 列表")
        void should_convert_entities_to_vos() {
            // given
            SystemConfig config = buildFullConfig();
            when(systemConfigService.getAllConfigs()).thenReturn(List.of(config));

            // when
            List<SystemConfigVO> result = facade.getAllConfigs();

            // then
            assertThat(result).hasSize(1);
            SystemConfigVO vo = result.getFirst();
            assertThat(vo.id()).isEqualTo(1L);
            assertThat(vo.configKey()).isEqualTo("site.name");
            assertThat(vo.configValue()).isEqualTo("MyApp");
            assertThat(vo.valueType()).isEqualTo("STRING");
            assertThat(vo.groupCode()).isEqualTo("BASIC");
            assertThat(vo.displayName()).isEqualTo("站点名称");
            assertThat(vo.inputType()).isEqualTo("TEXT");
            assertThat(vo.sort()).isEqualTo(1);
        }

        @Test
        @DisplayName("空列表应返回空 VO 列表")
        void should_return_empty_for_empty_list() {
            // given
            when(systemConfigService.getAllConfigs()).thenReturn(Collections.emptyList());

            // when
            List<SystemConfigVO> result = facade.getAllConfigs();

            // then
            assertThat(result).isEmpty();
        }
    }

    // ──────────────────────────────────────────────
    // getConfigByKey
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("getConfigByKey")
    class GetConfigByKey {

        @Test
        @DisplayName("存在的 Key 应返回对应 VO")
        void should_return_vo_for_existing_key() {
            // given
            SystemConfig config = buildFullConfig();
            when(systemConfigService.getConfigByKey("site.name")).thenReturn(config);

            // when
            SystemConfigVO result = facade.getConfigByKey("site.name");

            // then
            assertThat(result.configKey()).isEqualTo("site.name");
            assertThat(result.configValue()).isEqualTo("MyApp");
        }

        @Test
        @DisplayName("不存在的 Key 应抛出 BizException")
        void should_throw_for_nonexistent_key() {
            // given
            when(systemConfigService.getConfigByKey("not.exist")).thenReturn(null);

            // when / then
            assertThatThrownBy(() -> facade.getConfigByKey("not.exist"))
                    .isInstanceOf(BizException.class);
        }
    }

    // ──────────────────────────────────────────────
    // getConfigsByGroup
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("getConfigsByGroup")
    class GetConfigsByGroup {

        @Test
        @DisplayName("应按分组返回 VO 列表")
        void should_return_vos_by_group() {
            // given
            SystemConfig config = buildFullConfig();
            when(systemConfigService.getConfigsByGroup("BASIC")).thenReturn(List.of(config));

            // when
            List<SystemConfigVO> result = facade.getConfigsByGroup("BASIC");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().groupCode()).isEqualTo("BASIC");
        }
    }

    // ──────────────────────────────────────────────
    // updateConfig
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("updateConfig")
    class UpdateConfig {

        @Test
        @DisplayName("应委托 Service 执行更新")
        void should_delegate_update_to_service() {
            // given
            UpdateConfigCommand command = new UpdateConfigCommand("site.name", "NewName");

            // when
            facade.updateConfig(command);

            // then
            verify(systemConfigService).updateConfig(command);
        }
    }

    // ──────────────────────────────────────────────
    // findByPage
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("findByPage")
    class FindByPage {

        @Test
        @DisplayName("应将 IPage<Entity> 转换为 BasePageResult<VO>")
        void should_convert_page_to_result() {
            // given
            SystemConfig config = buildFullConfig();
            Page<SystemConfig> entityPage = new Page<>(1, 10, 1);
            entityPage.setRecords(List.of(config));
            when(systemConfigService.findByPage(any())).thenReturn(entityPage);

            SystemConfigPageQuery query = new SystemConfigPageQuery(1, 10, null);

            // when
            BasePageResult<SystemConfigVO> result = facade.findByPage(query);

            // then
            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getPageNo()).isEqualTo(1);
            assertThat(result.getPageSize()).isEqualTo(10);
            assertThat(result.getData()).hasSize(1);
            assertThat(result.getData().getFirst().configKey()).isEqualTo("site.name");
        }

        @Test
        @DisplayName("空页应返回空数据列表")
        void should_return_empty_data_for_empty_page() {
            // given
            Page<SystemConfig> entityPage = new Page<>(1, 10, 0);
            entityPage.setRecords(Collections.emptyList());
            when(systemConfigService.findByPage(any())).thenReturn(entityPage);

            SystemConfigPageQuery query = new SystemConfigPageQuery(1, 10, null);

            // when
            BasePageResult<SystemConfigVO> result = facade.findByPage(query);

            // then
            assertThat(result.getTotal()).isZero();
            assertThat(result.getData()).isEmpty();
        }
    }

    // ──────────────────────────────────────────────
    // toVO 边界条件（null 值对象字段）
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("toVO 边界条件")
    class ToVOEdgeCases {

        @Test
        @DisplayName("Entity 值对象字段全为 null 时应安全转换")
        void should_handle_null_value_objects() {
            // given — buildPartialNullConfig 的 configKey/configValue/valueType/groupCode/displayName/inputType 全为 null
            SystemConfig config = buildPartialNullConfig();
            when(systemConfigService.getAllConfigs()).thenReturn(List.of(config));

            // when
            List<SystemConfigVO> result = facade.getAllConfigs();

            // then
            assertThat(result).hasSize(1);
            SystemConfigVO vo = result.getFirst();
            assertThat(vo.configKey()).isNull();
            assertThat(vo.configValue()).isNull();
            assertThat(vo.valueType()).isNull();
            assertThat(vo.groupCode()).isNull();
            assertThat(vo.displayName()).isNull();
            assertThat(vo.inputType()).isNull();
            assertThat(vo.description()).isEqualTo("partial config");
        }
    }
}
