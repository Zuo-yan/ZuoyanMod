package org.gwfx.zuoyanmod.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

/**
 * 超级电能机枪豌豆的客户端渲染状态。
 */
public class SuperElectricGatlingPeaRenderState extends LivingEntityRenderState {
    public final AnimationState idleAnimation = new AnimationState();
    public final AnimationState shootAnimation = new AnimationState();
    public boolean isShooting;
    public boolean isCarried;
}

