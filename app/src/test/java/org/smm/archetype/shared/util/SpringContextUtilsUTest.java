package org.smm.archetype.shared.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.*;

/**
 * {@link SpringContextUtils} 单元测试
 *
 * @author Leonardo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Spring 上下文工具类 - SpringContextUtils")
class SpringContextUtilsUTest {

    @Mock
    private ApplicationContext mockContext;

    @BeforeEach
    void setUp() {
        // 确保测试前 context 已设置
        SpringContextUtils.context = mockContext;
    }

    @AfterEach
    void tearDown() {
        // 清理静态状态，避免影响其他测试
        SpringContextUtils.context = null;
    }

    // ================================================================
    // getBean
    // ================================================================

    @Nested
    @DisplayName("getBean - 获取 Bean 实例")
    class GetBean {

        @Test
        @DisplayName("getBean(name)：按名称获取 Bean，正确委托给 ApplicationContext")
        void should_delegate_getBean_by_name() {
            // arrange
            Object expectedBean = new Object();
            when(mockContext.getBean("myService")).thenReturn(expectedBean);

            // act
            Object result = SpringContextUtils.getBean("myService");

            // assert
            assertThat(result).isSameAs(expectedBean);
            verify(mockContext).getBean("myService");
        }

        @Test
        @DisplayName("getBean(Class)：按类型获取 Bean，正确委托给 ApplicationContext")
        void should_delegate_getBean_by_type() {
            // arrange
            Runnable expectedBean = mock(Runnable.class);
            when(mockContext.getBean(Runnable.class)).thenReturn(expectedBean);

            // act
            Runnable result = SpringContextUtils.getBean(Runnable.class);

            // assert
            assertThat(result).isSameAs(expectedBean);
            verify(mockContext).getBean(Runnable.class);
        }

        @Test
        @DisplayName("getBean(name, Class)：按名称和类型获取 Bean，正确委托给 ApplicationContext")
        void should_delegate_getBean_by_name_and_type() {
            // arrange
            String expectedBean = "testBean";
            when(mockContext.getBean("myBean", String.class)).thenReturn(expectedBean);

            // act
            String result = SpringContextUtils.getBean("myBean", String.class);

            // assert
            assertThat(result).isEqualTo(expectedBean);
            verify(mockContext).getBean("myBean", String.class);
        }
    }

    // ================================================================
    // containsBean
    // ================================================================

    @Nested
    @DisplayName("containsBean - 判断 Bean 是否存在")
    class ContainsBean {

        @Test
        @DisplayName("containsBean：返回 true，表示 Bean 存在")
        void should_return_true_when_bean_exists() {
            // arrange
            when(mockContext.containsBean("existingBean")).thenReturn(true);

            // act
            boolean result = SpringContextUtils.containsBean("existingBean");

            // assert
            assertThat(result).isTrue();
            verify(mockContext).containsBean("existingBean");
        }

        @Test
        @DisplayName("containsBean：返回 false，表示 Bean 不存在")
        void should_return_false_when_bean_not_exists() {
            // arrange
            when(mockContext.containsBean("missingBean")).thenReturn(false);

            // act
            boolean result = SpringContextUtils.containsBean("missingBean");

            // assert
            assertThat(result).isFalse();
            verify(mockContext).containsBean("missingBean");
        }
    }

    // ================================================================
    // isSingleton
    // ================================================================

    @Nested
    @DisplayName("isSingleton - 判断 Bean 是否为单例")
    class IsSingleton {

        @Test
        @DisplayName("isSingleton：返回 true，表示单例 Bean")
        void should_return_true_when_singleton() {
            // arrange
            when(mockContext.isSingleton("singletonBean")).thenReturn(true);

            // act
            boolean result = SpringContextUtils.isSingleton("singletonBean");

            // assert
            assertThat(result).isTrue();
            verify(mockContext).isSingleton("singletonBean");
        }

        @Test
        @DisplayName("isSingleton：返回 false，表示非单例 Bean")
        void should_return_false_when_not_singleton() {
            // arrange
            when(mockContext.isSingleton("prototypeBean")).thenReturn(false);

            // act
            boolean result = SpringContextUtils.isSingleton("prototypeBean");

            // assert
            assertThat(result).isFalse();
            verify(mockContext).isSingleton("prototypeBean");
        }
    }

    // ================================================================
    // getType
    // ================================================================

    @Nested
    @DisplayName("getType - 获取 Bean 类型")
    class GetType {

        @Test
        @DisplayName("getType：返回 Bean 的实际类型")
        void should_return_bean_type() {
            // arrange
            when(mockContext.getType("myService")).thenReturn((Class) String.class);

            // act
            Class<?> result = SpringContextUtils.getType("myService");

            // assert
            assertThat(result).isEqualTo(String.class);
            verify(mockContext).getType("myService");
        }

        @Test
        @DisplayName("getType：Bean 不存在时返回 null")
        void should_return_null_when_type_not_found() {
            // arrange
            when(mockContext.getType("unknownBean")).thenReturn(null);

            // act
            Class<?> result = SpringContextUtils.getType("unknownBean");

            // assert
            assertThat(result).isNull();
            verify(mockContext).getType("unknownBean");
        }
    }

    // ================================================================
    // setApplicationContext
    // ================================================================

    @Nested
    @DisplayName("setApplicationContext - 设置 Spring 上下文")
    class SetApplicationContext {

        @Test
        @DisplayName("setApplicationContext：正确设置静态 context 字段")
        void should_set_context_field() {
            // arrange
            SpringContextUtils utils = new SpringContextUtils();
            ApplicationContext anotherContext = mock(ApplicationContext.class);

            // act
            utils.setApplicationContext(anotherContext);

            // assert
            assertThat(SpringContextUtils.context).isSameAs(anotherContext);
        }
    }

    // ================================================================
    // context 未设置
    // ================================================================

    @Nested
    @DisplayName("context 未设置时的行为")
    class ContextNotSet {

        @Test
        @DisplayName("context 为 null 时 getBean(name) 抛出 NullPointerException")
        void should_throw_npe_when_context_null() {
            // arrange
            SpringContextUtils.context = null;

            // act & assert
            assertThatNullPointerException()
                    .isThrownBy(() -> SpringContextUtils.getBean("anyBean"));
        }

        @Test
        @DisplayName("context 为 null 时 containsBean 抛出 NullPointerException")
        void should_throw_npe_when_context_null_containsBean() {
            // arrange
            SpringContextUtils.context = null;

            // act & assert
            assertThatNullPointerException()
                    .isThrownBy(() -> SpringContextUtils.containsBean("anyBean"));
        }

        @Test
        @DisplayName("context 为 null 时 isSingleton 抛出 NullPointerException")
        void should_throw_npe_when_context_null_isSingleton() {
            // arrange
            SpringContextUtils.context = null;

            // act & assert
            assertThatNullPointerException()
                    .isThrownBy(() -> SpringContextUtils.isSingleton("anyBean"));
        }

        @Test
        @DisplayName("context 为 null 时 getType 抛出 NullPointerException")
        void should_throw_npe_when_context_null_getType() {
            // arrange
            SpringContextUtils.context = null;

            // act & assert
            assertThatNullPointerException()
                    .isThrownBy(() -> SpringContextUtils.getType("anyBean"));
        }
    }
}
