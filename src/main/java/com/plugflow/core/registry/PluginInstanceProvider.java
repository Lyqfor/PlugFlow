package com.plugflow.core.registry;

/**
 * 插件实例提供者。用于在调用插件时获取实现类实例，便于与 Spring 等容器集成：
 * 若项目使用 Spring，可注册从 ApplicationContext 取 Bean 的 Provider，否则使用默认的反射+单例缓存。
 */
@FunctionalInterface
public interface PluginInstanceProvider {

    /**
     * 获取指定插件实现类的实例。
     *
     * @param pluginClass 插件实现类
     * @return 实例，不应为 null
     * @throws Exception 创建或获取失败时抛出
     */
    Object getInstance(Class<?> pluginClass) throws Exception;
}
