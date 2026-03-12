package com.plugflow.core.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认插件实例提供者：无参构造反射创建，按 Class 缓存单例。
 */
public class DefaultPluginInstanceProvider implements PluginInstanceProvider {

    private final Map<Class<?>, Object> cache = new ConcurrentHashMap<>();

    @Override
    public Object getInstance(Class<?> pluginClass) throws Exception {
        return cache.computeIfAbsent(pluginClass, c -> {
            try {
                return c.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalStateException("Cannot instantiate plugin: " + c.getName(), e);
            }
        });
    }

    /** 清除缓存（如热更新时使用） */
    public void clearCache() {
        cache.clear();
    }

    public void removeFromCache(Class<?> pluginClass) {
        cache.remove(pluginClass);
    }
}
