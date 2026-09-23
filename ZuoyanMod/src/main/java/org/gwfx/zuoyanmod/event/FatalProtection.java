package org.gwfx.zuoyanmod.event;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 致命伤害保护的共享仲裁器。
 * <p>
 * 解决的问题：名刀司命与空间锚点都订阅 {@code LivingIncomingDamageEvent}，
 * 事件处理顺序取决于 NeoForge 的扫描顺序（不可控），会出现"名刀先取消事件 → 锚点直接 return → 保命生效但不传送"。
 * 这里把顺序改成显式的事件优先级 + 共享状态：
 * <ul>
 *   <li>名刀司命：{@code EventPriority.HIGH}，先行判定</li>
 *   <li>空间锚点：{@code EventPriority.NORMAL}，兜底判定</li>
 * </ul>
 * 同一次伤害只允许一个效果生效（只扣一个冷却）。
 * <p>
 * 传送不在伤害事件内直接执行，而是排队到玩家 tick 末尾执行——伤害事件中玩家实体正处在受伤流程里，
 * 此时做跨维度切换（会移除并重建实体）容易与伤害源的后续逻辑打架。
 */
public final class FatalProtection {

    /** 保护生效所在 tick（gameTime 全维度共享），用于同帧去重 */
    private static final Map<UUID, Long> PROTECTED_TICK = new ConcurrentHashMap<>();
    /** 待执行的回溯传送，玩家 tick 末尾消费 */
    private static final Map<UUID, AnchorTarget> PENDING_TELEPORT = new ConcurrentHashMap<>();
    /** 传送失败标记（维度不可用等），消费一次即清除 */
    private static final Map<UUID, Boolean> FAILED_TELEPORT = new ConcurrentHashMap<>();

    private FatalProtection() {}

    /** 本 tick 是否已经有保护效果生效（名刀已顶过，锚点就不该再消耗） */
    public static boolean isProtectedThisTick(Player player) {
        Long last = PROTECTED_TICK.get(player.getUUID());
        return last != null && last == player.level().getGameTime();
    }

    /** 标记本次保护已生效，并跳转到下一个 tick 前不再重复判定 */
    public static void markProtected(Player player) {
        PROTECTED_TICK.put(player.getUUID(), player.level().getGameTime());
    }

    /** 把回溯传送排到玩家 tick 末尾 */
    public static void queueTeleport(Player player, String dimensionId, double x, double y, double z, float yRot, float xRot) {
        PENDING_TELEPORT.put(player.getUUID(), new AnchorTarget(dimensionId, x, y, z, yRot, xRot));
    }

    /** 玩家 tick 末尾执行排队的传送，返回是否真的传了 */
    public static boolean flushTeleport(Player player) {
        AnchorTarget target = PENDING_TELEPORT.remove(player.getUUID());
        if (target == null) {
            return false;
        }
        if (player.isRemoved() || player.isDeadOrDying() || player.isSpectator()) {
            return false;
        }
        if (target.apply(player)) {
            return true;
        }
        // 维度不可用等失败情况，留给下一处提示玩家
        FAILED_TELEPORT.put(player.getUUID(), Boolean.TRUE);
        return false;
    }

    /** 取出并清除一次"传送失败"标记 */
    public static boolean consumeFailedTeleport(Player player) {
        return FAILED_TELEPORT.remove(player.getUUID()) != null;
    }

    /** 玩家离开服务器时清掉残留状态 */
    public static void clear(UUID uuid) {
        PROTECTED_TICK.remove(uuid);
        PENDING_TELEPORT.remove(uuid);
        FAILED_TELEPORT.remove(uuid);
    }

    private record AnchorTarget(String dimensionId, double x, double y, double z, float yRot, float xRot) {

        private boolean apply(Player player) {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return false;
            }
            if (!(player.level() instanceof ServerLevel currentLevel)) {
                return false;
            }
            MinecraftServer server = currentLevel.getServer();
            if (server == null) {
                return false;
            }
            ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimensionId));
            ServerLevel target = server.getLevel(dimensionKey);
            if (target == null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7空间锚点所在的维度不可用"));
                return false;
            }
            // 骑乘状态下传送会丢载具，先解除
            serverPlayer.stopRiding();
            // 跨维度分支内部是"创建新实体 + 移除旧实体"，可能失败，必须检查返回值
            boolean success = serverPlayer.teleportTo(target, x, y, z, Set.<Relative>of(), yRot, xRot, false);
            if (!success) {
                return false;
            }
            serverPlayer.playSound(net.minecraft.sounds.SoundEvents.CHORUS_FRUIT_TELEPORT, 1.0F, 1.0F);
            return true;
        }
    }
}
