package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class CounterBeltItem extends net.minecraft.world.item.Item {

    public CounterBeltItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.zuoyanmod.counter_belt.desc1"));
        tooltip.add(Component.translatable("item.zuoyanmod.counter_belt.desc2"));
        tooltip.add(Component.translatable("item.zuoyanmod.counter_belt.desc3"));
        tooltip.add(Component.translatable("item.zuoyanmod.counter_belt.desc4"));
        org.gwfx.zuoyanmod.util.AccessoryChecks.appendEquipHint(tooltip);
    }
}
