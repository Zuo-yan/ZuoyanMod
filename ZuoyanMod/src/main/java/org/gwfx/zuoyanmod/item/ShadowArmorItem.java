package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class ShadowArmorItem extends Item {

    public ShadowArmorItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.literal("§6§l圣辉套装·§5§l暗影头盔"));
        tooltip.accept(Component.literal("§7§o被动：夜视 + 水下呼吸"));
        tooltip.accept(Component.literal("§7§o生命低于50%时获得失明效果"));
        tooltip.accept(Component.literal("§7§o暗影之刃：拥有失明时攻击有30%概率触发，造成50%额外伤害，冷却10秒"));
        tooltip.accept(Component.literal("§7§o暗影侵蚀：拥有失明时攻击使敌方获得失明效果（持续5秒）"));
        tooltip.accept(Component.literal("§7§o暗影庇护：拥有失明时受到伤害有50%概率完全抵挡一次伤害"));
        tooltip.accept(Component.literal("§e§l全套圣辉套装效果："));
        tooltip.accept(Component.literal("§7§o纵横三千界：创造飞行 + 每次攻击造成目标最大生命值10%的伤害"));
    }
}