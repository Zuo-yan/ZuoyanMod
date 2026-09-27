package org.gwfx.zuoyanmod.item;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 紫金靴子：材质在注册时传入（1.21.1 的 ArmorMaterial 是注册表对象，不再塞进 Properties）。 */
public class WalkerBoots extends ArmorItem {

    public WalkerBoots(Holder<ArmorMaterial> material, Properties properties) {
        super(material, ArmorItem.Type.BOOTS, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.zuoyanmod.walker_boots.desc1"));
        tooltip.add(Component.translatable("item.zuoyanmod.walker_boots.desc2"));
        tooltip.add(Component.translatable("item.zuoyanmod.walker_boots.desc3"));
        tooltip.add(Component.translatable("item.zuoyanmod.walker_boots.desc4"));
        tooltip.add(Component.translatable("item.zuoyanmod.walker_boots.desc5"));
        tooltip.add(Component.translatable("item.zuoyanmod.walker_boots.desc6"));
    }
}
