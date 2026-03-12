package com.plugflow.core.invoke;

/**
 * 插件被跳过的原因（仅在 includeSkipped=true 时可见）。
 */
public enum SkipReason {
    DISABLED,
    DEGRADED,
    GRAY_MISS
}

