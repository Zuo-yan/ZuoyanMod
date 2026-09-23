package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;

import org.gwfx.zuoyanmod.util.AccessoryChecks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class WanHuiRingItem extends Item {

    public WanHuiRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.literal("§d万晦转生之环"));
        tooltip.accept(Component.literal("§7当拥有任意三项负面效果时"));
        tooltip.accept(Component.literal("§7触发「百战无伤」效果"));
        tooltip.accept(Component.literal("§7负面效果: 失明、反胃、中毒、饥饿、虚弱、缓慢、挖掘疲劳"));
        tooltip.accept(Component.literal("§7清除所有效果并获得再战天荒"));
        tooltip.accept(Component.literal("§7再战天荒: §a+50%攻击力 §e+30%移动速度"));
        tooltip.accept(Component.literal("§7再战天荒持续时间: §630秒"));
        tooltip.accept(Component.literal("§7冷却时间: §c60秒"));
        AccessoryChecks.appendEquipHint(tooltip);
    }
}
