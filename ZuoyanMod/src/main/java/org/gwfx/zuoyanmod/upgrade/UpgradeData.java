package org.gwfx.zuoyanmod.upgrade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.network.UpgradeSyncPacket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 玩家的升级档案（AttachmentType，{@code copyOnDeath}——死亡不掉点）。
 *
 * <p>存四样东西：5 条基础能力的等级、已选的终极天赋（-1 = 未选）、每个天赋的冷却截止
 * （gameTime 绝对值）、「分子离解·灌能」的到期时间。序列化时冷却存的是<b>剩余秒数</b>，
 * 读档时按当前 gameTime 换算回来——存剩余值把"存档闲置一年后冷却还剩多少"这种边界问题一起消掉了。
 */
public final class UpgradeData {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Zuoyanmod.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<UpgradeData>> ATTACHMENT =
            ATTACHMENTS.register("upgrade_data", () -> AttachmentType
                    .builder(UpgradeData::new)
                    .serialize(new IAttachmentSerializer<CompoundTag, UpgradeData>() {
                        @Override
                        public UpgradeData read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                            UpgradeData data = new UpgradeData();
                            data.deserialize(tag);
                            return data;
                        }

                        @Override
                        public CompoundTag write(UpgradeData attachment, HolderLookup.Provider provider) {
                            CompoundTag tag = new CompoundTag();
                            attachment.serialize(tag);
                            return tag;
                        }
                    })
                    .copyOnDeath()
                    .build());

    public static UpgradeData of(Player player) {
        return player.getData(ATTACHMENT);
    }

    // ===== 存储本体 =====

    /** 5 条基础能力各多少级，下标对齐 {@link UpgradeType#VALUES}。 */
    private final int[] levels = new int[UpgradeType.VALUES.length];
    /** 已选终极天赋的 ordinal，-1 = 尚未选择。选定后永久不可改。 */
    private int talent = -1;
    /** 每个天赋的冷却截止 gameTime，下标对齐 {@link UltimateTalent#VALUES}。 */
    private final long[] cooldownEnds = new long[UltimateTalent.VALUES.length];
    /** 「分子离解·灌能」的到期 gameTime（0 = 未灌能）。命中目标后归零。 */
    private long dissociationExpire;

    /**
     * 已经广播过「冷却完毕」toast 的天赋（瞬态，不序列化）。
     * {@code startCooldown} 时置 false，{@link #pollReadyAnnouncement} 检测到到期时置 true；
     * 读档时若冷却早已在离线期间结束，直接按已广播处理，避免每次登录都弹一次。
     */
    private final boolean[] readyAnnounced = new boolean[UltimateTalent.VALUES.length];

    // ===== 读 =====

    public int level(int statIndex) {
        return this.levels[statIndex];
    }

    public int talentOrdinal() {
        return this.talent;
    }

    /** 已选天赋，未选时返回 null。 */
    public UltimateTalent talent() {
        return this.talent >= 0 ? UltimateTalent.VALUES[this.talent] : null;
    }

    public int totalLevels() {
        int sum = 0;
        for (int level : this.levels) {
            sum += level;
        }
        return sum;
    }

    /** 5 条全部满级（50/50），选终极天赋的前提。 */
    public boolean allMaxed() {
        return this.totalLevels() >= UpgradeType.VALUES.length * UpgradeType.MAX_LEVEL;
    }

    public boolean isOnCooldown(UltimateTalent talent, long gameTime) {
        return this.cooldownEnds[talent.ordinal()] > gameTime;
    }

    /** 剩余冷却 tick 数（用于界面倒计时，<= 0 表示就绪）。 */
    public long cooldownRemainingTicks(UltimateTalent talent, long gameTime) {
        return Math.max(0L, this.cooldownEnds[talent.ordinal()] - gameTime);
    }

    public void startCooldown(UltimateTalent talent, long gameTime) {
        this.cooldownEnds[talent.ordinal()] = gameTime + talent.cooldownTicks();
        this.readyAnnounced[talent.ordinal()] = false;
    }

    /**
     * 每 tick 轮询：已选天赋的冷却是否<b>刚刚</b>结束（结束瞬间返回一次，之后不再返回）。
     * 服务端据此发「冷却完毕」toast——HUD 不再常驻冷却显示，就绪与否全靠这一下。
     */
    public UltimateTalent pollReadyAnnouncement(long gameTime) {
        UltimateTalent talent = this.talent();
        if (talent == null) {
            return null;
        }
        int i = talent.ordinal();
        long end = this.cooldownEnds[i];
        if (end <= 0L || end > gameTime || this.readyAnnounced[i]) {
            return null;
        }
        this.readyAnnounced[i] = true;
        return talent;
    }

    /** 清空已选终极天赋（重置道具用）：连带冷却与灌能状态一起归零，可重新选择。 */
    public void clearTalent() {
        this.talent = -1;
        Arrays.fill(this.cooldownEnds, 0L);
        this.dissociationExpire = 0L;
        Arrays.fill(this.readyAnnounced, false);
    }

    public boolean dissociationArmed(long gameTime) {
        return this.dissociationExpire > gameTime;
    }

    public void armDissociation(long gameTime, int durationTicks) {
        this.dissociationExpire = gameTime + durationTicks;
    }

    public void consumeDissociation() {
        this.dissociationExpire = 0L;
    }

    // ===== 写 =====

    public void setLevel(int statIndex, int level) {
        this.levels[statIndex] = level;
    }

    /** 选定终极天赋。调用方负责保证此前 talent == -1（选定即永久）。 */
    public void setTalent(UltimateTalent talent) {
        this.talent = talent.ordinal();
    }

    /** 打成同步包发往客户端（界面与 HUD 都只读这份快照）。 */
    public UpgradeSyncPacket toSyncPacket() {
        List<Integer> levelList = new ArrayList<>(this.levels.length);
        for (int level : this.levels) {
            levelList.add(level);
        }
        List<Long> cooldownList = new ArrayList<>(this.cooldownEnds.length);
        for (long end : this.cooldownEnds) {
            cooldownList.add(end);
        }
        return new UpgradeSyncPacket(levelList, this.talent, cooldownList, this.dissociationExpire);
    }

    // ===== 序列化 =====

    private void serialize(CompoundTag tag) {
        long gameTime = serverGameTime();
        tag.putIntArray("Levels", this.levels);
        tag.putInt("Talent", this.talent);
        int[] cooldownSeconds = new int[this.cooldownEnds.length];
        for (int i = 0; i < cooldownSeconds.length; i++) {
            cooldownSeconds[i] = remainingSeconds(this.cooldownEnds[i], gameTime);
        }
        tag.putIntArray("CooldownSeconds", cooldownSeconds);
        tag.putInt("DissociationSeconds", remainingSeconds(this.dissociationExpire, gameTime));
    }

    private void deserialize(CompoundTag tag) {
        long gameTime = serverGameTime();
        int[] levels = tag.getIntArray("Levels");
        if (levels != null && levels.length == this.levels.length) {
            System.arraycopy(levels, 0, this.levels, 0, levels.length);
        }
        this.talent = Math.min(tag.contains("Talent") ? tag.getInt("Talent") : -1, UltimateTalent.VALUES.length - 1);
        int[] cooldownSeconds = tag.getIntArray("CooldownSeconds");
        if (cooldownSeconds != null) {
            for (int i = 0; i < Math.min(cooldownSeconds.length, this.cooldownEnds.length); i++) {
                this.cooldownEnds[i] = gameTime + cooldownSeconds[i] * 20L;
                // 离线期间就冷却完的天赋按已广播处理，登录不弹陈年 toast
                this.readyAnnounced[i] = this.cooldownEnds[i] <= gameTime;
            }
        }
        this.dissociationExpire = gameTime + tag.getInt("DissociationSeconds") * 20L;
    }

    private static int remainingSeconds(long end, long gameTime) {
        return (int) Math.ceil(Math.max(0L, end - gameTime) / 20.0D);
    }

    /**
     * 序列化时机的 gameTime。玩家存读档都发生在服务器运行中，钩子必然已就位；
     * 万一在无服务器上下文时被序列化（理论上不会），按 0 处理也只是冷却基准归零，无害。
     */
    private static long serverGameTime() {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? 0L : server.overworld().getGameTime();
    }
}
