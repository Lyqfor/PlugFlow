package com.plugflow.core.invoke;

import java.util.Objects;

/**
 * 单个插件的调用结果（包含执行结果、异常、耗时、以及被跳过的原因）。
 */
public final class PluginCallResult<R> {

    private final String pluginName;
    private final Class<?> pluginClass;
    private final PluginCallStatus status;
    private final SkipReason skipReason;
    private final R result;
    private final Throwable error;
    private final long durationNanos;

    private PluginCallResult(String pluginName,
                             Class<?> pluginClass,
                             PluginCallStatus status,
                             SkipReason skipReason,
                             R result,
                             Throwable error,
                             long durationNanos) {
        this.pluginName = Objects.requireNonNull(pluginName, "pluginName");
        this.pluginClass = Objects.requireNonNull(pluginClass, "pluginClass");
        this.status = Objects.requireNonNull(status, "status");
        this.skipReason = skipReason;
        this.result = result;
        this.error = error;
        this.durationNanos = durationNanos;
    }

    public static <R> PluginCallResult<R> executed(String pluginName,
                                                   Class<?> pluginClass,
                                                   R result,
                                                   Throwable error,
                                                   long durationNanos) {
        return new PluginCallResult<>(pluginName, pluginClass, PluginCallStatus.EXECUTED, null, result, error, durationNanos);
    }

    public static <R> PluginCallResult<R> skipped(String pluginName,
                                                  Class<?> pluginClass,
                                                  SkipReason reason) {
        return new PluginCallResult<>(pluginName, pluginClass, PluginCallStatus.SKIPPED, reason, null, null, 0L);
    }

    public String getPluginName() {
        return pluginName;
    }

    public Class<?> getPluginClass() {
        return pluginClass;
    }

    public PluginCallStatus getStatus() {
        return status;
    }

    public SkipReason getSkipReason() {
        return skipReason;
    }

    public R getResult() {
        return result;
    }

    public Throwable getError() {
        return error;
    }

    public long getDurationNanos() {
        return durationNanos;
    }

    public boolean isSuccess() {
        return status == PluginCallStatus.EXECUTED && error == null;
    }
}

