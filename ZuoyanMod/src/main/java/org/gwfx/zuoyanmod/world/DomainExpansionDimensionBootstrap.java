package org.gwfx.zuoyanmod.world;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

public final class DomainExpansionDimensionBootstrap {

    private static final Logger LOGGER = LogUtils.getLogger();

    private DomainExpansionDimensionBootstrap() {}

    public static ServerLevel getOrCreateDomain(MinecraftServer server) {
        if (server == null) return null;
        ServerLevel domain = server.getLevel(DomainExpansionDimensions.DOMAIN_KEY);
        if (domain == null) {
            LOGGER.warn("Domain expansion dimension '{}' is not loaded in this world.", DomainExpansionDimensions.DOMAIN_KEY.location());
        }
        return domain;
    }
}
