package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import org.gwfx.zuoyanmod.util.AccessoryChecks;

import java.util.function.Consumer;

public class CounterBeltItem extends Item {

    public CounterBeltItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.literal("§6反击腰带"));
        tooltip.accept(Component.literal("§7当单次伤害≤0.9时"));
        tooltip.accept(Component.literal("§7激活「几曾识干戈」"));
        tooltip.accept(Component.literal("§7对目标造成99%最大生命值伤害"));
        AccessoryChecks.appendEquipHint(tooltip);
        tooltip.accept(Component.literal("§e§o放入背包即生效"));
    }
}
