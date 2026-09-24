package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;

import org.gwfx.zuoyanmod.util.AccessoryChecks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class WanHuiRingItem extends Item {

    public WanHuiRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("item.zuoyanmod.wan_hui_ring.desc1"));
        tooltip.accept(Component.translatable("item.zuoyanmod.wan_hui_ring.desc2"));
        tooltip.accept(Component.translatable("item.zuoyanmod.wan_hui_ring.desc3"));
        tooltip.accept(Component.translatable("item.zuoyanmod.wan_hui_ring.desc4"));
        tooltip.accept(Component.translatable("item.zuoyanmod.wan_hui_ring.desc5"));
        tooltip.accept(Component.translatable("item.zuoyanmod.wan_hui_ring.desc6"));
        tooltip.accept(Component.translatable("item.zuoyanmod.wan_hui_ring.desc7"));
        tooltip.accept(Component.translatable("item.zuoyanmod.wan_hui_ring.desc8"));
        AccessoryChecks.appendEquipHint(tooltip);
    }
}
