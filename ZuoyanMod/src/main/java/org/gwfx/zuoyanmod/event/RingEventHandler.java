package org.gwfx.zuoyanmod.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.item.ItemRegistry;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 戒指（杀戒之环）：每次击杀 +20% 基础生命上限，但每次击杀 -10% 造成伤害。
 * 1.20.1 适配：AttributeModifier 的 ID 由 Identifier 换回 UUID；
 * LivingDamageEvent.Pre→{@code LivingDamageEvent}（getAmount/setAmount）。
 */
@Mod.EventBusSubscriber(modid = Zuoyanmod.MODID)
public class RingEventHandler {

    /** 1.20.1 的属性修改器用 UUID 标识（26.x 是 ResourceLocation） */
    private static final UUID HEALTH_BONUS_ID = UUID.fromString("9e1c3d4a-5f6b-4c7d-8a8b-3c4d5e6f7a82");

    private static final Map<UUID, Integer> KILL_COUNTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> HEALTH_BONUS_VALUES = new ConcurrentHashMap<>();

    private static final float HEALTH_INCREASE_PER_KILL = 0.20f;
    private static final float DAMAGE_PENALTY_PER_KILL = 0.10f;

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player killer)) return;
        if (killer.level().isClientSide) return;

        if (!hasRingInInventory(killer)) return;

        LivingEntity target = event.getEntity();
        if (target instanceof Player || target instanceof net.minecraft.world.entity.Mob) {
            int currentKills = KILL_COUNTS.getOrDefault(killer.getUUID(), 0);
            currentKills++;
            KILL_COUNTS.put(killer.getUUID(), currentKills);

            AttributeInstance maxHealth = killer.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth == null) return;
            float baseHealth = (float) maxHealth.getBaseValue();
            float healthIncrease = baseHealth * HEALTH_INCREASE_PER_KILL;
            float currentBonus = HEALTH_BONUS_VALUES.getOrDefault(killer.getUUID(), 0.0f);
            float newBonus = currentBonus + healthIncrease;
            HEALTH_BONUS_VALUES.put(killer.getUUID(), newBonus);

            updateHealthModifier(killer, newBonus);
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.level().isClientSide) return;

        if (hasRingInInventory(attacker)) {
            int killCount = KILL_COUNTS.getOrDefault(attacker.getUUID(), 0);
            if (killCount > 0) {
                float penalty = 1.0f - (killCount * DAMAGE_PENALTY_PER_KILL);
                penalty = Math.max(0.1f, penalty);
                event.setAmount(event.getAmount() * penalty);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide) return;

        boolean hasRing = hasRingInInventory(player);
        boolean hadRing = KILL_COUNTS.containsKey(player.getUUID());

        if (!hasRing && hadRing) {
            removeRingEffects(player);
        } else if (hasRing && !hadRing) {
            initializeRing(player);
        }
    }

    private static boolean hasRingInInventory(Player player) {
        if (player.getMainHandItem().is(ItemRegistry.RING_OF_KILLS.get())) return true;
        if (player.getOffhandItem().is(ItemRegistry.RING_OF_KILLS.get())) return true;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(ItemRegistry.RING_OF_KILLS.get())) return true;
        }
        return false;
    }

    private static void updateHealthModifier(Player player, float bonus) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;
        attr.removeModifier(HEALTH_BONUS_ID);
        if (bonus > 0) {
            attr.addTransientModifier(new AttributeModifier(HEALTH_BONUS_ID, "ring_health_bonus", bonus, AttributeModifier.Operation.ADDITION));
            player.heal(bonus);
        }
    }

    private static void removeRingEffects(Player player) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) attr.removeModifier(HEALTH_BONUS_ID);

        float newMax = player.getMaxHealth();
        if (player.getHealth() > newMax) {
            player.setHealth(newMax);
        }

        KILL_COUNTS.remove(player.getUUID());
        HEALTH_BONUS_VALUES.remove(player.getUUID());
    }

    private static void initializeRing(Player player) {
        KILL_COUNTS.put(player.getUUID(), 0);
        HEALTH_BONUS_VALUES.put(player.getUUID(), 0.0f);
    }

    public static int getKillCount(Player player) {
        return KILL_COUNTS.getOrDefault(player.getUUID(), 0);
    }
}
