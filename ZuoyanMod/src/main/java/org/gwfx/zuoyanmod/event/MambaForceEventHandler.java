package org.gwfx.zuoyanmod.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.effect.EffectRegistry;

@EventBusSubscriber(modid = Zuoyanmod.MODID)
public class MambaForceEventHandler {

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Player attacker) {
            if (attacker.hasEffect(EffectRegistry.MAMBA_FORCE_ATTACK)) {
                LivingEntity target = event.getEntity();

                attacker.removeEffect(EffectRegistry.MAMBA_FORCE_ATTACK);
                event.setCanceled(true);

                if (target.level() instanceof ServerLevel serverLevel) {
                    target.hurtServer(serverLevel, event.getSource(), Float.MAX_VALUE);
                }
            }
        }
    }
}