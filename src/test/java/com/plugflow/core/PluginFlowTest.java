package com.plugflow.core;

import com.plugflow.core.annotation.Plugin;
import com.plugflow.core.annotation.PluginTemplate;
import com.plugflow.core.invoke.ErrorStrategy;
import com.plugflow.core.invoke.InvokeOptions;
import com.plugflow.core.invoke.PluginCallResult;
import com.plugflow.core.invoke.PluginCallStatus;
import com.plugflow.core.registry.DefaultPluginInstanceProvider;
import com.plugflow.core.registry.GrayDecider;
import com.plugflow.core.registry.PluginRegistry;
import com.plugflow.core.registry.PluginScanner;
import org.junit.jupiter.api.Test;

import java.time.Duration;
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

    @Plugin(template = TestTemplate.class, name = "禁用插件", enabled = false, order = 0)
    static class DisabledPlugin implements TestTemplate {
        @Override
        public String execute(PluginContext context) {
            return "X";
        }
    }

    @Plugin(template = TestTemplate.class, name = "异常插件", order = 3)
    static class ErrorPlugin implements TestTemplate {
        @Override
        public String execute(PluginContext context) {
            throw new IllegalStateException("boom");
        }
    }

    @Plugin(template = TestTemplate.class, name = "慢插件", order = 4)
    static class SlowPlugin implements TestTemplate {
        @Override
        public String execute(PluginContext context) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "SLOW";
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

    @Test
    void enabledFalse_shouldNotRegister() {
        PluginRegistry registry = new PluginRegistry();
        registry.registerFromAnnotation(DisabledPlugin.class);
        assertEquals(0, registry.getPlugins(TestTemplate.class).size());
    }

    @Test
    void order_shouldBeSorted() {
        PluginRegistry registry = new PluginRegistry();
        registry.registerFromAnnotation(PluginB.class);
        registry.registerFromAnnotation(PluginA.class);

        PluginInvoker invoker = new PluginInvoker(registry, new DefaultPluginInstanceProvider());
        List<String> results = invoker.invoke(TestTemplate.class, new PluginContext());
        assertEquals(List.of("A", "B"), results);
    }

    @Test
    void degradeAndGray_shouldSkip_whenIncludeSkipped() {
        PluginRegistry registry = new PluginRegistry();
        registry.registerFromAnnotation(PluginA.class);
        registry.registerFromAnnotation(PluginB.class);

        // 动态降级 PluginB
        registry.setDegrade(TestTemplate.class, PluginB.class, true);

        // 灰度决策：永远不命中（让 PluginA 也被跳过）
        GrayDecider neverHit = (ratio, ctx) -> false;
        PluginInvoker invoker = new PluginInvoker(registry, new DefaultPluginInstanceProvider(), neverHit, null);

        InvokeOptions options = InvokeOptions.builder()
                .errorStrategy(ErrorStrategy.CONTINUE)
                .includeSkipped(true)
                .build();

        List<PluginCallResult<String>> detailed = invoker.invokeDetailed(TestTemplate.class, new PluginContext(), options);
        assertEquals(2, detailed.size());
        assertTrue(detailed.stream().anyMatch(r -> r.getPluginClass().equals(PluginA.class)));
        assertTrue(detailed.stream().anyMatch(r -> r.getPluginClass().equals(PluginB.class)));
        assertTrue(detailed.stream().allMatch(r -> r.getStatus() == PluginCallStatus.SKIPPED));
    }

    @Test
    void errorStrategy_continue_shouldNotFailWholeCall() {
        PluginRegistry registry = new PluginRegistry();
        registry.registerFromAnnotation(PluginA.class);
        registry.registerFromAnnotation(ErrorPlugin.class);
        registry.registerFromAnnotation(PluginB.class);

        PluginInvoker invoker = new PluginInvoker(registry, new DefaultPluginInstanceProvider());
        InvokeOptions options = InvokeOptions.builder()
                .errorStrategy(ErrorStrategy.CONTINUE)
                .build();

        List<PluginCallResult<String>> detailed = invoker.invokeDetailed(TestTemplate.class, new PluginContext(), options);
        assertEquals(3, detailed.size());
        assertTrue(detailed.stream().anyMatch(PluginCallResult::isSuccess));
        assertTrue(detailed.stream().anyMatch(r -> r.getPluginClass().equals(ErrorPlugin.class) && r.getError() != null));
    }

    @Test
    void errorStrategy_failFast_shouldFailWholeCall() {
        PluginRegistry registry = new PluginRegistry();
        registry.registerFromAnnotation(PluginA.class);
        registry.registerFromAnnotation(ErrorPlugin.class);
        registry.registerFromAnnotation(PluginB.class);

        PluginInvoker invoker = new PluginInvoker(registry, new DefaultPluginInstanceProvider());
        InvokeOptions options = InvokeOptions.builder()
                .errorStrategy(ErrorStrategy.FAIL_FAST)
                .build();

        assertThrows(RuntimeException.class, () -> invoker.invokeDetailed(TestTemplate.class, new PluginContext(), options));
    }

    @Test
    void timeout_shouldProduceError_whenContinue() {
        PluginRegistry registry = new PluginRegistry();
        registry.registerFromAnnotation(SlowPlugin.class);

        PluginInvoker invoker = new PluginInvoker(registry, new DefaultPluginInstanceProvider());
        InvokeOptions options = InvokeOptions.builder()
                .errorStrategy(ErrorStrategy.CONTINUE)
                .timeoutPerPlugin(Duration.ofMillis(1))
                .build();

        List<PluginCallResult<String>> detailed = invoker.invokeDetailed(TestTemplate.class, new PluginContext(), options);
        assertEquals(1, detailed.size());
        assertNotNull(detailed.get(0).getError());
    }
}
