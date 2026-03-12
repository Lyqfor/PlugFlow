package com.plugflow.core.registry;

import com.plugflow.core.PluginContext;

/**
 * 灰度决策器：决定当前流量是否命中某插件的灰度比例。
 * 默认实现为随机数；业务可替换为基于用户 ID、租户、标签等的一致性灰度。
 */
@FunctionalInterface
public interface GrayDecider {

    /**
     * 是否命中灰度。
     *
     * @param grayRatio 插件配置的灰度比例 0–100
     * @param context   可选上下文，用于一致性灰度（如 userId）
     * @return true 表示命中，应执行该插件
     */
    boolean hit(int grayRatio, PluginContext context);

    /** 默认实现：随机 0–99 小于 grayRatio 则命中 */
    static GrayDecider random() {
        return (grayRatio, ctx) -> grayRatio >= 100 || (grayRatio > 0 && java.util.concurrent.ThreadLocalRandom.current().nextInt(100) < grayRatio);
    }
}
