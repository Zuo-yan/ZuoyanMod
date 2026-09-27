package org.gwfx.zuoyanmod.client;

import net.minecraft.client.Minecraft;
import org.gwfx.zuoyanmod.network.UpgradeSyncPacket;
import org.gwfx.zuoyanmod.upgrade.UltimateTalent;
import org.gwfx.zuoyanmod.upgrade.UpgradeType;
import org.jspecify.annotations.Nullable;

/**
 * 客户端的升级档案快照（{@link UpgradeSyncPacket} 的落地点）。
 *
 * <p>与 {@code ClientKleinBottleView} 同款套路：客户端不查服务端数据，只消费同步包；
 * HUD 与升级界面都从这里读。冷却剩余用<b>客户端世界的 gameTime</b> 对着截止值减出来
 * （原版每 tick 同步世界时间，两端天然对齐），不需要再发请求轮询。
 */
public final class ClientUpgradeData {

    private static int[] levels = new int[UpgradeType.VALUES.length];
    private static int talent = -1;
    private static long[] cooldownEnds = new long[UltimateTalent.VALUES.length];
    private static long dissociationExpire;

    private ClientUpgradeData() {}

    public static void apply(UpgradeSyncPacket packet) {
        levels = packet.levels().stream().mapToInt(Integer::intValue).toArray();
        talent = packet.talent();
        cooldownEnds = packet.cooldownEnds().stream().mapToLong(Long::longValue).toArray();
        dissociationExpire = packet.dissociationExpire();
    }

    public static int level(int statIndex) {
        return statIndex >= 0 && statIndex < levels.length ? levels[statIndex] : 0;
    }

    public static int totalLevels() {
        int sum = 0;
        for (int level : levels) {
            sum += level;
        }
        return sum;
    }

    public static boolean allMaxed() {
        return totalLevels() >= UpgradeType.VALUES.length * UpgradeType.MAX_LEVEL;
    }

    public static int talentOrdinal() {
        return talent;
    }

    public static @Nullable UltimateTalent talent() {
        return talent >= 0 && talent < UltimateTalent.VALUES.length
                ? UltimateTalent.VALUES[talent] : null;
    }

    private static long gameTime() {
        var level = Minecraft.getInstance().level;
        return level != null ? level.getGameTime() : 0L;
    }

    /** 剩余冷却秒数（向上取整），0 = 就绪。 */
    public static long cooldownRemainingSeconds(UltimateTalent talent) {
        long end = talent.ordinal() < cooldownEnds.length ? cooldownEnds[talent.ordinal()] : 0L;
        long ticks = Math.max(0L, end - gameTime());
        return (ticks + 19) / 20;
    }

    /** 「分子离解·灌能」是否生效中（HUD 用来画标记）。 */
    public static boolean dissociationArmed() {
        return dissociationExpire > gameTime();
    }
}
