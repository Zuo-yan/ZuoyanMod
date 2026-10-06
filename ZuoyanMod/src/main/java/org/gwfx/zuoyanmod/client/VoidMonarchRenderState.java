package org.gwfx.zuoyanmod.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

/**
 * 湮灭君主的客户端渲染状态。
 */
public class VoidMonarchRenderState extends LivingEntityRenderState {
    public final AnimationState sitAnimation = new AnimationState();
    public final AnimationState awakenAnimation = new AnimationState();
    public final AnimationState idleAnimation = new AnimationState();
    public final AnimationState walkAnimation = new AnimationState();
    public final AnimationState attackHorizontalAnimation = new AnimationState();
    public final AnimationState attackOverheadAnimation = new AnimationState();
    public final AnimationState phase2Animation = new AnimationState();
    public final AnimationState deathAnimation = new AnimationState();
    public final AnimationState attackBarrageAnimation = new AnimationState();

    public boolean awakened;
    public boolean isAwakening;
    public boolean enraged;
    public byte attackState;
    public boolean isDying;
}

