package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class RingItem extends Item {

    public RingItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.literal("§6杀戮之戒"));
        tooltip.accept(Component.literal("§7佩戴后每击杀一个生物"));
        tooltip.accept(Component.literal("§7永久提升20%基础生命值"));
        tooltip.accept(Component.literal("§7同时降低10%攻击伤害"));
        tooltip.accept(Component.literal("§7卸下戒指后效果重置"));
        tooltip.accept(Component.literal("§e§o放入背包即生效"));
    }
}
