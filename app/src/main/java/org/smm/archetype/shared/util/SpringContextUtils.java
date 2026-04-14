package org.smm.archetype.shared.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Spring上下文工具类
 *
 * 提供静态方法访问Spring容器中的Bean，实现了ApplicationContextAware接口，
 * 在Spring容器启动时自动注入ApplicationContext对象。
 * 可用于在非Spring管理的类中获取Spring容器中的Bean实例。
 */
public class SpringContextUtils implements ApplicationContextAware {

    public static ApplicationContext context;

    /**
     * 根据Bean名称获取Bean实例
     *
     * 从Spring容器中获取指定名称的Bean实例。这是一个静态方法，可以在任何地方调用，
     * 不需要依赖Spring容器注入。
     * @param name Bean的名称
     * @return Bean实例对象
     */
    public static Object getBean(String name) {
        return context.getBean(name);
    }

    /**
     * 根据Bean类型获取Bean实例
     *
     * 从Spring容器中获取指定类型的Bean实例。这是一个静态方法，可以在任何地方调用，
     * 不需要依赖Spring容器注入。
     * @param requiredType Bean的类型
     * @param <T>          Bean的泛型类型
     * @return Bean实例对象
     */
    public static <T> T getBean(Class<T> requiredType) {
        return context.getBean(requiredType);
    }

    /**
     * 根据Bean名称和类型获取Bean实例
     *
     * 从Spring容器中获取指定名称和类型的Bean实例。这是一个静态方法，可以在任何地方调用，
     * 不需要依赖Spring容器注入。
     * @param name         Bean的名称
     * @param requiredType Bean的类型
     * @param <T>          Bean的泛型类型
     * @return Bean实例对象
     */
    public static <T> T getBean(String name, Class<T> requiredType) {
        return context.getBean(name, requiredType);
    }

    /**
     * 判断Spring容器中是否包含指定名称的Bean
     *
     * 检查Spring容器中是否存在指定名称的Bean定义。
     * @param name Bean的名称
     * @return 如果存在返回true，否则返回false
     */
    public static boolean containsBean(String name) {
        return context.containsBean(name);
    }

    /**
     * 判断指定名称的Bean是否为单例
     *
     * 检查Spring容器中指定名称的Bean是否配置为单例模式。
     * @param name Bean的名称
     * @return 如果是单例返回true，否则返回false
     */
    public static boolean isSingleton(String name) {
        return context.isSingleton(name);
    }

    /**
     * 获取指定名称Bean的类型
     *
     * 获取Spring容器中指定名称Bean的实际类型。
     * @param name Bean的名称
     * @return Bean的类型Class对象
     */
    public static Class<?> getType(String name) {
        return context.getType(name);
    }

    /**
     * 设置ApplicationContext对象
     *
     * 实现ApplicationContextAware接口的方法，在Spring容器启动时自动调用，
     * 将ApplicationContext对象注入到静态变量中，供其他静态方法使用。
     * @param applicationContext Spring的ApplicationContext对象
     * @throws BeansException 当设置ApplicationContext时发生错误
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextUtils.context = applicationContext;
    }

}
