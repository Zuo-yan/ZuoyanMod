package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;

import org.gwfx.zuoyanmod.util.AccessoryChecks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class YemengadeVenomFangItem extends Item {

    public YemengadeVenomFangItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.literal("§5耶梦加得的毒牙"));
        tooltip.accept(Component.literal("§7体力未满时受到攻击，有60%概率使攻击者中毒4秒"));
        tooltip.accept(Component.literal("§7若攻击者已带有中毒效果，则触发§c尘世巨蟒§7："));
        tooltip.accept(Component.literal("§7反伤等同于自身护甲值的伤害"));
        AccessoryChecks.appendEquipHint(tooltip);
    }
}
