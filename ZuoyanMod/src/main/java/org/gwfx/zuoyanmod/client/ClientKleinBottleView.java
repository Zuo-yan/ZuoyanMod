package org.gwfx.zuoyanmod.client;

import org.gwfx.zuoyanmod.network.KleinBottleSyncPacket;

/**
 * 客户端侧的「四维空间」视图快照。
 *
 * <p>角色对应 AE2 的 {@code Repo}：服务端的存储内容通过 {@link KleinBottleSyncPacket}
 * 持续推过来，界面只负责画。区别是 AE2 把整份物品清单都同步下来、过滤排序都在客户端做；
 * 这里存储挂在玩家附件上、种类数不设上限，全量同步不划算，所以只同步
 * <b>当前窗口那几格的总量</b> + <b>视图统计</b>，过滤排序留在服务端。
 *
 * <p>换界面时（{@link KleinBottleScreen#init()}）会 {@link #clear()} 一次，
 * 免得刚打开时闪一下上一个终端的数字。
 */
public final class ClientKleinBottleView {

    private static KleinBottleSyncPacket latest;

    private ClientKleinBottleView() {}

    public static void apply(KleinBottleSyncPacket packet) {
        latest = packet;
    }

    public static KleinBottleSyncPacket latest() {
        return latest;
    }

    /** 当前窗口第 slot 格的总量；还没有快照时返回 0（界面会退化成只显示槽位自带的数量） */
    public static long windowTotal(int slot) {
        return latest == null ? 0L : latest.windowTotal(slot);
    }

    public static void clear() {
        latest = null;
    }
}
