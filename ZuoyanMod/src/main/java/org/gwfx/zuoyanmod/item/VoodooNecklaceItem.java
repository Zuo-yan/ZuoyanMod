package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class VoodooNecklaceItem extends net.minecraft.world.item.Item {

    public VoodooNecklaceItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("§5千厄噬魂之坠"));
        tooltip.add(Component.literal("§7受到攻击时有35%概率获得负面效果"));
        tooltip.add(Component.literal("§7每拥有一项负面效果，秒杀概率+1%"));
        tooltip.add(Component.literal("§7负面效果: 失明、反胃、中毒、饥饿、虚弱、缓慢"));
        org.gwfx.zuoyanmod.util.AccessoryChecks.appendEquipHint(tooltip);
    }
}
