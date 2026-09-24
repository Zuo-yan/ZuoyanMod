package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class RingItem extends net.minecraft.world.item.Item {

    public RingItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("§6杀戮之戒"));
        tooltip.add(Component.literal("§7佩戴后每击杀一个生物"));
        tooltip.add(Component.literal("§7永久提升20%基础生命值"));
        tooltip.add(Component.literal("§7同时降低10%攻击伤害"));
        tooltip.add(Component.literal("§7卸下戒指后效果重置"));
        org.gwfx.zuoyanmod.util.AccessoryChecks.appendEquipHint(tooltip);
    }
}
