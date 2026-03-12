package com.plugflow.core.annotation;

import java.lang.annotation.*;

/**
 * 插件注解。在插件模版的<strong>实现类</strong>上使用，用于指定该实现属于哪个插件模版，
 * 并可配置执行顺序、灰度、降级等企业级属性。
 *
 * @see PluginTemplate
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Plugin {

    /**
     * 该插件所属的插件模版（接口）的 Class。
     */
    Class<?> template();

    /**
     * 插件名称，用于日志与监控，默认取实现类简单类名。
     */
    String name() default "";

    /**
     * 执行顺序，数值越小越先执行；同 order 时按类名排序。默认 0。
     */
    int order() default 0;

    /**
     * 是否启用。false 时该插件不会被加载与执行。
     */
    boolean enabled() default true;

    /**
     * 灰度比例 0–100。仅对灰度比例内的流量执行该插件，可用于灰度发布。
     * 100 表示全量，0 表示不执行（与 enabled=false 效果类似）。
     */
    int grayRatio() default 100;

    /**
     * 是否降级。true 时该插件在执行时会被跳过，用于故障快速止血。
     * 运行时可通过 {@link com.plugflow.core.registry.PluginRegistry} 或配置动态更新。
     */
    boolean degrade() default false;

    /**
     * 描述信息。
     */
    String description() default "";
}
