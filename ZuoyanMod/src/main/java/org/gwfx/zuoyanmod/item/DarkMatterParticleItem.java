package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class DarkMatterParticleItem extends net.minecraft.world.item.Item {

    public DarkMatterParticleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("§5暗物质粒子"));
        tooltip.add(Component.literal("§7虚空共振泵在末地悬空处共振的产物"));
        tooltip.add(Component.literal("§79 个可聚合成 1 个暗物质"));
    }
}
