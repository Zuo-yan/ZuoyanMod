package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/**
 * 带多行描述的方块物品（如机器"能放什么材料"的说明）。
 * 描述行走 lang 键，文案约定见 DescribedItem。
 */
public class DescriptionBlockItem extends BlockItem {
    private final String[] descKeys;

    public DescriptionBlockItem(Block block, Properties properties, String... descKeys) {
        super(block, properties);
        this.descKeys = descKeys;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        for (String key : this.descKeys) {
            tooltip.accept(Component.translatable(key));
        }
    }
}
