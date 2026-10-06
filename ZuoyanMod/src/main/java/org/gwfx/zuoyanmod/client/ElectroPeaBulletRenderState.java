package org.gwfx.zuoyanmod.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;

/**
 * 电能豌豆子弹的客户端渲染状态。
 */
public class ElectroPeaBulletRenderState extends EntityRenderState {
    public Vec3 velocity = Vec3.ZERO;
    public float yRot;
    public float xRot;
}

