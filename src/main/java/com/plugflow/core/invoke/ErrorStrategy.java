package com.plugflow.core.invoke;

/**
 * 插件执行失败策略。
 */
public enum ErrorStrategy {
    /**
     * 任一插件执行失败，则整体调用失败（Future 异常完成 / join 抛出）。
     */
    FAIL_FAST,
    /**
     * 插件执行失败不会中断整体调用，失败会体现在每个插件的结果中。
     */
    CONTINUE
}

