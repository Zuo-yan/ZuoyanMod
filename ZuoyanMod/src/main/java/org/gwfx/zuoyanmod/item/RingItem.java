package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;

import org.gwfx.zuoyanmod.util.AccessoryChecks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class RingItem extends Item {

    public RingItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("item.zuoyanmod.ring.desc1"));
        tooltip.accept(Component.translatable("item.zuoyanmod.ring.desc2"));
        tooltip.accept(Component.translatable("item.zuoyanmod.ring.desc3"));
        tooltip.accept(Component.translatable("item.zuoyanmod.ring.desc4"));
        tooltip.accept(Component.translatable("item.zuoyanmod.ring.desc5"));
        AccessoryChecks.appendEquipHint(tooltip);
    }
}
