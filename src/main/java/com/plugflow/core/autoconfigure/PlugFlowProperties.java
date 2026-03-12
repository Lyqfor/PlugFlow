package com.plugflow.core.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * PlugFlow 在 Spring Boot 中的配置项。
 * <p>
 * 在 application.yml 中配置示例：
 * </p>
 * <pre>
 * plugflow:
 *   scan-packages: com.example.myapp
 *   # 或多个包
 *   # scan-packages: com.example.a, com.example.b
 * </pre>
 */
@ConfigurationProperties(prefix = "plugflow")
public class PlugFlowProperties {

    /**
     * 要扫描的包名，多个用逗号分隔。不配置则不会自动扫描，需自行调用 PluginScanner.scan()。
     */
    private String[] scanPackages = new String[0];

    /**
     * 用于并行执行插件的线程池；不配置时使用 ForkJoinPool.commonPool()。
     */
    private Executor executor = ForkJoinPool.commonPool();

    public String[] getScanPackages() {
        return scanPackages;
    }

    public void setScanPackages(String[] scanPackages) {
        this.scanPackages = scanPackages != null ? scanPackages : new String[0];
    }

    /**
     * 支持配置单个字符串（逗号分隔多包）或 List。
     */
    public void setScanPackages(String scanPackagesStr) {
        if (scanPackagesStr == null || scanPackagesStr.isBlank()) {
            this.scanPackages = new String[0];
            return;
        }
        this.scanPackages = Arrays.stream(scanPackagesStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }
}
