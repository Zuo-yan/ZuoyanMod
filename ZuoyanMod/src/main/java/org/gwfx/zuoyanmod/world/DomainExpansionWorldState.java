package org.gwfx.zuoyanmod.world;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gwfx.zuoyanmod.platform.Teleports;
import org.slf4j.Logger;

/**
 * 领域展开的实体搬运。
 *
 * <p>1.20.1 的实体跨维度重建走 {@code CompoundTag + EntityType.loadEntityRecursive}
 * （26.x 是 TagValueOutput + EntitySpawnReason）。
 */
public final class DomainExpansionWorldState {

    private static final Logger LOGGER = LogUtils.getLogger();

    private DomainExpansionWorldState() {}

    public static Vec3 platformSpawn() {
        return new Vec3(0.5D, 80D, 0.5D);
    }

    public static void teleportPair(ServerLevel domainLevel, ServerPlayer caster, LivingEntity target) {
        Vec3 spawn = platformSpawn();

        teleportEntity(domainLevel, target, spawn.x + 2.0D, spawn.y, spawn.z, target.getYRot(), target.getXRot());
        teleportEntity(domainLevel, caster, spawn.x - 2.0D, spawn.y, spawn.z, caster.getYRot(), caster.getXRot());
    }

    public static Entity teleportEntity(ServerLevel destination, Entity entity, double x, double y, double z, float yRot, float xRot) {
        if (entity instanceof ServerPlayer player) {
            // 传送签名随版本变化，统一走 platform 适配层
            Teleports.crossDimension(player, destination, x, y, z, yRot, xRot);
            player.setDeltaMovement(Vec3.ZERO);
            return player;
        }

        Entity recreated = recreateInDestination(destination, entity, x, y, z, yRot, xRot);
        if (recreated != null) {
            entity.discard();
            return recreated;
        }
        LOGGER.warn("Failed to recreate entity {} in dimension {}", entity.getType(), destination.dimension().location());
        return entity;
    }

    private static Entity recreateInDestination(ServerLevel destination, Entity source, double x, double y, double z, float yRot, float xRot) {
        CompoundTag tag = new CompoundTag();
        source.saveWithoutId(tag);
        Entity recreated = EntityType.loadEntityRecursive(tag, destination, entity -> {
            entity.moveTo(x, y, z, yRot, xRot);
            entity.setDeltaMovement(Vec3.ZERO);
            return entity;
        });
        if (recreated != null) {
            recreated.moveTo(x, y, z, yRot, xRot);
            recreated.setDeltaMovement(Vec3.ZERO);
            destination.addFreshEntity(recreated);
        }
        return recreated;
    }
}
