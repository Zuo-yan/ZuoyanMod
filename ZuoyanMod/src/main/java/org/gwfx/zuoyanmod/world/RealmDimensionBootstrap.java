package org.gwfx.zuoyanmod.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class RealmDimensionBootstrap {
    private RealmDimensionBootstrap() {}

    public static ServerLevel getOrCreate(MinecraftServer server) {
        if (server == null) return null;
        return server.getLevel(RealmDimensions.REALM_KEY);
    }
}