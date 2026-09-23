package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;

import org.gwfx.zuoyanmod.util.AccessoryChecks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class VoodooNecklaceItem extends Item {

    public VoodooNecklaceItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.literal("§5千厄噬魂之坠"));
        tooltip.accept(Component.literal("§7受到攻击时有35%概率获得负面效果"));
        tooltip.accept(Component.literal("§7每拥有一项负面效果，秒杀概率+1%"));
        tooltip.accept(Component.literal("§7负面效果: 失明、反胃、中毒、饥饿、虚弱、缓慢"));
        AccessoryChecks.appendEquipHint(tooltip);
    }
}
