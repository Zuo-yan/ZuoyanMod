package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class WalkerBoots extends Item {

    public WalkerBoots(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("item.zuoyanmod.walker_boots.desc1"));
        tooltip.accept(Component.translatable("item.zuoyanmod.walker_boots.desc2"));
        tooltip.accept(Component.translatable("item.zuoyanmod.walker_boots.desc3"));
        tooltip.accept(Component.translatable("item.zuoyanmod.walker_boots.desc4"));
        tooltip.accept(Component.translatable("item.zuoyanmod.walker_boots.desc5"));
        tooltip.accept(Component.translatable("item.zuoyanmod.walker_boots.desc6"));
    }
}