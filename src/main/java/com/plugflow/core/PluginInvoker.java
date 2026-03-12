package com.plugflow.core;

import com.plugflow.core.invoke.ErrorStrategy;
import com.plugflow.core.invoke.InvokeOptions;
import com.plugflow.core.invoke.PluginCallResult;
import com.plugflow.core.invoke.SkipReason;
import com.plugflow.core.registry.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

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
     * 异步并行执行某模版下的所有命中插件，返回插件结果列表。
     * <p>
     * 默认策略为 FAIL_FAST：任一插件异常会导致整体 Future 异常完成。
     * </p>
     *
     * @param templateClass 插件模版类型，需继承 {@link PluginExecutor}{@code <C, R>}
     * @param context       执行上下文
     * @return 每个命中插件的执行结果（顺序与插件 order 一致）
     */
    public <C extends PluginContext, R> CompletableFuture<List<R>> invokeAsync(
            Class<? extends PluginExecutor<C, R>> templateClass, C context) {
        return invokeDetailedAsync(templateClass, context, InvokeOptions.defaults())
                .thenApply(detailed -> {
                    List<R> results = new ArrayList<>();
                    for (PluginCallResult<R> r : detailed) {
                        if (r.getError() == null) {
                            results.add(r.getResult());
                        }
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

    /**
     * 异步并行执行某模版下的插件，返回每个插件的详细结果（可选包含被跳过的插件）。
     */
    @SuppressWarnings("unchecked")
    public <C extends PluginContext, R> CompletableFuture<List<PluginCallResult<R>>> invokeDetailedAsync(
            Class<? extends PluginExecutor<C, R>> templateClass, C context, InvokeOptions options) {

        Objects.requireNonNull(templateClass, "templateClass");
        Objects.requireNonNull(context, "context");
        InvokeOptions opt = options != null ? options : InvokeOptions.defaults();

        List<PluginDefinition> definitions = registry.getPlugins(templateClass);
        if (definitions.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        List<CompletableFuture<PluginCallResult<R>>> futures = new ArrayList<>();
        for (PluginDefinition d : definitions) {
            if (!d.isEnabled()) {
                if (opt.isIncludeSkipped()) {
                    futures.add(CompletableFuture.completedFuture(
                            PluginCallResult.skipped(d.getName(), d.getPluginClass(), SkipReason.DISABLED)));
                }
                continue;
            }
            if (d.isDegrade()) {
                if (opt.isIncludeSkipped()) {
                    futures.add(CompletableFuture.completedFuture(
                            PluginCallResult.skipped(d.getName(), d.getPluginClass(), SkipReason.DEGRADED)));
                }
                continue;
            }
            if (!grayDecider.hit(d.getGrayRatio(), context)) {
                if (opt.isIncludeSkipped()) {
                    futures.add(CompletableFuture.completedFuture(
                            PluginCallResult.skipped(d.getName(), d.getPluginClass(), SkipReason.GRAY_MISS)));
                }
                continue;
            }

            CompletableFuture<PluginCallResult<R>> f = CompletableFuture.supplyAsync(() -> {
                long start = System.nanoTime();
                try {
                    Object instance = instanceProvider.getInstance(d.getPluginClass());
                    PluginExecutor<C, R> plugin = (PluginExecutor<C, R>) instance;
                    R result = plugin.execute(context);
                    return PluginCallResult.executed(d.getName(), d.getPluginClass(), result, null, System.nanoTime() - start);
                } catch (Throwable t) {
                    return PluginCallResult.executed(d.getName(), d.getPluginClass(), null, t, System.nanoTime() - start);
                }
            }, executor);

            if (opt.getTimeoutPerPlugin() != null) {
                long ms = opt.getTimeoutPerPlugin().toMillis();
                if (ms > 0) {
                    f = f.orTimeout(ms, TimeUnit.MILLISECONDS)
                            .exceptionally(ex -> PluginCallResult.executed(d.getName(), d.getPluginClass(), null, ex, 0L));
                }
            }

            if (opt.getErrorStrategy() == ErrorStrategy.FAIL_FAST) {
                f = f.thenApply(r -> {
                    if (r.getError() != null) {
                        throw new RuntimeException("Plugin execution failed: " + r.getPluginClass().getName(), r.getError());
                    }
                    return r;
                });
            } else {
                f = f.exceptionally(ex -> PluginCallResult.executed(d.getName(), d.getPluginClass(), null, ex, 0L));
            }

            futures.add(f);
        }

        if (futures.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<PluginCallResult<R>> results = new ArrayList<>(futures.size());
                    for (CompletableFuture<PluginCallResult<R>> future : futures) {
                        results.add(future.join());
                    }
                    return results;
                });
    }

    /**
     * 同步执行并返回详细结果。
     */
    public <C extends PluginContext, R> List<PluginCallResult<R>> invokeDetailed(
            Class<? extends PluginExecutor<C, R>> templateClass, C context, InvokeOptions options) {
        return invokeDetailedAsync(templateClass, context, options).join();
    }
}
