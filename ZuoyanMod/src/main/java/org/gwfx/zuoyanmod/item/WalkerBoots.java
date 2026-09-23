package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class WalkerBoots extends ArmorItem {

    public WalkerBoots(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("§6§l圣辉套装·§f§l行者之靴"));
        tooltip.add(Component.literal("§7§o被动：抗火 + 海豚的恩惠"));
        tooltip.add(Component.literal("§7§o行至空元：满血时受到超过45%最大生命值的伤害"));
        tooltip.add(Component.literal("§7§o将自身与攻击者随机传送至20格范围内"));
        tooltip.add(Component.literal("§e§l全套圣辉套装效果："));
        tooltip.add(Component.literal("§7§o纵横三千界：创造飞行 + 每次攻击造成目标最大生命值10%的伤害"));
    }
}
