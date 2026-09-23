package org.gwfx.zuoyanmod.event;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.item.ItemRegistry;
import org.gwfx.zuoyanmod.util.AccessoryChecks;

@EventBusSubscriber(modid = Zuoyanmod.MODID)
public final class YemengadeVenomFangEventHandler {

    private YemengadeVenomFangEventHandler() {}

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        if (!hasVenomFangInInventory(player)) return;

        Entity directEntity = event.getSource().getEntity();
        if (!(directEntity instanceof LivingEntity attacker)) return;

        // 体力未满时受到攻击，有60%概率使攻击者中毒4秒
        if (player.getHealth() < player.getMaxHealth()) {
            if (player.getRandom().nextFloat() < 0.60f) {
                attacker.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 4, 0));
                player.sendSystemMessage(Component.literal("§5耶梦加得的毒牙§7：攻击者被毒牙侵蚀，中毒 4 秒"));
            }
        }

        // 若攻击者已带有中毒效果，则反伤等同于自身护甲值的伤害
        if (attacker.hasEffect(MobEffects.POISON)) {
            float armorDamage = (float) player.getArmorValue();
            if (armorDamage > 0.0f && attacker.level() instanceof ServerLevel serverLevel) {
                attacker.hurtServer(serverLevel, player.damageSources().thorns(player), armorDamage);
                player.sendSystemMessage(Component.literal("§c尘世巨蟒§7：反伤 §c" + armorDamage + "§7 点"));
            }
        }
    }

    private static boolean hasVenomFangInInventory(Player player) {
        return AccessoryChecks.isEquipped(player, ItemRegistry.YEMENGADE_VENOM_FANG.get());
    }
}
