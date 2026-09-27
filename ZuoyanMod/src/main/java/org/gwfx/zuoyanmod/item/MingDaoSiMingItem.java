package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import org.gwfx.zuoyanmod.util.AccessoryChecks;

import java.util.List;

public class MingDaoSiMingItem extends Item {

    public MingDaoSiMingItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.zuoyanmod.ming_dao_si_ming.desc1"));
        tooltip.add(Component.translatable("item.zuoyanmod.ming_dao_si_ming.desc2"));
        tooltip.add(Component.translatable("item.zuoyanmod.ming_dao_si_ming.desc3"));
        tooltip.add(Component.translatable("item.zuoyanmod.ming_dao_si_ming.desc4"));
        AccessoryChecks.appendEquipHint(tooltip::add);
    }
}