package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class MingDaoSiMingItem extends net.minecraft.world.item.Item {

    public MingDaoSiMingItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("§6名刀司命"));
        tooltip.add(Component.literal("§7免死一次，濒死时保留生命"));
        tooltip.add(Component.literal("§5一个真正想赢的人，脸上，是不会有笑容的 "));
        tooltip.add(Component.literal("§2冷却:120s"));
        org.gwfx.zuoyanmod.util.AccessoryChecks.appendEquipHint(tooltip);
    }
}
