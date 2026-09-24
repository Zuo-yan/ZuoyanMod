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
        tooltip.accept(Component.translatable("item.zuoyanmod.ming_dao_si_ming.desc1"));
        tooltip.accept(Component.translatable("item.zuoyanmod.ming_dao_si_ming.desc2"));
        tooltip.accept(Component.translatable("item.zuoyanmod.ming_dao_si_ming.desc3"));
        tooltip.accept(Component.translatable("item.zuoyanmod.ming_dao_si_ming.desc4"));
        AccessoryChecks.appendEquipHint(tooltip);
    }
}