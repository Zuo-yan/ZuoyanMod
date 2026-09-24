package org.gwfx.zuoyanmod.event;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.item.ItemRegistry;
import org.gwfx.zuoyanmod.util.AccessoryChecks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 反击腰带：造成的伤害 ≤ 0.9 点时触发「几曾识干戈」，反打目标 99% 最大生命值。
 * 1.20.1 适配：LivingDamageEvent.Pre→{@code LivingDamageEvent}，
 * getNewDamage/setNewDamage→getAmount/setAmount，hurtServer→hurt。
 */
@Mod.EventBusSubscriber(modid = Zuoyanmod.MODID)
public class CounterBeltEventHandler {

    private static final float DAMAGE_THRESHOLD = 0.9f;
    private static final float DAMAGE_PERCENTAGE = 0.99f;

    private static final Map<UUID, Boolean> IS_COUNTERING = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.level().isClientSide) return;

        if (IS_COUNTERING.getOrDefault(attacker.getUUID(), false)) {
            return;
        }

        if (!hasCounterBeltInInventory(attacker)) return;

        float damageAmount = event.getAmount();

        if (damageAmount <= DAMAGE_THRESHOLD) {
            IS_COUNTERING.put(attacker.getUUID(), true);
            try {
                LivingEntity target = event.getEntity();
                float maxHealth = target.getMaxHealth();
                float counterDamage = maxHealth * DAMAGE_PERCENTAGE;

                target.hurt(event.getSource(), counterDamage);

                attacker.sendSystemMessage(
                        Component.literal("§6§l反击腰带 §7- 「几曾识干戈」激活! 对目标造成 §c" + Math.round(counterDamage) + " §7点伤害")
                );
            } finally {
                IS_COUNTERING.remove(attacker.getUUID());
            }
        }
    }

    private static boolean hasCounterBeltInInventory(Player player) {
        return AccessoryChecks.isEquipped(player, ItemRegistry.COUNTER_BELT.get());
    }
}
