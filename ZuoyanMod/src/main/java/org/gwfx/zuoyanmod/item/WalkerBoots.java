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
        tooltip.accept(Component.literal("§6§l圣辉套装·§f§l行者之靴"));
        tooltip.accept(Component.literal("§7§o被动：抗火 + 海豚的恩惠"));
        tooltip.accept(Component.literal("§7§o行至空元：满血时受到超过45%最大生命值的伤害"));
        tooltip.accept(Component.literal("§7§o将自身与攻击者随机传送至20格范围内"));
        tooltip.accept(Component.literal("§e§l全套圣辉套装效果："));
        tooltip.accept(Component.literal("§7§o纵横三千界：创造飞行 + 每次攻击造成目标最大生命值10%的伤害"));
    }
}