package com.plugflow.core.annotation;

import java.lang.annotation.*;

/**
 * 插件模版注解。在<strong>接口</strong>上使用，声明该接口是一个插件模版；
 * 所有实现该接口且被 {@link Plugin} 标注的类将作为该模版下的具体插件被扫描与注册。
 * <p>
 * 插件模版接口建议继承 {@link com.plugflow.core.PluginExecutor} 以约定执行入口。
 * </p>
 *
 * @see Plugin
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PluginTemplate {

    /**
     * 模版名称，用于日志与监控，默认取接口简单类名。
     */
    String name() default "";

    /**
     * 模版描述。
     */
    String description() default "";
}
