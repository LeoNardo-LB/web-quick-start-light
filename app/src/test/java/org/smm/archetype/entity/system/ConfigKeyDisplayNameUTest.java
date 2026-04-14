package org.smm.archetype.entity.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.UnitTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ConfigKey / DisplayName / SystemConfig 值对象与实体")
class ConfigKeyDisplayNameUTest extends UnitTestBase {

    @Nested
    @DisplayName("ConfigKey 配置键值对象")
    class ConfigKeyTests {

        @Nested
        @DisplayName("构造器校验")
        class ConstructorValidation {

            @Test
            @DisplayName("value=null 应抛出 IllegalArgumentException")
            void should_throw_when_value_is_null() {
                assertThatThrownBy(() -> new ConfigKey(null))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("配置键不能为空");
            }

            @Test
            @DisplayName("value=空字符串 应抛出 IllegalArgumentException")
            void should_throw_when_value_is_blank() {
                assertThatThrownBy(() -> new ConfigKey(""))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("配置键不能为空");
            }

            @Test
            @DisplayName("value=纯空白字符串 应抛出 IllegalArgumentException")
            void should_throw_when_value_is_whitespace_only() {
                assertThatThrownBy(() -> new ConfigKey("   "))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("配置键不能为空");
            }

            @Test
            @DisplayName("合法 value 应正常构造")
            void should_construct_with_valid_value() {
                ConfigKey key = new ConfigKey("sys.enabled");

                assertThat(key.value()).isEqualTo("sys.enabled");
            }
        }

        @Nested
        @DisplayName("of 工厂方法")
        class OfFactory {

            @Test
            @DisplayName("of 应自动 trim 值")
            void should_trim_value() {
                ConfigKey key = ConfigKey.of("  sys.name  ");

                assertThat(key.value()).isEqualTo("sys.name");
            }

            @Test
            @DisplayName("of(null) 应抛出 NullPointerException（trim() 先于构造器校验）")
            void should_throw_when_of_null() {
                assertThatThrownBy(() -> ConfigKey.of(null))
                        .isInstanceOf(NullPointerException.class);
            }

            @Test
            @DisplayName("of(空字符串) 应抛出 IllegalArgumentException")
            void should_throw_when_of_blank() {
                assertThatThrownBy(() -> ConfigKey.of(""))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("配置键不能为空");
            }
        }

        @Nested
        @DisplayName("record equals/hashCode/toString")
        class RecordBehavior {

            @Test
            @DisplayName("相同 value 的两个 ConfigKey 应相等")
            void should_be_equal_with_same_value() {
                ConfigKey k1 = new ConfigKey("a.b");
                ConfigKey k2 = new ConfigKey("a.b");

                assertThat(k1).isEqualTo(k2);
                assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
            }

            @Test
            @DisplayName("不同 value 的两个 ConfigKey 应不相等")
            void should_not_be_equal_with_different_value() {
                ConfigKey k1 = new ConfigKey("a.b");
                ConfigKey k2 = new ConfigKey("c.d");

                assertThat(k1).isNotEqualTo(k2);
            }

            @Test
            @DisplayName("value() 应返回原始值")
            void should_return_value() {
                ConfigKey key = ConfigKey.of("test.key");
                assertThat(key.value()).isEqualTo("test.key");
            }
        }
    }

    @Nested
    @DisplayName("DisplayName 显示名称值对象")
    class DisplayNameTests {

        @Nested
        @DisplayName("构造器校验")
        class ConstructorValidation {

            @Test
            @DisplayName("value=null 应抛出 IllegalArgumentException")
            void should_throw_when_value_is_null() {
                assertThatThrownBy(() -> new org.smm.archetype.entity.system.DisplayName(null))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("显示名称不能为空");
            }

            @Test
            @DisplayName("value=空字符串 应抛出 IllegalArgumentException")
            void should_throw_when_value_is_blank() {
                assertThatThrownBy(() -> new org.smm.archetype.entity.system.DisplayName(""))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("显示名称不能为空");
            }

            @Test
            @DisplayName("value=纯空白字符串 应抛出 IllegalArgumentException")
            void should_throw_when_value_is_whitespace_only() {
                assertThatThrownBy(() -> new org.smm.archetype.entity.system.DisplayName("   "))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("显示名称不能为空");
            }

            @Test
            @DisplayName("合法 value 应正常构造")
            void should_construct_with_valid_value() {
                org.smm.archetype.entity.system.DisplayName dn =
                        new org.smm.archetype.entity.system.DisplayName("系统开关");

                assertThat(dn.value()).isEqualTo("系统开关");
            }
        }

        @Nested
        @DisplayName("of 工厂方法")
        class OfFactory {

            @Test
            @DisplayName("of 应自动 trim 值")
            void should_trim_value() {
                org.smm.archetype.entity.system.DisplayName dn =
                        org.smm.archetype.entity.system.DisplayName.of("  系统名称  ");

                assertThat(dn.value()).isEqualTo("系统名称");
            }

            @Test
            @DisplayName("of(null) 应抛出 NullPointerException（trim() 先于构造器校验）")
            void should_throw_when_of_null() {
                assertThatThrownBy(() -> org.smm.archetype.entity.system.DisplayName.of(null))
                        .isInstanceOf(NullPointerException.class);
            }

            @Test
            @DisplayName("of(空字符串) 应抛出 IllegalArgumentException")
            void should_throw_when_of_blank() {
                assertThatThrownBy(() -> org.smm.archetype.entity.system.DisplayName.of(""))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("显示名称不能为空");
            }
        }

        @Nested
        @DisplayName("record equals/hashCode")
        class RecordBehavior {

            @Test
            @DisplayName("相同 value 的两个 DisplayName 应相等")
            void should_be_equal_with_same_value() {
                org.smm.archetype.entity.system.DisplayName d1 =
                        new org.smm.archetype.entity.system.DisplayName("名称");
                org.smm.archetype.entity.system.DisplayName d2 =
                        new org.smm.archetype.entity.system.DisplayName("名称");

                assertThat(d1).isEqualTo(d2);
                assertThat(d1.hashCode()).isEqualTo(d2.hashCode());
            }

            @Test
            @DisplayName("不同 value 的两个 DisplayName 应不相等")
            void should_not_be_equal_with_different_value() {
                org.smm.archetype.entity.system.DisplayName d1 =
                        new org.smm.archetype.entity.system.DisplayName("名称A");
                org.smm.archetype.entity.system.DisplayName d2 =
                        new org.smm.archetype.entity.system.DisplayName("名称B");

                assertThat(d1).isNotEqualTo(d2);
            }
        }
    }

    @Nested
    @DisplayName("SystemConfig 实体")
    class SystemConfigTests {

        @Nested
        @DisplayName("updateValue 方法")
        class UpdateValue {

            @Test
            @DisplayName("updateValue(null) 应抛出 IllegalArgumentException")
            void should_throw_when_newValue_is_null() {
                SystemConfig config = new SystemConfig();

                assertThatThrownBy(() -> config.updateValue(null))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("配置值不能为空");
            }

            @Test
            @DisplayName("updateValue(合法值) 应正常设置 configValue")
            void should_set_configValue_on_valid_update() {
                SystemConfig config = new SystemConfig();
                ConfigValue newValue = ConfigValue.of("true");

                config.updateValue(newValue);

                assertThat(config.getConfigValue()).isEqualTo(newValue);
            }

            @Test
            @DisplayName("updateValue 应覆盖之前的值")
            void should_overwrite_previous_value() {
                SystemConfig config = new SystemConfig();
                config.updateValue(ConfigValue.of("old"));

                ConfigValue newValue = ConfigValue.of("new");

                config.updateValue(newValue);

                assertThat(config.getConfigValue()).isEqualTo(newValue);
                assertThat(config.getConfigValue().value()).isEqualTo("new");
            }
        }

        @Nested
        @DisplayName("getter/setter")
        class GetterSetter {

            @Test
            @DisplayName("所有字段应正确读写")
            void should_read_write_all_fields() {
                SystemConfig config = new SystemConfig();
                ConfigKey key = ConfigKey.of("test.key");
                ConfigValue value = ConfigValue.of("test-value");
                org.smm.archetype.entity.system.DisplayName displayName =
                        org.smm.archetype.entity.system.DisplayName.of("测试配置");

                config.setId(1L);
                config.setConfigKey(key);
                config.setConfigValue(value);
                config.setDisplayName(displayName);
                config.setDescription("测试描述");
                config.setSort(10);

                assertThat(config.getId()).isEqualTo(1L);
                assertThat(config.getConfigKey()).isEqualTo(key);
                assertThat(config.getConfigValue()).isEqualTo(value);
                assertThat(config.getDisplayName()).isEqualTo(displayName);
                assertThat(config.getDescription()).isEqualTo("测试描述");
                assertThat(config.getSort()).isEqualTo(10);
            }
        }
    }
}
