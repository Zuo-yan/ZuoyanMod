package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 带多行描述的方块物品（如机器"能放什么材料"的说明）。
 * 描述行走 lang 键，文案约定见 DescribedItem。
 *
 * <p>1.20.1 适配：tooltip 是 {@code List<Component>}。</p>
 */
public class DescriptionBlockItem extends BlockItem {
    private final String[] descKeys;

    public DescriptionBlockItem(Block block, Properties properties, String... descKeys) {
        super(block, properties);
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
