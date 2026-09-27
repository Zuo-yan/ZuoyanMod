package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;

import org.gwfx.zuoyanmod.util.AccessoryChecks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class WanHuiRingItem extends Item {

    public WanHuiRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.zuoyanmod.wan_hui_ring.desc1"));
        tooltip.add(Component.translatable("item.zuoyanmod.wan_hui_ring.desc2"));
        tooltip.add(Component.translatable("item.zuoyanmod.wan_hui_ring.desc3"));
        tooltip.add(Component.translatable("item.zuoyanmod.wan_hui_ring.desc4"));
        tooltip.add(Component.translatable("item.zuoyanmod.wan_hui_ring.desc5"));
        tooltip.add(Component.translatable("item.zuoyanmod.wan_hui_ring.desc6"));
        tooltip.add(Component.translatable("item.zuoyanmod.wan_hui_ring.desc7"));
        tooltip.add(Component.translatable("item.zuoyanmod.wan_hui_ring.desc8"));
        AccessoryChecks.appendEquipHint(tooltip);
    }
}
