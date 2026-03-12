package com.plugflow.core.registry;

import com.plugflow.core.annotation.Plugin;
import com.plugflow.core.annotation.PluginTemplate;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.util.HashSet;
import java.util.Set;

/**
 * 插件扫描器：在指定包下扫描被 {@link PluginTemplate} 标注的接口与被 {@link Plugin} 标注的实现类，
 * 校验实现类确实实现了对应模版接口后，注册到 {@link PluginRegistry}。
 */
public final class PluginScanner {

    private final PluginRegistry registry;

    public PluginScanner(PluginRegistry registry) {
        this.registry = registry;
    }

    /**
     * 扫描指定包（含子包），将发现的插件注册到构造时传入的 Registry。
     *
     * @param basePackages 要扫描的包名，如 "com.example.app"
     */
    public void scan(String... basePackages) {
        if (basePackages == null || basePackages.length == 0) {
            return;
        }
        Set<Class<?>> pluginClasses = new HashSet<>();
        Set<Class<?>> templateClasses = new HashSet<>();

        for (String pkg : basePackages) {
            if (pkg == null || pkg.isBlank()) continue;
            Reflections ref = new Reflections(
                    new ConfigurationBuilder()
                            .forPackages(pkg.trim())
                            .setScanners(Scanners.TypesAnnotated)
            );
            Set<Class<?>> plugin = ref.getTypesAnnotatedWith(Plugin.class);
            Set<Class<?>> template = ref.getTypesAnnotatedWith(PluginTemplate.class);
            pluginClasses.addAll(plugin);
            templateClasses.addAll(template);
        }

        for (Class<?> impl : pluginClasses) {
            if (impl.isInterface()) {
                continue;
            }
            Plugin ann = impl.getDeclaredAnnotation(Plugin.class);
            if (ann == null) continue;
            Class<?> template = ann.template();
            if (!template.isInterface()) {
                throw new IllegalStateException("@Plugin.template() must be an interface: " + template.getName() + ", used on " + impl.getName());
            }
            if (!template.isAssignableFrom(impl)) {
                throw new IllegalStateException("Plugin " + impl.getName() + " does not implement template " + template.getName());
            }
            registry.registerFromAnnotation(impl);
        }
    }
}
