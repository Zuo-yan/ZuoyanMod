package org.gwfx.zuoyanmod.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

public final class DomainExpansionWorldGen {

    private static final int PLATFORM_SIZE = 50;
    private static final int HALF_SIZE = PLATFORM_SIZE / 2;
    private static final int PLATFORM_Y = 80;

    private DomainExpansionWorldGen() {}

    public static void generateArena(ServerLevel level) {
        BlockPos center = new BlockPos(0, PLATFORM_Y, 0);

        for (int x = -HALF_SIZE; x < HALF_SIZE; x++) {
            for (int z = -HALF_SIZE; z < HALF_SIZE; z++) {
                level.setBlockAndUpdate(center.offset(x, -1, z), Blocks.BEDROCK.defaultBlockState());
                level.setBlockAndUpdate(center.offset(x, 13, z), Blocks.BEDROCK.defaultBlockState());

                boolean edge = x == -HALF_SIZE || x == HALF_SIZE - 1 || z == -HALF_SIZE || z == HALF_SIZE - 1;
                if (edge) {
                    level.setBlockAndUpdate(center.offset(x, 0, z), Blocks.BEDROCK.defaultBlockState());
                    level.setBlockAndUpdate(center.offset(x, 1, z), Blocks.BEDROCK.defaultBlockState());
                    level.setBlockAndUpdate(center.offset(x, 2, z), Blocks.BEDROCK.defaultBlockState());
                    for (int y = 3; y <= 10; y++) {
                        level.setBlockAndUpdate(center.offset(x, y, z), Blocks.IRON_BARS.defaultBlockState());
                    }
                    level.setBlockAndUpdate(center.offset(x, 11, z), Blocks.IRON_BARS.defaultBlockState());
                    level.setBlockAndUpdate(center.offset(x, 12, z), Blocks.BEDROCK.defaultBlockState());
                }
            }
        }
    }
}
