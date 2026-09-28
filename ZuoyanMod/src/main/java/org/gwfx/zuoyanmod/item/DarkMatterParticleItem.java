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
        tooltip.accept(Component.translatable("item.zuoyanmod.dark_matter_particle.desc1"));
        tooltip.accept(Component.translatable("item.zuoyanmod.dark_matter_particle.desc2"));
        tooltip.accept(Component.translatable("item.zuoyanmod.dark_matter_particle.desc3"));
    }
}
