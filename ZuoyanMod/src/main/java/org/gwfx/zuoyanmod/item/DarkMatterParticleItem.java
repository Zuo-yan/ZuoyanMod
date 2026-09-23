package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class DarkMatterParticleItem extends Item {

    public DarkMatterParticleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.literal("§5暗物质粒子"));
        tooltip.accept(Component.literal("§7虚空共振泵在末地悬空处共振的产物"));
        tooltip.accept(Component.literal("§79 个可聚合成 1 个暗物质"));
    }
}
