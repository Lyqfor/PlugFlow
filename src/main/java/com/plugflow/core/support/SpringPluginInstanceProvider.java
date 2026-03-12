package com.plugflow.core.support;

import com.plugflow.core.registry.PluginInstanceProvider;
import org.springframework.context.ApplicationContext;

/**
 * 基于 Spring 容器的插件实例提供者：优先从 ApplicationContext 获取 Bean，
 * 若不存在则回退到反射无参构造（与默认行为一致）。引入本类需依赖 spring-context。
 */
public class SpringPluginInstanceProvider implements PluginInstanceProvider {

    private final ApplicationContext applicationContext;
    private final PluginInstanceProvider fallback;

    public SpringPluginInstanceProvider(ApplicationContext applicationContext,
                                        PluginInstanceProvider fallback) {
        this.applicationContext = applicationContext;
        this.fallback = fallback;
    }

    public SpringPluginInstanceProvider(ApplicationContext applicationContext) {
        this(applicationContext, new com.plugflow.core.registry.DefaultPluginInstanceProvider());
    }

    @Override
    public Object getInstance(Class<?> pluginClass) throws Exception {
        try {
            return applicationContext.getBean(pluginClass);
        } catch (Exception e) {
            return fallback.getInstance(pluginClass);
        }
    }
}
