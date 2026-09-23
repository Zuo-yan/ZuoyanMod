package org.gwfx.zuoyanmod.world;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RealmTransitionManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, Position> OVERWORLD_POSITIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Position> REALM_POSITIONS = new ConcurrentHashMap<>();

    private RealmTransitionManager() {}

    public static void toggle(ServerPlayer player) {
        LOGGER.info("[Realm] toggle requested by {} in {}", player.getName().getString(), player.level().dimension().identifier());
        if (player.level().dimension().equals(RealmDimensions.REALM_KEY)) {
            leaveRealm(player);
        } else {
            enterRealm(player);
        }
    }

    private static void enterRealm(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel currentLevel)) return;
        MinecraftServer server = currentLevel.getServer();
        ServerLevel realm = RealmDimensionBootstrap.getOrCreate(server);
        if (realm == null) {
            LOGGER.warn("[Realm] realm level is null, cannot enter");
            return;
        }

        OVERWORLD_POSITIONS.put(player.getUUID(), Position.capture(player));
        Position destination = REALM_POSITIONS.get(player.getUUID());
        if (destination == null) {
            destination = new Position(RealmDimensions.REALM_KEY, 0.5D, 12.0D, 0.5D, player.getYRot(), player.getXRot());
        }
        teleport(player, realm, destination);
    }

    private static void leaveRealm(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel currentLevel)) return;
        MinecraftServer server = currentLevel.getServer();

        REALM_POSITIONS.put(player.getUUID(), Position.capture(player));
        Position destination = OVERWORLD_POSITIONS.get(player.getUUID());
        if (destination == null) {
            destination = new Position(Level.OVERWORLD, 0.5D, 80.0D, 0.5D, player.getYRot(), player.getXRot());
        }

        ResourceKey<Level> dimension = Objects.requireNonNull(destination.dimension());
        ServerLevel target = server.getLevel(dimension);
        if (target == null) {
            LOGGER.warn("[Realm] destination dimension {} is unavailable", dimension.identifier());
            return;
        }
        teleport(player, target, destination);
    }

    private static void teleport(ServerPlayer player, ServerLevel level, Position destination) {
        LOGGER.info("[Realm] teleporting {} to {} at {}, {}, {}", player.getName().getString(),
                level.dimension().identifier(), destination.x(), destination.y(), destination.z());
        // 匹配 26.x 签名：teleportTo(ServerLevel, double, double, double, Set<Relative>, float, float, boolean)
        player.teleportTo(level, destination.x(), destination.y(), destination.z(), Set.<Relative>of(), destination.yRot(), destination.xRot(), false);
    }

    private record Position(ResourceKey<Level> dimension, double x, double y, double z, float yRot, float xRot) {
        private static Position capture(ServerPlayer player) {
            return new Position(player.level().dimension(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        }
    }
}