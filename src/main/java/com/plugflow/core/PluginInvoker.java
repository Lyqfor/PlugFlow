package com.plugflow.core;

import com.plugflow.core.registry.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * 插件调用工具类：根据插件模版类型从注册表加载对应插件，按启用/降级/灰度过滤后，
 * 以异步并行的方式执行，并汇总结果。
 * <p>
 * 插件模版接口需继承 {@link PluginExecutor}{@code <C, R>}，以便统一调用 {@code execute(context)}。
 * </p>
 */
public final class PluginInvoker {

    private final PluginRegistry registry;
    private final PluginInstanceProvider instanceProvider;
    private final GrayDecider grayDecider;
    private final Executor executor;

    public PluginInvoker(PluginRegistry registry,
                         PluginInstanceProvider instanceProvider,
                         GrayDecider grayDecider,
                         Executor executor) {
        this.registry = registry;
        this.instanceProvider = instanceProvider;
        this.grayDecider = grayDecider != null ? grayDecider : GrayDecider.random();
        this.executor = executor != null ? executor : ForkJoinPool.commonPool();
    }

    public PluginInvoker(PluginRegistry registry, PluginInstanceProvider instanceProvider) {
        this(registry, instanceProvider, GrayDecider.random(), ForkJoinPool.commonPool());
    }

    /**
     * 异步并行执行某模版下的所有命中插件，返回各插件执行结果的 Future 列表。
     *
     * @param templateClass 插件模版类型，需继承 {@link PluginExecutor}{@code <C, R>}
     * @param context       执行上下文
     * @return 每个命中插件的执行结果（顺序与执行完成顺序可能不一致）
     */
    @SuppressWarnings("unchecked")
    public <C extends PluginContext, R> CompletableFuture<List<R>> invokeAsync(
            Class<? extends PluginExecutor<C, R>> templateClass, C context) {

        List<PluginDefinition> definitions = registry.getPlugins(templateClass);
        List<PluginDefinition> eligible = new ArrayList<>();
        for (PluginDefinition d : definitions) {
            if (!d.isEnabled() || d.isDegrade()) continue;
            if (!grayDecider.hit(d.getGrayRatio(), context)) continue;
            eligible.add(d);
        }

        if (eligible.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        List<CompletableFuture<R>> futures = new ArrayList<>();
        for (PluginDefinition d : eligible) {
            CompletableFuture<R> f = CompletableFuture.supplyAsync(() -> {
                try {
                    Object instance = instanceProvider.getInstance(d.getPluginClass());
                    PluginExecutor<C, R> plugin = (PluginExecutor<C, R>) instance;
                    return plugin.execute(context);
                } catch (Throwable t) {
                    throw new RuntimeException("Plugin execution failed: " + d.getPluginClass().getName(), t);
                }
            }, executor);
            futures.add(f);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<R> results = new ArrayList<>(futures.size());
                    for (CompletableFuture<R> future : futures) {
                        results.add(future.join());
                    }
                    return results;
                });
    }

    /**
     * 同步执行某模版下的所有命中插件，并行执行后等待全部完成并返回结果列表。
     */
    public <C extends PluginContext, R> List<R> invoke(
            Class<? extends PluginExecutor<C, R>> templateClass, C context) {
        return invokeAsync(templateClass, context).join();
    }
}
