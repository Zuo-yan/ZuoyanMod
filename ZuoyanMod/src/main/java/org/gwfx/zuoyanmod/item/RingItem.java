package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;

import org.gwfx.zuoyanmod.util.AccessoryChecks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class RingItem extends Item {

    public RingItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.zuoyanmod.ring.desc1"));
        tooltip.add(Component.translatable("item.zuoyanmod.ring.desc2"));
        tooltip.add(Component.translatable("item.zuoyanmod.ring.desc3"));
        tooltip.add(Component.translatable("item.zuoyanmod.ring.desc4"));
        tooltip.add(Component.translatable("item.zuoyanmod.ring.desc5"));
        AccessoryChecks.appendEquipHint(tooltip);
    }
}
