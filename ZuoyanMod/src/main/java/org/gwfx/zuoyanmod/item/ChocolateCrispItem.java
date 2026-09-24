package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.gwfx.zuoyanmod.effect.EffectRegistry;
import org.gwfx.zuoyanmod.sound.SoundRegistry;

import java.util.function.Consumer;

public class ChocolateCrispItem extends Item {

    public ChocolateCrispItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);

        if (entity instanceof Player player) {
            // 巧乐兹是一根赌博雪糕：1200 tick 的一击必杀，和 821 tick 的定时处刑同时挂上。
            // 两个时长的差（约 19 秒）就是"力量还在、死期未到"的窗口，
            // 而 821 比 1200 短意味着：力量还没失效，处刑就先到了 —— 这是刻意的，
            // 逼玩家在必杀还有效的时候就去处理那条命（空间锚点之类的保命手段）。
            // 26.x 中 MobEffectInstance 直接接收 DeferredHolder/Holder，无需 .get()
            player.addEffect(new MobEffectInstance(
                    EffectRegistry.INSTANT_KILL,
                    1200,
                    0,
                    false,
                    true,
                    true
            ));
            player.addEffect(new MobEffectInstance(
                    EffectRegistry.HEART_PARALYSIS,
                    821,
                    0,
                    false,
                    true,
                    true
            ));

            if (!level.isClientSide()) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundRegistry.CHILLED_DRINK.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
            }
        }

        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("item.zuoyanmod.chocolate_crisp.desc1"));
        tooltip.accept(Component.translatable("item.zuoyanmod.chocolate_crisp.desc2"));
        tooltip.accept(Component.translatable("item.zuoyanmod.chocolate_crisp.desc3"));
        tooltip.accept(Component.translatable("item.zuoyanmod.chocolate_crisp.desc4"));
    }
}