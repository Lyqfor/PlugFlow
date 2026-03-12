package com.plugflow.core.registry;

import com.plugflow.core.annotation.Plugin;

import java.util.Comparator;

/**
 * 插件定义元数据，对应一个 {@link Plugin} 实现类的注册信息，
 * 包含顺序、启用、灰度、降级等可运行时调整的属性。
 */
public final class PluginDefinition {

    private final Class<?> pluginClass;
    private final String name;
    private final int order;
    private volatile boolean enabled;
    private volatile int grayRatio;
    private volatile boolean degrade;
    private final String description;

    public PluginDefinition(Class<?> pluginClass, String name, int order,
                            boolean enabled, int grayRatio, boolean degrade, String description) {
        this.pluginClass = pluginClass;
        this.name = name != null && !name.isEmpty() ? name : pluginClass.getSimpleName();
        this.order = order;
        this.enabled = enabled;
        this.grayRatio = Math.max(0, Math.min(100, grayRatio));
        this.degrade = degrade;
        this.description = description != null ? description : "";
    }

    public static PluginDefinition fromAnnotation(Class<?> pluginClass, Plugin ann) {
        if (ann == null) {
            return new PluginDefinition(pluginClass, pluginClass.getSimpleName(), 0, true, 100, false, "");
        }
        return new PluginDefinition(
                pluginClass,
                ann.name().isEmpty() ? pluginClass.getSimpleName() : ann.name(),
                ann.order(),
                ann.enabled(),
                ann.grayRatio(),
                ann.degrade(),
                ann.description()
        );
    }

    public Class<?> getPluginClass() { return pluginClass; }
    public String getName() { return name; }
    public int getOrder() { return order; }
    public boolean isEnabled() { return enabled; }
    public int getGrayRatio() { return grayRatio; }
    public boolean isDegrade() { return degrade; }
    public String getDescription() { return description; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setGrayRatio(int grayRatio) { this.grayRatio = Math.max(0, Math.min(100, grayRatio)); }
    public void setDegrade(boolean degrade) { this.degrade = degrade; }

    /** 按 order 升序，同 order 按类名排序 */
    public static final Comparator<PluginDefinition> ORDER_COMPARATOR =
            Comparator.comparingInt(PluginDefinition::getOrder)
                    .thenComparing(p -> p.getPluginClass().getName());
}
