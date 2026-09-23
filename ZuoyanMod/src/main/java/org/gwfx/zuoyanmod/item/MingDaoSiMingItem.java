package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import org.gwfx.zuoyanmod.util.AccessoryChecks;

import java.util.function.Consumer;

public class MingDaoSiMingItem extends Item {

    public MingDaoSiMingItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.literal("§6名刀司命"));
        tooltip.accept(Component.literal("§7免死一次，濒死时保留生命"));
        tooltip.accept(Component.literal("§5一个真正想赢的人，脸上，是不会有笑容的 "));
        tooltip.accept(Component.literal("§2冷却:120s"));
        AccessoryChecks.appendEquipHint(tooltip);
    }
}