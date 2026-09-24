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

public class IceTeaItem extends Item {

    public IceTeaItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);

        if (entity instanceof Player player) {
            // 26.x 中 MobEffectInstance 直接接收 DeferredHolder/Holder，无需 .get()
            player.addEffect(new MobEffectInstance(
                    EffectRegistry.MAMBA_FORCE_ATTACK,
                    1200,
                    0,
                    false,
                    true,
                    true
            ));
            player.addEffect(new MobEffectInstance(
                    EffectRegistry.MAMBA_FORCE_DEFENSE,
                    821,
                    0,
                    false,
                    true,
                    true
            ));

            if (!level.isClientSide()) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundRegistry.ICE_TEA_DRINK.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
            }
        }

        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("item.zuoyanmod.ice_tea.desc1"));
        tooltip.accept(Component.translatable("item.zuoyanmod.ice_tea.desc2"));
        tooltip.accept(Component.translatable("item.zuoyanmod.ice_tea.desc3"));
        tooltip.accept(Component.translatable("item.zuoyanmod.ice_tea.desc4"));
    }
}