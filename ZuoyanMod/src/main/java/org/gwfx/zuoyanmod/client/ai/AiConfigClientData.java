package org.gwfx.zuoyanmod.client.ai;

import org.gwfx.zuoyanmod.ai.core.config.AiConfigSnapshot;

/**
 * 客户端侧持有的 AI 配置快照（最近一次从服务端同步来的）。
 *
 * <p>与 {@code client.ClientUpgradeData} 的分工完全一致：界面只读它，不发请求、不自己算，
 * 由 payload 处理线程调度到客户端主线程后写入。
 *
 * <p>刻意<b>不引用任何 {@code net.minecraft.client.*}</b>：这样双端类（payload）可以直接
 * 在 {@code enqueueWork} 的 lambda 里引用它，专用服务端也不会因此加载客户端类。
 */
public final class AiConfigClientData {

    private static volatile AiConfigSnapshot snapshot;

    private AiConfigClientData() {
    }

    /** 写入一份新快照。 */
    public static void apply(String json) {
        snapshot = AiConfigSnapshot.fromJson(json);
    }

    /** 最近一次快照；尚未收到时为 {@code null}（界面显示「加载中…」）。 */
    public static AiConfigSnapshot snapshot() {
        return snapshot;
    }

    /**
     * 清掉快照。
     *
     * <p>打开界面前调用：否则会先闪出上一次的（可能来自别的服务器/存档的）配置，
     * 再被新快照覆盖，看起来像"配置自己变了"。
     */
    public static void clear() {
        snapshot = null;
    }
}
