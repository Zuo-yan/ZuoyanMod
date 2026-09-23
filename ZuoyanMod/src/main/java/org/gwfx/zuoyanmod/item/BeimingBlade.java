package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class BeimingBlade extends Item {

    public BeimingBlade(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.literal("§1§l北冥狂刃"));
        tooltip.accept(Component.literal("§7§o战之狂热：攻击敌方时进入狂热状态"));
        tooltip.accept(Component.literal("§7§o每次攻击提升最大生命值并扣除自身10%生命值"));
        tooltip.accept(Component.literal("§7§o对目标造成自身已损生命值60%的额外伤害"));
        tooltip.accept(Component.literal("§7§o停止攻击10秒后退出狂热状态"));
    }
}