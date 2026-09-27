package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import org.gwfx.zuoyanmod.util.AccessoryChecks;

import java.util.List;

public class CounterBeltItem extends Item {

    public CounterBeltItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.zuoyanmod.counter_belt.desc1"));
        tooltip.add(Component.translatable("item.zuoyanmod.counter_belt.desc2"));
        tooltip.add(Component.translatable("item.zuoyanmod.counter_belt.desc3"));
        tooltip.add(Component.translatable("item.zuoyanmod.counter_belt.desc4"));
        AccessoryChecks.appendEquipHint(tooltip);
    }
}
