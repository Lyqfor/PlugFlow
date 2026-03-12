package com.plugflow.core;

import com.plugflow.core.annotation.Plugin;
import com.plugflow.core.annotation.PluginTemplate;
import com.plugflow.core.registry.DefaultPluginInstanceProvider;
import com.plugflow.core.registry.PluginRegistry;
import com.plugflow.core.registry.PluginScanner;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 插件注册与调用基础测试。
 */
class PluginFlowTest {

    @PluginTemplate(name = "测试模版")
    interface TestTemplate extends PluginExecutor<PluginContext, String> {
        @Override
        String execute(PluginContext context);
    }

    @Plugin(template = TestTemplate.class, name = "插件A", order = 1)
    static class PluginA implements TestTemplate {
        @Override
        public String execute(PluginContext context) {
            return "A";
        }
    }

    @Plugin(template = TestTemplate.class, name = "插件B", order = 2)
    static class PluginB implements TestTemplate {
        @Override
        public String execute(PluginContext context) {
            return "B";
        }
    }

    @Test
    void scanAndInvoke() {
        PluginRegistry registry = new PluginRegistry();
        PluginScanner scanner = new PluginScanner(registry);
        scanner.scan("com.plugflow.core");  // 当前包会扫到 PluginA, PluginB

        List<com.plugflow.core.registry.PluginDefinition> defs = registry.getPlugins(TestTemplate.class);
        assertTrue(defs.size() >= 2);

        DefaultPluginInstanceProvider provider = new DefaultPluginInstanceProvider();
        PluginInvoker invoker = new PluginInvoker(registry, provider);
        PluginContext ctx = new PluginContext();
        List<String> results = invoker.invoke(TestTemplate.class, ctx);
        assertTrue(results.contains("A"));
        assertTrue(results.contains("B"));
    }

    @Test
    void invokeAsync() {
        PluginRegistry registry = new PluginRegistry();
        registry.registerFromAnnotation(PluginA.class);
        registry.registerFromAnnotation(PluginB.class);

        PluginInvoker invoker = new PluginInvoker(registry, new DefaultPluginInstanceProvider());
        List<String> results = invoker.invokeAsync(TestTemplate.class, new PluginContext()).join();
        assertEquals(2, results.size());
        assertTrue(results.contains("A"));
        assertTrue(results.contains("B"));
    }
}
