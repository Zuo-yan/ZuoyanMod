package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class WindLeggings extends ArmorItem {

    public WindLeggings(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("§6§l圣辉套装·§3§l疾风护腿"));
        tooltip.add(Component.literal("§7§o被动：抗性提升 II（20%减伤）"));
        tooltip.add(Component.literal("§7§o生命：+20最大生命值 + 25%移动速度"));
        tooltip.add(Component.literal("§7§o疾风掠影：生命低于10%时+100%移动速度 + 免疫掉落伤害"));
        tooltip.add(Component.literal("§e§l全套圣辉套装效果："));
        tooltip.add(Component.literal("§7§o纵横三千界：创造飞行 + 每次攻击造成目标最大生命值10%的伤害"));
    }
}
