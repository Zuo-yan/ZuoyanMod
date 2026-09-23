package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class WanHuiRingItem extends net.minecraft.world.item.Item {

    public WanHuiRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("§d万晦转生之环"));
        tooltip.add(Component.literal("§7当拥有任意三项负面效果时"));
        tooltip.add(Component.literal("§7触发「百战无伤」效果"));
        tooltip.add(Component.literal("§7负面效果: 失明、反胃、中毒、饥饿、虚弱、缓慢、挖掘疲劳"));
        tooltip.add(Component.literal("§7清除所有效果并获得再战天荒"));
        tooltip.add(Component.literal("§7再战天荒: §a+50%攻击力 §e+30%移动速度"));
        tooltip.add(Component.literal("§7再战天荒持续时间: §630秒"));
        tooltip.add(Component.literal("§7冷却时间: §c60秒"));
        tooltip.add(Component.literal("§e§o放入背包即生效"));
    }
}
