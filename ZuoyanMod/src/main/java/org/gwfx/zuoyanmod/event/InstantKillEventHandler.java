package org.gwfx.zuoyanmod.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.effect.EffectRegistry;

/**
 * 消费 {@link org.gwfx.zuoyanmod.effect.InstantKillEffect}：把持有者的下一次攻击换成必杀。
 *
 * <p><b>为什么改名：</b>原类名带 Mamba，但它只处理"一击必杀"这一件事，
 * 和黑曼巴这个梗没有任何代码上的关系（梗只体现在 lang 文案里）。名字按职责取。
 *
 * <p><b>先移除效果再结算</b>：否则 {@code target.hurt} 触发的新一轮事件里
 * 攻击者仍然带着必杀效果，会递归地再消费一次（这是一条真的会套娃的路径）。
 *
 * <p>1.20.1 适配：26.x 的 {@code LivingIncomingDamageEvent} 在这里是结算<b>前</b>的
 * {@code LivingHurtEvent}，{@code hurtServer} 换成 {@code hurt}。
 */
@Mod.EventBusSubscriber(modid = Zuoyanmod.MODID)
public class InstantKillEventHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player attacker) {
            if (attacker.hasEffect(EffectRegistry.INSTANT_KILL.get())) {
                LivingEntity target = event.getEntity();

                // 先移除效果再结算，防止新的 hurt 事件递归进来再次触发
                attacker.removeEffect(EffectRegistry.INSTANT_KILL.get());
                event.setCanceled(true);

                target.hurt(event.getSource(), Float.MAX_VALUE);
            }
        }
    }
}
