package org.gwfx.zuoyanmod.fluid;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;

/** 液态暗物质方块（LiquidBlock 构造为 protected，公开以便注册） */
public class DarkMatterLiquidBlock extends LiquidBlock {

    public DarkMatterLiquidBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
    }
}
