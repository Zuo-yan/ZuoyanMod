package org.gwfx.zuoyanmod.event;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.effect.EffectRegistry;
import org.gwfx.zuoyanmod.item.ItemRegistry;
import org.gwfx.zuoyanmod.util.AccessoryChecks;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Zuoyanmod.MODID)
public class WanHuiRingEventHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>[] NEGATIVE_EFFECTS = new net.minecraft.core.Holder[]{
            MobEffects.BLINDNESS,
            MobEffects.NAUSEA,
            MobEffects.POISON,
            MobEffects.HUNGER,
            MobEffects.WEAKNESS,
            MobEffects.SLOWNESS,
            MobEffects.MINING_FATIGUE
    };

    private static final int REQUIRED_EFFECT_COUNT = 3;
    private static final int FIGHT_AGAIN_DURATION = 600;
    private static final int COOLDOWN_TICKS = 1200;
    private static final int DETECTION_INTERVAL = 20;

    private static final ConcurrentHashMap<UUID, Long> COOLDOWN_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Integer> TICK_COUNTER_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Boolean> IS_TRIGGERING_MAP = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        if (!hasWanHuiRingInInventory(player)) return;

        UUID playerId = player.getUUID();

        if (IS_TRIGGERING_MAP.getOrDefault(playerId, false)) return;

        int tickCounter = TICK_COUNTER_MAP.getOrDefault(playerId, 0);
        tickCounter++;
        TICK_COUNTER_MAP.put(playerId, tickCounter);

        if (tickCounter % DETECTION_INTERVAL != 0) return;

        long currentTime = System.currentTimeMillis();

        Long lastTriggerTime = COOLDOWN_MAP.get(playerId);
        if (lastTriggerTime != null && (currentTime - lastTriggerTime) < (COOLDOWN_TICKS * 50)) {
            return;
        }

        int negativeEffectCount = countNegativeEffects(player);

        if (negativeEffectCount >= REQUIRED_EFFECT_COUNT) {
            IS_TRIGGERING_MAP.put(playerId, true);
            try {
                triggerBaiZhanWuShang(player);
                COOLDOWN_MAP.put(playerId, currentTime);
            } catch (Exception e) {
                LOGGER.error("Error triggering BaiZhanWuShang effect for player: " + player.getName().getString(), e);
            } finally {
                IS_TRIGGERING_MAP.remove(playerId);
            }
        }
    }

    private static void triggerBaiZhanWuShang(Player player) {
        if (player == null || player.isRemoved()) return;

        // 收集所有效果以便移除
        List<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> effectsToRemove = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (effect != null) {
                effectsToRemove.add(effect.getEffect());
            }
        }

        for (var effect : effectsToRemove) {
            try {
                player.removeEffect(effect);
            } catch (Exception e) {
                LOGGER.warn("Error removing effect", e);
            }
        }

        try {
            player.addEffect(new MobEffectInstance(EffectRegistry.FIGHT_AGAIN, FIGHT_AGAIN_DURATION, 0));
        } catch (Exception e) {
            LOGGER.error("Error adding FightAgain effect", e);
        }

        try {
            player.sendSystemMessage(
                    Component.literal("§d§l万晦转生之环 §7- 「百战无伤」触发! 清除所有效果，获得30秒再战天荒")
            );
        } catch (Exception e) {
            LOGGER.warn("Error sending system message", e);
        }

        try {
            player.level().broadcastEntityEvent(player, (byte) 35);
        } catch (Exception e) {
            LOGGER.warn("Error broadcasting entity event", e);
        }
    }

    private static boolean hasWanHuiRingInInventory(Player player) {
        return AccessoryChecks.isEquipped(player, ItemRegistry.WAN_HUI_RING.get());
    }

    @SuppressWarnings("unchecked")
    private static int countNegativeEffects(Player player) {
        int count = 0;
        for (var effect : NEGATIVE_EFFECTS) {
            try {
                if (player.hasEffect(effect)) {
                    count++;
                }
            } catch (Exception e) {
                LOGGER.warn("Error checking effect", e);
            }
        }
        return count;
    }
}
