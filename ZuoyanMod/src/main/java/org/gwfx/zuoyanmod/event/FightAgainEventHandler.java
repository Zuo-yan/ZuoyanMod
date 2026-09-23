package org.gwfx.zuoyanmod.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.effect.EffectRegistry;

/**
 * 「再战天荒」效果：持有者的伤害翻倍。
 * 1.20.1 适配：LivingDamageEvent.Pre→{@code LivingDamageEvent}，
 * getNewDamage/setNewDamage→getAmount/setAmount。
 */
@Mod.EventBusSubscriber(modid = Zuoyanmod.MODID)
public class FightAgainEventHandler {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.level().isClientSide) return;

        if (attacker.hasEffect(EffectRegistry.FIGHT_AGAIN.get())) {
            float originalDamage = event.getAmount();
            event.setAmount(originalDamage * 2.0f);
        }
    }
}
