package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class WanHuiRingItem extends net.minecraft.world.item.Item {

    public WanHuiRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.zuoyanmod.wan_hui_ring.desc1"));
        tooltip.add(Component.translatable("item.zuoyanmod.wan_hui_ring.desc2"));
        tooltip.add(Component.translatable("item.zuoyanmod.wan_hui_ring.desc3"));
        tooltip.add(Component.translatable("item.zuoyanmod.wan_hui_ring.desc4"));
        tooltip.add(Component.translatable("item.zuoyanmod.wan_hui_ring.desc5"));
        tooltip.add(Component.translatable("item.zuoyanmod.wan_hui_ring.desc6"));
        tooltip.add(Component.translatable("item.zuoyanmod.wan_hui_ring.desc7"));
        tooltip.add(Component.translatable("item.zuoyanmod.wan_hui_ring.desc8"));
        org.gwfx.zuoyanmod.util.AccessoryChecks.appendEquipHint(tooltip);
    }
}
