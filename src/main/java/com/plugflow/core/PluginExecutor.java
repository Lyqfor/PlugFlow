package com.plugflow.core;

/**
 * 插件执行器根接口。所有被 {@link com.plugflow.core.annotation.PluginTemplate} 标注的插件模版接口应继承本接口，
 * 以约定统一的执行入口，便于 {@link PluginInvoker} 泛型调用与并行执行。
 *
 * @param <C> 上下文类型，通常为 {@link PluginContext} 或其子类
 * @param <R> 单次执行返回值类型，无返回值时可使用 {@link Void}
 */
public interface PluginExecutor<C extends PluginContext, R> {

    /**
     * 执行插件逻辑。
     *
     * @param context 插件上下文，可携带业务身份、请求参数等
     * @return 执行结果，可为 null；无返回值时使用 Void
     */
    R execute(C context);
}
