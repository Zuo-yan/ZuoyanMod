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
        tooltip.add(Component.translatable("item.zuoyanmod.walker_boots.desc1"));
        tooltip.add(Component.translatable("item.zuoyanmod.walker_boots.desc2"));
        tooltip.add(Component.translatable("item.zuoyanmod.walker_boots.desc3"));
        tooltip.add(Component.translatable("item.zuoyanmod.walker_boots.desc4"));
        tooltip.add(Component.translatable("item.zuoyanmod.wind_leggings.desc5"));
        tooltip.add(Component.translatable("item.zuoyanmod.wind_leggings.desc6"));
    }
}
