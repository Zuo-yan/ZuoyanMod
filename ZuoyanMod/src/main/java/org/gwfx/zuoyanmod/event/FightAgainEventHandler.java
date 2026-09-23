package org.gwfx.zuoyanmod.event;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.effect.EffectRegistry;

@EventBusSubscriber(modid = Zuoyanmod.MODID)
public class FightAgainEventHandler {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.level().isClientSide()) return;

        if (attacker.hasEffect(EffectRegistry.FIGHT_AGAIN)) {
            float originalDamage = event.getNewDamage();
            event.setNewDamage(originalDamage * 2.0f);
        }
    }
}
