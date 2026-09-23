package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class WindLeggings extends Item {

    public WindLeggings(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.literal("§6§l圣辉套装·§3§l疾风护腿"));
        tooltip.accept(Component.literal("§7§o被动：抗性提升 II（20%减伤）"));
        tooltip.accept(Component.literal("§7§o生命：+20最大生命值 + 25%移动速度"));
        tooltip.accept(Component.literal("§7§o疾风掠影：生命低于10%时+100%移动速度 + 免疫掉落伤害"));
        tooltip.accept(Component.literal("§e§l全套圣辉套装效果："));
        tooltip.accept(Component.literal("§7§o纵横三千界：创造飞行 + 每次攻击造成目标最大生命值10%的伤害"));
    }
}