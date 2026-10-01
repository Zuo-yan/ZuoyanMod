package org.gwfx.zuoyanmod.client;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/**
 * 湮灭君主的全部关键帧动画定义（王座沉眠、苏醒拔剑、威严待机、行进、横斩、重劈、二阶段光翼蜕变、湮灭之环弹幕施法、死亡跪地）。
 * 双武器阶段：一阶段右手持湮灭君王之刃（近战横斩/重劈），二阶段切换为权杖·湮灭之环（弹幕施法）。
 * 基于 void_monarch.bbmodel 精确烘焙生成。
 */
public final class VoidMonarchAnimations {
    public static final AnimationDefinition SIT_DORMANT = AnimationDefinition.Builder.withLength(2.0F).looping()
            .addAnimation("waist", new AnimationChannel(
                    AnimationChannel.Targets.POSITION,
                    new Keyframe(0.00F, KeyframeAnimations.posVec(0.0F, -7.0F, -1.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.posVec(0.0F, -6.8F, -1.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.posVec(0.0F, -7.0F, -1.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("waist", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-6.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(-4.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(-6.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("left_leg", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-84.0F, -8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(-84.0F, -8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(-84.0F, -8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("right_leg", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-84.0F, 8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(-84.0F, 8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(-84.0F, 8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("head", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("left_arm", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, -15.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(-18.0F, 0.0F, -15.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, -15.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("right_arm", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(-18.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("sword", new AnimationChannel(
                    AnimationChannel.Targets.POSITION,
                    new Keyframe(0.00F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.posVec(0.0F, 0.2F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("sword", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("void_wings", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("void_wings", new AnimationChannel(
                    AnimationChannel.Targets.POSITION,
                    new Keyframe(0.00F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .build();

    public static final AnimationDefinition AWAKEN = AnimationDefinition.Builder.withLength(3.0F)
            .addAnimation("waist", new AnimationChannel(
                    AnimationChannel.Targets.POSITION,
                    new Keyframe(0.00F, KeyframeAnimations.posVec(0.0F, -7.0F, -1.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.80F, KeyframeAnimations.posVec(0.0F, -7.0F, -1.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.60F, KeyframeAnimations.posVec(0.0F, -3.0F, 0.5F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.20F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.00F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("waist", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-6.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.50F, KeyframeAnimations.degreeVec(4.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(12.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.80F, KeyframeAnimations.degreeVec(2.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.50F, KeyframeAnimations.degreeVec(-4.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("left_leg", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-84.0F, -8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.80F, KeyframeAnimations.degreeVec(-80.0F, -6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.60F, KeyframeAnimations.degreeVec(-40.0F, -3.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.20F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("right_leg", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-84.0F, 8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.80F, KeyframeAnimations.degreeVec(-80.0F, 6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.60F, KeyframeAnimations.degreeVec(-40.0F, 3.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.20F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("head", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.60F, KeyframeAnimations.degreeVec(22.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.20F, KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(-25.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.60F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("right_arm", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.80F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 20.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.50F, KeyframeAnimations.degreeVec(-45.0F, 20.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.20F, KeyframeAnimations.degreeVec(-95.0F, 30.0F, 15.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.60F, KeyframeAnimations.degreeVec(-40.0F, 10.0F, 5.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.00F, KeyframeAnimations.degreeVec(-15.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("sword", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.50F, KeyframeAnimations.degreeVec(-30.0F, 15.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.20F, KeyframeAnimations.degreeVec(-50.0F, 30.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("cape", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.50F, KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.20F, KeyframeAnimations.degreeVec(-45.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.00F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .build();

    public static final AnimationDefinition IDLE = AnimationDefinition.Builder.withLength(2.0F).looping()
            .addAnimation("body", new AnimationChannel(
                    AnimationChannel.Targets.POSITION,
                    new Keyframe(0.00F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.posVec(0.0F, 0.4F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("body", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(1.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("head", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(-2.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("left_arm", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, -8.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(-6.0F, 0.0F, -12.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, -8.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("right_arm", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-25.0F, -5.0F, 8.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(-22.0F, -5.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(-25.0F, -5.0F, 8.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("sword", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-10.0F, -15.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(-8.0F, -15.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(-10.0F, -15.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("cape", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(-12.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(-5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .build();

    public static final AnimationDefinition WALK = AnimationDefinition.Builder.withLength(1.5F).looping()
            .addAnimation("waist", new AnimationChannel(
                    AnimationChannel.Targets.POSITION,
                    new Keyframe(0.00F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.38F, KeyframeAnimations.posVec(0.0F, 0.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.75F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.12F, KeyframeAnimations.posVec(0.0F, 0.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.50F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("waist", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(0.0F, 2.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, -2.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.50F, KeyframeAnimations.degreeVec(0.0F, 2.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("left_leg", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-28.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(28.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.50F, KeyframeAnimations.degreeVec(-28.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("right_leg", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(28.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(-28.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.50F, KeyframeAnimations.degreeVec(28.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("left_arm", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(22.0F, 0.0F, -8.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(-22.0F, 0.0F, -8.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.50F, KeyframeAnimations.degreeVec(22.0F, 0.0F, -8.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("right_arm", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-35.0F, 0.0F, 8.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(-15.0F, 0.0F, 8.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.50F, KeyframeAnimations.degreeVec(-35.0F, 0.0F, 8.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("cape", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(-25.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.50F, KeyframeAnimations.degreeVec(-18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .build();

    public static final AnimationDefinition ATTACK_HORIZONTAL = AnimationDefinition.Builder.withLength(1.0F)
            .addAnimation("body", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 35.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.40F, KeyframeAnimations.degreeVec(0.0F, -45.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.70F, KeyframeAnimations.degreeVec(0.0F, -20.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("right_arm", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-25.0F, 0.0F, 8.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(-20.0F, 65.0F, 30.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.40F, KeyframeAnimations.degreeVec(-80.0F, -75.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.60F, KeyframeAnimations.degreeVec(-60.0F, -40.0F, -5.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(-25.0F, 0.0F, 8.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("sword", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(10.0F, 45.0F, -20.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.40F, KeyframeAnimations.degreeVec(-15.0F, -60.0F, 45.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.70F, KeyframeAnimations.degreeVec(0.0F, -20.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("left_arm", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, -8.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(-20.0F, -20.0F, -15.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.40F, KeyframeAnimations.degreeVec(25.0F, 30.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, -8.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .build();

    public static final AnimationDefinition ATTACK_OVERHEAD = AnimationDefinition.Builder.withLength(1.2F)
            .addAnimation("waist", new AnimationChannel(
                    AnimationChannel.Targets.POSITION,
                    new Keyframe(0.00F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.35F, KeyframeAnimations.posVec(0.0F, 1.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.55F, KeyframeAnimations.posVec(0.0F, -2.5F, 1.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.85F, KeyframeAnimations.posVec(0.0F, -1.0F, 0.5F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.20F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("waist", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.35F, KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.55F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.85F, KeyframeAnimations.degreeVec(15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.20F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("right_arm", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-25.0F, 0.0F, 8.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.35F, KeyframeAnimations.degreeVec(-165.0F, 10.0F, -15.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.55F, KeyframeAnimations.degreeVec(-25.0F, 5.0F, -5.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.85F, KeyframeAnimations.degreeVec(-45.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.20F, KeyframeAnimations.degreeVec(-25.0F, 0.0F, 8.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("left_arm", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, -8.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.35F, KeyframeAnimations.degreeVec(-155.0F, -10.0F, 15.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.55F, KeyframeAnimations.degreeVec(-20.0F, -5.0F, 5.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.85F, KeyframeAnimations.degreeVec(-35.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.20F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, -8.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("sword", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.35F, KeyframeAnimations.degreeVec(35.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.55F, KeyframeAnimations.degreeVec(-75.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.85F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.20F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .build();

    public static final AnimationDefinition PHASE2_TRANSFORM = AnimationDefinition.Builder.withLength(2.0F)
            .addAnimation("waist", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.30F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.70F, KeyframeAnimations.degreeVec(-25.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.40F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("head", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.30F, KeyframeAnimations.degreeVec(15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.70F, KeyframeAnimations.degreeVec(-45.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.40F, KeyframeAnimations.degreeVec(-40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("void_wings", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.50F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.70F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 45.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.40F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 30.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("void_wings", new AnimationChannel(
                    AnimationChannel.Targets.POSITION,
                    new Keyframe(0.00F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.70F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.5F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.posVec(0.0F, 0.5F, 0.2F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("right_arm", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-25.0F, 0.0F, 8.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.30F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.70F, KeyframeAnimations.degreeVec(-110.0F, 35.0F, 45.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.40F, KeyframeAnimations.degreeVec(-100.0F, 30.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(-25.0F, 0.0F, 8.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("left_arm", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, -8.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.30F, KeyframeAnimations.degreeVec(-5.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.70F, KeyframeAnimations.degreeVec(-110.0F, -35.0F, -45.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.40F, KeyframeAnimations.degreeVec(-100.0F, -30.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, -8.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("scepter", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.70F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -55.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.10F, KeyframeAnimations.degreeVec(25.0F, 0.0F, 30.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("scepter", new AnimationChannel(
                    AnimationChannel.Targets.POSITION,
                    new Keyframe(0.00F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.70F, KeyframeAnimations.posVec(1.5F, -7.0F, 1.5F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.10F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .build();

    /** 二阶段弹幕施法：举杖引能，0.7s 处前甩释出虚空弹幕（与服务端 volley 时机对齐） */
    public static final AnimationDefinition CAST_BARRAGE = AnimationDefinition.Builder.withLength(1.3F)
            .addAnimation("right_arm", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-25.0F, 0.0F, 8.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.30F, KeyframeAnimations.degreeVec(-95.0F, 0.0F, 20.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.70F, KeyframeAnimations.degreeVec(-115.0F, 0.0F, 8.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.95F, KeyframeAnimations.degreeVec(-100.0F, 0.0F, 14.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.30F, KeyframeAnimations.degreeVec(-25.0F, 0.0F, 8.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("scepter", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.30F, KeyframeAnimations.degreeVec(-25.0F, 0.0F, 12.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.70F, KeyframeAnimations.degreeVec(40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.30F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("left_arm", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, -8.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.30F, KeyframeAnimations.degreeVec(-70.0F, -15.0F, -35.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.70F, KeyframeAnimations.degreeVec(-60.0F, -10.0F, -30.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.30F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, -8.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("head", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.30F, KeyframeAnimations.degreeVec(-12.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.30F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("waist", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.30F, KeyframeAnimations.degreeVec(-8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.70F, KeyframeAnimations.degreeVec(6.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.30F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .build();

    public static final AnimationDefinition DEATH_KNEEL = AnimationDefinition.Builder.withLength(3.0F)
            .addAnimation("waist", new AnimationChannel(
                    AnimationChannel.Targets.POSITION,
                    new Keyframe(0.00F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.40F, KeyframeAnimations.posVec(0.0F, 0.5F, -0.5F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.posVec(0.0F, -6.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.posVec(0.0F, -8.0F, 0.5F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.00F, KeyframeAnimations.posVec(0.0F, -8.0F, 0.5F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("waist", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.40F, KeyframeAnimations.degreeVec(-18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(12.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(35.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.00F, KeyframeAnimations.degreeVec(35.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("right_leg", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.40F, KeyframeAnimations.degreeVec(15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(-85.0F, 5.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(-88.0F, 5.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.00F, KeyframeAnimations.degreeVec(-88.0F, 5.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("left_leg", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.40F, KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(45.0F, -5.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(50.0F, -5.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.00F, KeyframeAnimations.degreeVec(50.0F, -5.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("head", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.40F, KeyframeAnimations.degreeVec(-25.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(48.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.00F, KeyframeAnimations.degreeVec(52.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("right_arm", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-25.0F, 0.0F, 8.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.40F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(15.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 8.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.00F, KeyframeAnimations.degreeVec(8.0F, 0.0F, 8.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("left_arm", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, -8.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.40F, KeyframeAnimations.degreeVec(-25.0F, 0.0F, -20.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(35.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(45.0F, 0.0F, -5.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.00F, KeyframeAnimations.degreeVec(45.0F, 0.0F, -5.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("sword", new AnimationChannel(
                    AnimationChannel.Targets.POSITION,
                    new Keyframe(0.00F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.40F, KeyframeAnimations.posVec(0.0F, -2.0F, 1.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.posVec(-2.0F, -8.0F, 3.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.posVec(-2.5F, -10.0F, 3.5F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.00F, KeyframeAnimations.posVec(-2.5F, -10.0F, 3.5F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("sword", new AnimationChannel(
                    AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.00F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.40F, KeyframeAnimations.degreeVec(45.0F, 0.0F, 20.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.00F, KeyframeAnimations.degreeVec(80.0F, -10.0F, 65.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.00F, KeyframeAnimations.degreeVec(85.0F, -12.0F, 70.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.00F, KeyframeAnimations.degreeVec(85.0F, -12.0F, 70.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .build();

    private VoidMonarchAnimations() {}
}
