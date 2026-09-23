package org.gwfx.zuoyanmod.event;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.item.CounterBeltItem;
import org.gwfx.zuoyanmod.item.ItemRegistry;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Zuoyanmod.MODID)
public class CounterBeltEventHandler {

    private static final float DAMAGE_THRESHOLD = 0.9f;
    private static final float DAMAGE_PERCENTAGE = 0.99f;

    private static final Map<UUID, Boolean> IS_COUNTERING = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.level().isClientSide()) return;

        if (IS_COUNTERING.getOrDefault(attacker.getUUID(), false)) {
            return;
        }

        if (!hasCounterBeltInInventory(attacker)) return;

        float damageAmount = event.getNewDamage();

        if (damageAmount <= DAMAGE_THRESHOLD) {
            IS_COUNTERING.put(attacker.getUUID(), true);
            try {
                LivingEntity target = event.getEntity();
                float maxHealth = target.getMaxHealth();
                float counterDamage = maxHealth * DAMAGE_PERCENTAGE;

                if (target.level() instanceof ServerLevel serverLevel) {
                    target.hurtServer(serverLevel, event.getSource(), counterDamage);
                }

                attacker.sendSystemMessage(
                        Component.literal("§6§l反击腰带 §7- 「几曾识干戈」激活! 对目标造成 §c" + Math.round(counterDamage) + " §7点伤害")
                );
            } finally {
                IS_COUNTERING.remove(attacker.getUUID());
            }
        }
    }

    private static boolean hasCounterBeltInInventory(Player player) {
        if (player.getMainHandItem().is(ItemRegistry.COUNTER_BELT.get())) return true;
        if (player.getOffhandItem().is(ItemRegistry.COUNTER_BELT.get())) return true;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(ItemRegistry.COUNTER_BELT.get())) return true;
        }
        return false;
    }
}
