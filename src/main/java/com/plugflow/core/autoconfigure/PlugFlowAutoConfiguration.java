package com.plugflow.core.autoconfigure;

import com.plugflow.core.PluginInvoker;
import com.plugflow.core.registry.*;
import com.plugflow.core.support.SpringPluginInstanceProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

import java.util.concurrent.Executor;

/**
 * Spring Boot 自动配置：注册 PluginRegistry、PluginScanner、PluginInvoker，
 * 并从配置项读取扫描包后执行扫描。需在 classpath 中存在 Spring Boot 与 Spring 时生效。
 */
@Configuration
@ConditionalOnClass(ApplicationContext.class)
@EnableConfigurationProperties(PlugFlowProperties.class)
public class PlugFlowAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PluginRegistry pluginRegistry() {
        return new PluginRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public PluginScanner pluginScanner(PluginRegistry pluginRegistry) {
        return new PluginScanner(pluginRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public PluginInstanceProvider pluginInstanceProvider(ApplicationContext applicationContext) {
        return new SpringPluginInstanceProvider(applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public PluginInvoker pluginInvoker(PluginRegistry pluginRegistry,
                                       PluginInstanceProvider pluginInstanceProvider,
                                       PlugFlowProperties properties) {
        Executor exec = properties.getExecutor();
        return new PluginInvoker(
                pluginRegistry,
                pluginInstanceProvider,
                GrayDecider.random(),
                exec != null ? exec : java.util.concurrent.ForkJoinPool.commonPool()
        );
    }

    @Bean
    public PlugFlowScannerRunner plugFlowScannerRunner(PluginScanner pluginScanner,
                                                       PlugFlowProperties properties) {
        return new PlugFlowScannerRunner(pluginScanner, properties);
    }

    /**
     * 启动时根据配置执行包扫描。
     */
    public static class PlugFlowScannerRunner {

        private final PluginScanner pluginScanner;
        private final PlugFlowProperties properties;

        public PlugFlowScannerRunner(PluginScanner pluginScanner, PlugFlowProperties properties) {
            this.pluginScanner = pluginScanner;
            this.properties = properties;
        }

        @PostConstruct
        public void scan() {
            String[] packages = properties.getScanPackages();
            if (packages != null && packages.length > 0) {
                pluginScanner.scan(packages);
            }
        }
    }
}
