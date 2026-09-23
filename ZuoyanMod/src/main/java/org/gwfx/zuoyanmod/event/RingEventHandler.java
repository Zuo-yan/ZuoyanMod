package org.gwfx.zuoyanmod.event;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.item.ItemRegistry;
import org.gwfx.zuoyanmod.util.AccessoryChecks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Zuoyanmod.MODID)
public class RingEventHandler {

    private static final Identifier HEALTH_BONUS_ID = Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "ring_health_bonus");

    private static final Map<UUID, Integer> KILL_COUNTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> HEALTH_BONUS_VALUES = new ConcurrentHashMap<>();

    private static final float HEALTH_INCREASE_PER_KILL = 0.20f;
    private static final float DAMAGE_PENALTY_PER_KILL = 0.10f;

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player killer)) return;
        if (killer.level().isClientSide()) return;

        if (!hasRingInInventory(killer)) return;

        LivingEntity target = event.getEntity();
        if (target instanceof Player || target instanceof net.minecraft.world.entity.Mob) {
            int currentKills = KILL_COUNTS.getOrDefault(killer.getUUID(), 0);
            currentKills++;
            KILL_COUNTS.put(killer.getUUID(), currentKills);

            float baseHealth = (float) killer.getAttributeBaseValue(Attributes.MAX_HEALTH);
            float healthIncrease = baseHealth * HEALTH_INCREASE_PER_KILL;
            float currentBonus = HEALTH_BONUS_VALUES.getOrDefault(killer.getUUID(), 0.0f);
            float newBonus = currentBonus + healthIncrease;
            HEALTH_BONUS_VALUES.put(killer.getUUID(), newBonus);

            updateHealthModifier(killer, newBonus);
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.level().isClientSide()) return;

        if (hasRingInInventory(attacker)) {
            int killCount = KILL_COUNTS.getOrDefault(attacker.getUUID(), 0);
            if (killCount > 0) {
                float penalty = 1.0f - (killCount * DAMAGE_PENALTY_PER_KILL);
                penalty = Math.max(0.1f, penalty);
                event.setNewDamage(event.getNewDamage() * penalty);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        boolean hasRing = hasRingInInventory(player);
        boolean hadRing = KILL_COUNTS.containsKey(player.getUUID());

        if (!hasRing && hadRing) {
            removeRingEffects(player);
        } else if (hasRing && !hadRing) {
            initializeRing(player);
        }
    }

    private static boolean hasRingInInventory(Player player) {
        return AccessoryChecks.isEquipped(player, ItemRegistry.RING_OF_KILLS.get());
    }

    private static void updateHealthModifier(Player player, float bonus) {
        var attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;
        attr.removeModifier(HEALTH_BONUS_ID);
        if (bonus > 0) {
            attr.addTransientModifier(new AttributeModifier(HEALTH_BONUS_ID, bonus, AttributeModifier.Operation.ADD_VALUE));
            player.heal(bonus);
        }
    }

    private static void removeRingEffects(Player player) {
        var attr = player.getAttribute(Attributes.MAX_HEALTH);
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
