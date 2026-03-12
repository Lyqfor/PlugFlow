package com.plugflow.core;

import com.plugflow.core.registry.*;

import java.util.concurrent.Executor;

/**
 * PlugFlow 入口门面：用于非 Spring 场景下快速组装注册表、扫描器与调用器。
 * <p>
 * 使用示例：
 * </p>
 * <pre>
 * PluginRegistry registry = new PluginRegistry();
 * PluginScanner scanner = new PluginScanner(registry);
 * scanner.scan("com.example.myapp");
 * PluginInstanceProvider provider = new DefaultPluginInstanceProvider();
 * PluginInvoker invoker = new PluginInvoker(registry, provider);
 * // 调用
 * List&lt;Result&gt; results = invoker.invoke(MyPluginTemplate.class, context);
 * </pre>
 */
public final class PlugFlow {

    private final PluginRegistry registry;
    private final PluginInvoker invoker;

    public PlugFlow(PluginRegistry registry, PluginInstanceProvider instanceProvider,
                    GrayDecider grayDecider, Executor executor) {
        this.registry = registry;
        this.invoker = new PluginInvoker(registry, instanceProvider, grayDecider, executor);
    }

    public PlugFlow(PluginRegistry registry, PluginInstanceProvider instanceProvider) {
        this.registry = registry;
        this.invoker = new PluginInvoker(registry, instanceProvider);
    }

    public PluginRegistry getRegistry() {
        return registry;
    }

    public PluginInvoker getInvoker() {
        return invoker;
    }

    /**
     * 构建默认的 PlugFlow 实例（使用默认实例提供者与线程池），并扫描指定包。
     */
    public static PlugFlow createAndScan(String... basePackages) {
        PluginRegistry registry = new PluginRegistry();
        PluginScanner scanner = new PluginScanner(registry);
        scanner.scan(basePackages);
        PluginInstanceProvider provider = new DefaultPluginInstanceProvider();
        return new PlugFlow(registry, provider);
    }
}
