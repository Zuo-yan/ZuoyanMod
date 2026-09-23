package org.gwfx.zuoyanmod.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.gwfx.zuoyanmod.Zuoyanmod;

public final class DomainExpansionDimensions {

    public static final ResourceKey<Level> DOMAIN_KEY = ResourceKey.create(
            Registries.DIMENSION,
            Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "domain_expansion")
    );
    public static final ResourceKey<Level> OVERWORLD_KEY = Level.OVERWORLD;
    public static final BlockPos PLATFORM_CENTER = new BlockPos(0, 80, 0);

    private DomainExpansionDimensions() {}

    public static Vec3 spawnPos() {
        return new Vec3(0.5D, 80D, 0.5D);
    }

    public static ServerLevel getDomain(ServerLevel serverLevel) {
        return serverLevel.getServer().getLevel(DOMAIN_KEY);
    }
}
