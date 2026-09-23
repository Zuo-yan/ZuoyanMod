package org.gwfx.zuoyanmod.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.effect.EffectRegistry;

/**
 * 曼巴之力（攻击形态）：持有「曼巴之击」效果的攻击直接秒杀目标。
 * 1.20.1 适配：LivingIncomingDamageEvent→{@code LivingHurtEvent}，hurtServer→hurt。
 */
@Mod.EventBusSubscriber(modid = Zuoyanmod.MODID)
public class MambaForceEventHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player attacker) {
            if (attacker.hasEffect(EffectRegistry.MAMBA_FORCE_ATTACK.get())) {
                LivingEntity target = event.getEntity();

                // 先移除效果再结算，防止新的 hurt 事件递归进来再次触发
                attacker.removeEffect(EffectRegistry.MAMBA_FORCE_ATTACK.get());
                event.setCanceled(true);

                target.hurt(event.getSource(), Float.MAX_VALUE);
            }
        }
    }
}
