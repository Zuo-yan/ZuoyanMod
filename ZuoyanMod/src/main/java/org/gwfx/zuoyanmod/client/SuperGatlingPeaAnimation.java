package org.gwfx.zuoyanmod.client;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/**
 * 超级电能机枪豌豆的模型动画定义（待机与射击）。
 * 数据由 super_electric_gatling_pea.bbmodel 骨骼关键帧精确烘焙生成。
 */
public final class SuperGatlingPeaAnimation {

    public static final AnimationDefinition IDLE = AnimationDefinition.Builder.withLength(2.0F)
            .looping()
            .addAnimation("stem", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-0.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(-2.0F, -0.0F, 1.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(-0.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("head", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-0.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(3.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(-0.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("head", new AnimationChannel(
                    AnimationChannel.Targets.POSITION,
                    new Keyframe(0.00F, KeyframeAnimations.posVec(-0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.posVec(-0.0F, 0.5F, -0.2F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.posVec(-0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("lightning", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-0.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.40F, KeyframeAnimations.degreeVec(-2.0F, -3.0F, -2.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.80F, KeyframeAnimations.degreeVec(2.0F, 2.0F, 3.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.20F, KeyframeAnimations.degreeVec(-3.0F, 1.0F, -1.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.60F, KeyframeAnimations.degreeVec(1.0F, -2.0F, 2.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(-0.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .build();

    public static final AnimationDefinition SHOOT = AnimationDefinition.Builder.withLength(1.0F)
            .looping()
            .addAnimation("snout", new AnimationChannel(
                    AnimationChannel.Targets.POSITION,
                    new Keyframe(0.00F, KeyframeAnimations.posVec(-0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.10F, KeyframeAnimations.posVec(-0.0F, 0.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.15F, KeyframeAnimations.posVec(-0.0F, 0.0F, -1.8F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.19F, KeyframeAnimations.posVec(-0.0F, 0.0F, 2.2F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.27F, KeyframeAnimations.posVec(-0.0F, 0.0F, 0.3F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.30F, KeyframeAnimations.posVec(-0.0F, 0.0F, -1.8F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.34F, KeyframeAnimations.posVec(-0.0F, 0.0F, 2.4F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.42F, KeyframeAnimations.posVec(-0.0F, 0.0F, 0.3F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.45F, KeyframeAnimations.posVec(-0.0F, 0.0F, -1.8F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.49F, KeyframeAnimations.posVec(-0.0F, 0.0F, 2.4F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.57F, KeyframeAnimations.posVec(-0.0F, 0.0F, 0.3F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.60F, KeyframeAnimations.posVec(-0.0F, 0.0F, -2.2F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.65F, KeyframeAnimations.posVec(-0.0F, 0.0F, 2.8F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.80F, KeyframeAnimations.posVec(-0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.posVec(-0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("head", new AnimationChannel(
                    AnimationChannel.Targets.POSITION,
                    new Keyframe(0.00F, KeyframeAnimations.posVec(-0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.10F, KeyframeAnimations.posVec(-0.0F, 0.0F, 0.6F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.19F, KeyframeAnimations.posVec(-0.0F, 0.2F, 1.6F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.27F, KeyframeAnimations.posVec(-0.0F, 0.0F, 0.4F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.34F, KeyframeAnimations.posVec(-0.0F, 0.2F, 1.8F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.42F, KeyframeAnimations.posVec(-0.0F, 0.0F, 0.4F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.49F, KeyframeAnimations.posVec(-0.0F, 0.2F, 2.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.57F, KeyframeAnimations.posVec(-0.0F, 0.0F, 0.4F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.65F, KeyframeAnimations.posVec(-0.0F, 0.3F, 2.4F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.82F, KeyframeAnimations.posVec(-0.0F, -0.2F, -0.5F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.92F, KeyframeAnimations.posVec(-0.0F, 0.1F, 0.1F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.posVec(-0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("head", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-0.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.10F, KeyframeAnimations.degreeVec(4.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.19F, KeyframeAnimations.degreeVec(8.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.27F, KeyframeAnimations.degreeVec(-1.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.34F, KeyframeAnimations.degreeVec(9.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.42F, KeyframeAnimations.degreeVec(-1.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.49F, KeyframeAnimations.degreeVec(10.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.57F, KeyframeAnimations.degreeVec(-1.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.65F, KeyframeAnimations.degreeVec(13.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.82F, KeyframeAnimations.degreeVec(-3.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.92F, KeyframeAnimations.degreeVec(1.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(-0.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("stem", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-0.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.10F, KeyframeAnimations.degreeVec(2.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.19F, KeyframeAnimations.degreeVec(5.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.27F, KeyframeAnimations.degreeVec(-0.5F, -0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.34F, KeyframeAnimations.degreeVec(6.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.42F, KeyframeAnimations.degreeVec(-0.5F, -0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.49F, KeyframeAnimations.degreeVec(7.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.57F, KeyframeAnimations.degreeVec(-0.5F, -0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.65F, KeyframeAnimations.degreeVec(8.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.82F, KeyframeAnimations.degreeVec(-3.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.92F, KeyframeAnimations.degreeVec(1.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(-0.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("lightning", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-0.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.10F, KeyframeAnimations.degreeVec(-4.0F, 5.0F, 6.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.19F, KeyframeAnimations.degreeVec(10.0F, -8.0F, -8.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.27F, KeyframeAnimations.degreeVec(-6.0F, 6.0F, 5.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.34F, KeyframeAnimations.degreeVec(12.0F, -9.0F, -9.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.42F, KeyframeAnimations.degreeVec(-7.0F, 7.0F, 6.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.49F, KeyframeAnimations.degreeVec(14.0F, -10.0F, -10.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.57F, KeyframeAnimations.degreeVec(-8.0F, 8.0F, 7.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.65F, KeyframeAnimations.degreeVec(16.0F, -12.0F, -12.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.80F, KeyframeAnimations.degreeVec(-5.0F, 3.0F, 3.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(-0.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("helmet", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-0.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.19F, KeyframeAnimations.degreeVec(-4.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.27F, KeyframeAnimations.degreeVec(2.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.34F, KeyframeAnimations.degreeVec(-5.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.42F, KeyframeAnimations.degreeVec(2.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.49F, KeyframeAnimations.degreeVec(-6.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.57F, KeyframeAnimations.degreeVec(3.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.65F, KeyframeAnimations.degreeVec(-8.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.82F, KeyframeAnimations.degreeVec(2.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(-0.0F, -0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .build();

    private SuperGatlingPeaAnimation() {}
}