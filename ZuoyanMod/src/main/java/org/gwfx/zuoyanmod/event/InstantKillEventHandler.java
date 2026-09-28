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
/**
 * 消费 {@link org.gwfx.zuoyanmod.effect.InstantKillEffect}：把持有者的下一次攻击换成必杀。
 *
 * <p><b>为什么改名：</b>原类名带 Mamba，但它只处理"一击必杀"这一件事，
 * 和黑曼巴这个梗没有任何代码上的关系（梗只体现在 lang 文案里）。名字按职责取。
 */
public class InstantKillEventHandler {

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Player attacker) {
            if (attacker.hasEffect(EffectRegistry.INSTANT_KILL)) {
                LivingEntity target = event.getEntity();

                attacker.removeEffect(EffectRegistry.INSTANT_KILL);
                event.setCanceled(true);

                if (target.level() instanceof ServerLevel serverLevel) {
                    target.hurtServer(serverLevel, event.getSource(), Float.MAX_VALUE);
                }
            }
        }
    }
}