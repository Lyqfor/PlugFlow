package com.plugflow.core.invoke;

import java.time.Duration;

/**
 * 插件调用选项。
 */
public final class InvokeOptions {

    private final ErrorStrategy errorStrategy;
    private final Duration timeoutPerPlugin;
    private final boolean includeSkipped;

    private InvokeOptions(Builder builder) {
        this.errorStrategy = builder.errorStrategy;
        this.timeoutPerPlugin = builder.timeoutPerPlugin;
        this.includeSkipped = builder.includeSkipped;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ErrorStrategy getErrorStrategy() {
        return errorStrategy;
    }

    public Duration getTimeoutPerPlugin() {
        return timeoutPerPlugin;
    }

    public boolean isIncludeSkipped() {
        return includeSkipped;
    }

    public static InvokeOptions defaults() {
        return builder().build();
    }

    public static final class Builder {
        private ErrorStrategy errorStrategy = ErrorStrategy.FAIL_FAST;
        private Duration timeoutPerPlugin = null;
        private boolean includeSkipped = false;

        public Builder errorStrategy(ErrorStrategy errorStrategy) {
            if (errorStrategy != null) {
                this.errorStrategy = errorStrategy;
            }
            return this;
        }

        public Builder timeoutPerPlugin(Duration timeoutPerPlugin) {
            if (timeoutPerPlugin != null && !timeoutPerPlugin.isNegative() && !timeoutPerPlugin.isZero()) {
                this.timeoutPerPlugin = timeoutPerPlugin;
            } else {
                this.timeoutPerPlugin = null;
            }
            return this;
        }

        public Builder includeSkipped(boolean includeSkipped) {
            this.includeSkipped = includeSkipped;
            return this;
        }

        public InvokeOptions build() {
            return new InvokeOptions(this);
        }
    }
}

