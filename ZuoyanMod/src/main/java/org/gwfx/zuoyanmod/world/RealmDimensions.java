package org.gwfx.zuoyanmod.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.gwfx.zuoyanmod.Zuoyanmod;

public final class RealmDimensions {
    public static final ResourceKey<Level> REALM_KEY = ResourceKey.create(
            Registries.DIMENSION,
            new ResourceLocation(Zuoyanmod.MODID, "realm")
    );

    /** 出生点的水平坐标（世界中心）。 */
    public static final int SPAWN_X = 0;
    public static final int SPAWN_Z = 0;

    /**
     * 出生点高度交给生成器算。
     *
     * <p>{@code FlatLevelSource#getSpawnHeight} = {@code minY + min(维度高度, 总层数)}，
     * 也就是最顶层固体方块上方那一格。这样调整维度 JSON 的 {@code layers} 时
     * 不需要再手动同步任何常量（层高从 76 改到 199 也不会把玩家埋进石头里）。
     */
    public static BlockPos spawnPos(ServerLevel level) {
        int y = level.getChunkSource().getGenerator().getSpawnHeight(level);
        return new BlockPos(SPAWN_X, y, SPAWN_Z);
    }

    private RealmDimensions() {
    }
}
