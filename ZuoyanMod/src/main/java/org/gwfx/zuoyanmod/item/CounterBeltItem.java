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
        tooltip.add(Component.literal("§6反击腰带"));
        tooltip.add(Component.literal("§7当单次伤害≤0.9时"));
        tooltip.add(Component.literal("§7激活「几曾识干戈」"));
        tooltip.add(Component.literal("§7对目标造成99%最大生命值伤害"));
        org.gwfx.zuoyanmod.util.AccessoryChecks.appendEquipHint(tooltip);
    }
}
