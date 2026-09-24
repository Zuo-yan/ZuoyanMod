package org.gwfx.zuoyanmod.event;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.item.ItemRegistry;
import org.gwfx.zuoyanmod.util.AccessoryChecks;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Zuoyanmod.MODID)
public class VoodooNecklaceEventHandler {

    private static final float EFFECT_TRIGGER_CHANCE = 0.35f;
    private static final int EFFECT_DURATION_TICKS = 500;
    private static final float BASE_INSTA_KILL_CHANCE = 0.0f;
    private static final float INSTA_KILL_PER_EFFECT = 0.01f;
    private static final float MAX_INSTA_KILL_CHANCE = 0.30f;

    private static final net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>[] NEGATIVE_EFFECTS = new net.minecraft.core.Holder[]{
            MobEffects.BLINDNESS,
            MobEffects.NAUSEA,
            MobEffects.POISON,
            MobEffects.HUNGER,
            MobEffects.WEAKNESS,
            MobEffects.SLOWNESS
    };

    private static final Random RANDOM = new Random();
    private static final ConcurrentHashMap<UUID, Boolean> IS_INSTA_KILLING = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        // 玩家作为攻击者
        if (event.getSource().getEntity() instanceof Player attacker) {
            if (attacker.level().isClientSide()) return;

            if (IS_INSTA_KILLING.getOrDefault(attacker.getUUID(), false)) return;

            if (hasVoodooNecklaceInInventory(attacker)) {
                int negativeEffectCount = countNegativeEffects(attacker);
                float instaKillChance = calculateInstaKillChance(negativeEffectCount);

                if (instaKillChance > 0 && RANDOM.nextFloat() < instaKillChance) {
                    IS_INSTA_KILLING.put(attacker.getUUID(), true);
                    try {
                        LivingEntity target = event.getEntity();
                        float maxHealth = target.getMaxHealth();
                        if (target.level() instanceof ServerLevel serverLevel) {
                            target.hurtServer(serverLevel, event.getSource(), maxHealth);
                        }

                        attacker.sendSystemMessage(
                                Component.translatable("message.zuoyanmod.voodoo_necklace.instakill",
                                        negativeEffectCount, Math.round(instaKillChance * 100))
                        );
                    } finally {
                        IS_INSTA_KILLING.remove(attacker.getUUID());
                    }
                }
            }
        }

        // 玩家作为受击者
        if (event.getEntity() instanceof Player victim) {
            if (victim.level().isClientSide()) return;

            if (hasVoodooNecklaceInInventory(victim)) {
                if (RANDOM.nextFloat() < EFFECT_TRIGGER_CHANCE) {
                    @SuppressWarnings("unchecked")
                    var randomEffect = NEGATIVE_EFFECTS[RANDOM.nextInt(NEGATIVE_EFFECTS.length)];
                    victim.addEffect(new MobEffectInstance(randomEffect, EFFECT_DURATION_TICKS, 0));
                    victim.sendSystemMessage(
                            Component.translatable("message.zuoyanmod.voodoo_necklace.gain_effect")
                    );
                }
            }
        }
    }

    private static boolean hasVoodooNecklaceInInventory(Player player) {
        return AccessoryChecks.isEquipped(player, ItemRegistry.VOODOO_NECKLACE.get());
    }

    @SuppressWarnings("unchecked")
    private static int countNegativeEffects(Player player) {
        int count = 0;
        for (var effect : NEGATIVE_EFFECTS) {
            if (player.hasEffect(effect)) {
                count++;
            }
        }
        return count;
    }

    private static float calculateInstaKillChance(int negativeEffectCount) {
        float chance = BASE_INSTA_KILL_CHANCE + (negativeEffectCount * INSTA_KILL_PER_EFFECT);
        return Math.min(chance, MAX_INSTA_KILL_CHANCE);
    }
}
