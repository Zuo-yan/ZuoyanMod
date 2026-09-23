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
        tooltip.accept(Component.literal("§6§l圣辉套装·胜天战甲"));
        tooltip.accept(Component.literal("§7§o被动：生命恢复 II + 力量 V"));
        tooltip.accept(Component.literal("§7§o护盾：获得等同于最大生命值的伤害吸收，每10秒刷新"));
        tooltip.accept(Component.literal("§7§o不屈：生命低于50%时获得30%减伤"));
        tooltip.accept(Component.literal("§7§o胜天之怒：生命高于50%时获得30%增伤"));
        tooltip.accept(Component.literal("§e§l全套圣辉套装效果："));
        tooltip.accept(Component.literal("§7§o纵横三千界：创造飞行 + 每次攻击造成目标最大生命值10%的伤害"));
    }
}