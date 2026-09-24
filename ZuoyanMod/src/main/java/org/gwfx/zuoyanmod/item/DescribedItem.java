package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 带多行描述的普通物品：描述行走 lang 键（appendHoverText 顺序输出）。
 * 文案约定：只说"是什么、能做什么"，不写警告句、不堆裸数值。
 *
 * <p>1.20.1 适配：{@code appendHoverText(ItemStack, Level, List<Component>, TooltipFlag)}
 * （26.x 换成了 TooltipContext + TooltipDisplay + Consumer）。</p>
 */
public class DescribedItem extends Item {
    private final String[] descKeys;

    public DescribedItem(Properties properties, String... descKeys) {
        super(properties);
        this.descKeys = descKeys;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        for (String key : this.descKeys) {
            tooltip.add(Component.translatable(key));
        }
    }
}
