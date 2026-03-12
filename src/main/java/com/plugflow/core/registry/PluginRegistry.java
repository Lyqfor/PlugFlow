package com.plugflow.core.registry;

import com.plugflow.core.annotation.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件注册表：维护「插件模版 → 插件定义列表」的映射，在应用启动时由扫描器填充，
 * 支持运行时动态调整降级、启用、灰度比例等。
 */
public final class PluginRegistry {

    /** template Class -> 按 order 排好序的 PluginDefinition 列表（不可变快照，可替换） */
    private final Map<Class<?>, List<PluginDefinition>> templateToPlugins = new ConcurrentHashMap<>();

    /**
     * 注册一个插件：将实现类绑定到其模版，并加入排序列表。
     */
    public void register(Class<?> templateClass, PluginDefinition definition) {
        if (templateClass == null || definition == null) {
            throw new IllegalArgumentException("template and definition must be non-null");
        }
        if (!templateClass.isInterface()) {
            throw new IllegalArgumentException("Plugin template must be an interface: " + templateClass.getName());
        }
        if (!templateClass.isAssignableFrom(definition.getPluginClass())) {
            throw new IllegalArgumentException("Plugin " + definition.getPluginClass().getName()
                    + " does not implement template " + templateClass.getName());
        }
        templateToPlugins.compute(templateClass, (k, list) -> {
            List<PluginDefinition> newList = list != null ? new ArrayList<>(list) : new ArrayList<>();
            newList.add(definition);
            newList.sort(PluginDefinition.ORDER_COMPARATOR);
            return Collections.unmodifiableList(newList);
        });
    }

    /**
     * 根据注解注册一个插件实现类。
     */
    public void registerFromAnnotation(Class<?> pluginImplClass) {
        Plugin ann = pluginImplClass.getDeclaredAnnotation(Plugin.class);
        if (ann == null) {
            throw new IllegalArgumentException("Class must be annotated with @Plugin: " + pluginImplClass.getName());
        }
        Class<?> template = ann.template();
        if (!template.isInterface()) {
            throw new IllegalArgumentException("@Plugin.template() must be an interface: " + template.getName());
        }
        PluginDefinition def = PluginDefinition.fromAnnotation(pluginImplClass, ann);
        if (!def.isEnabled()) {
            return;
        }
        register(template, def);
    }

    /**
     * 获取某模版下已注册的插件定义列表（只读），按 order 排序。
     */
    public List<PluginDefinition> getPlugins(Class<?> templateClass) {
        List<PluginDefinition> list = templateToPlugins.get(templateClass);
        return list == null ? List.of() : list;
    }

    /**
     * 获取某模版下已注册的插件定义列表（可变副本），便于调用方过滤或修改后使用。
     */
    public List<PluginDefinition> getPluginsCopy(Class<?> templateClass) {
        return new ArrayList<>(getPlugins(templateClass));
    }

    /**
     * 是否已注册该模版。
     */
    public boolean hasTemplate(Class<?> templateClass) {
        return templateToPlugins.containsKey(templateClass);
    }

    /**
     * 所有已注册的模版 Class。
     */
    public Set<Class<?>> getTemplateClasses() {
        return new HashSet<>(templateToPlugins.keySet());
    }

    /**
     * 设置某插件实现类的降级状态（按 Class 查找并更新第一个匹配的 Definition）。
     */
    public void setDegrade(Class<?> templateClass, Class<?> pluginImplClass, boolean degrade) {
        List<PluginDefinition> list = templateToPlugins.get(templateClass);
        if (list == null) return;
        for (PluginDefinition d : list) {
            if (d.getPluginClass().equals(pluginImplClass)) {
                d.setDegrade(degrade);
                return;
            }
        }
    }

    /**
     * 设置某插件实现类的启用状态。
     */
    public void setEnabled(Class<?> templateClass, Class<?> pluginImplClass, boolean enabled) {
        List<PluginDefinition> list = templateToPlugins.get(templateClass);
        if (list == null) return;
        for (PluginDefinition d : list) {
            if (d.getPluginClass().equals(pluginImplClass)) {
                d.setEnabled(enabled);
                return;
            }
        }
    }

    /**
     * 设置某插件实现类的灰度比例。
     */
    public void setGrayRatio(Class<?> templateClass, Class<?> pluginImplClass, int grayRatio) {
        List<PluginDefinition> list = templateToPlugins.get(templateClass);
        if (list == null) return;
        for (PluginDefinition d : list) {
            if (d.getPluginClass().equals(pluginImplClass)) {
                d.setGrayRatio(grayRatio);
                return;
            }
        }
    }
}
