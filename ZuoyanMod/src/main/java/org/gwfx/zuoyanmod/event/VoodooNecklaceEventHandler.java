package org.gwfx.zuoyanmod.event;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.item.ItemRegistry;
import org.gwfx.zuoyanmod.util.AccessoryChecks;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 千厄噬魂之坠：负面效果越多秒杀概率越高；受击概率获得负面效果。
 * 1.20.1 适配：MobEffect 不再是 Holder（NAUSEA/SLOWNESS→CONFUSION/MOVEMENT_SLOWDOWN）；
 * LivingDamageEvent.Pre→{@code LivingDamageEvent}；hurtServer→hurt。
 */
@Mod.EventBusSubscriber(modid = Zuoyanmod.MODID)
public class VoodooNecklaceEventHandler {

    private static final float EFFECT_TRIGGER_CHANCE = 0.35f;
    private static final int EFFECT_DURATION_TICKS = 500;
    private static final float BASE_INSTA_KILL_CHANCE = 0.0f;
    private static final float INSTA_KILL_PER_EFFECT = 0.01f;
    private static final float MAX_INSTA_KILL_CHANCE = 0.30f;

    private static final MobEffect[] NEGATIVE_EFFECTS = new MobEffect[]{
            MobEffects.BLINDNESS,
            MobEffects.CONFUSION,
            MobEffects.POISON,
            MobEffects.HUNGER,
            MobEffects.WEAKNESS,
            MobEffects.MOVEMENT_SLOWDOWN
    };

    private static final Random RANDOM = new Random();
    private static final ConcurrentHashMap<UUID, Boolean> IS_INSTA_KILLING = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        // 玩家作为攻击者
        if (event.getSource().getEntity() instanceof Player attacker) {
            if (attacker.level().isClientSide) return;

            if (IS_INSTA_KILLING.getOrDefault(attacker.getUUID(), false)) return;

            if (hasVoodooNecklaceInInventory(attacker)) {
                int negativeEffectCount = countNegativeEffects(attacker);
                float instaKillChance = calculateInstaKillChance(negativeEffectCount);

                if (instaKillChance > 0 && RANDOM.nextFloat() < instaKillChance) {
                    IS_INSTA_KILLING.put(attacker.getUUID(), true);
                    try {
                        LivingEntity target = event.getEntity();
                        float maxHealth = target.getMaxHealth();
                        target.hurt(event.getSource(), maxHealth);

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
            if (victim.level().isClientSide) return;

            if (hasVoodooNecklaceInInventory(victim)) {
                if (RANDOM.nextFloat() < EFFECT_TRIGGER_CHANCE) {
                    MobEffect randomEffect = NEGATIVE_EFFECTS[RANDOM.nextInt(NEGATIVE_EFFECTS.length)];
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

    private static int countNegativeEffects(Player player) {
        int count = 0;
        for (MobEffect effect : NEGATIVE_EFFECTS) {
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
