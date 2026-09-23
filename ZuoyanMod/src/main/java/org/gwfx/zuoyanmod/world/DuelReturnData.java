package org.gwfx.zuoyanmod.world;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record DuelReturnData(ResourceKey<Level> originDimension, Vec3 originPos, float yRot, float xRot) {
}
