package org.gwfx.zuoyanmod.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.gwfx.zuoyanmod.Zuoyanmod;

public final class RealmDimensions {
    public static final ResourceKey<Level> REALM_KEY = ResourceKey.create(
            Registries.DIMENSION,
            new ResourceLocation(Zuoyanmod.MODID, "realm")
    );
    public static final BlockPos SPAWN = new BlockPos(0, 12, 0);
    private RealmDimensions() {}
}
