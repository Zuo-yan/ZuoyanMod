package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class ShengTianChestplate extends Item {

    public ShengTianChestplate(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("item.zuoyanmod.sheng_tian_chestplate.desc1"));
        tooltip.accept(Component.translatable("item.zuoyanmod.sheng_tian_chestplate.desc2"));
        tooltip.accept(Component.translatable("item.zuoyanmod.sheng_tian_chestplate.desc3"));
        tooltip.accept(Component.translatable("item.zuoyanmod.sheng_tian_chestplate.desc4"));
        tooltip.accept(Component.translatable("item.zuoyanmod.sheng_tian_chestplate.desc5"));
        tooltip.accept(Component.translatable("item.zuoyanmod.sheng_tian_chestplate.desc6"));
        tooltip.accept(Component.translatable("item.zuoyanmod.sheng_tian_chestplate.desc7"));
    }
}