package org.gwfx.zuoyanmod.world;

import net.minecraft.core.BlockPos;

public final class RealmWorldGen {
    private RealmWorldGen() {}

    public static BlockPos getSpawn() {
        return RealmDimensions.SPAWN;
    }
}
