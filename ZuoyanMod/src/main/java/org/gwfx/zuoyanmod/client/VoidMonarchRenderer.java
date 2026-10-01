package org.gwfx.zuoyanmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.entity.VoidMonarchEntity;

/**
 * 湮灭君主的实体渲染器：专属高精 3D 骨骼模型与关键帧动画渲染。
 * 整体视觉放大 1.8 倍；一阶段右手持湮灭君王之刃，二阶段切换为权杖·湮灭之环，配王披风、金冠与虚空双翼。
 */
public class VoidMonarchRenderer extends MobRenderer<VoidMonarchEntity, VoidMonarchRenderState, VoidMonarchModel> {

    /** 整体渲染缩放（碰撞箱不变，仅视觉放大） */
    public static final float SCALE = 1.8F;

    /** 常态皮肤：assets/zuoyanmod/textures/entity/void_monarch.png */
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "textures/entity/void_monarch.png");

    /** 狂暴皮肤：assets/zuoyanmod/textures/entity/void_monarch_phase2.png */
    private static final Identifier TEXTURE_ENRAGED =
            Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "textures/entity/void_monarch_phase2.png");

    public VoidMonarchRenderer(EntityRendererProvider.Context context) {
        super(context, new VoidMonarchModel(context.bakeLayer(BossModelLayers.VOID_MONARCH_BODY)), 1.2F);
    }

    @Override
    public VoidMonarchRenderState createRenderState() {
        return new VoidMonarchRenderState();
    }

    @Override
    public void extractRenderState(VoidMonarchEntity entity, VoidMonarchRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.awakened = entity.isAwakened();
        state.isAwakening = entity.isAwakening();
        state.enraged = entity.isEnraged();
        state.attackState = entity.getAttackState();
        state.isDying = entity.isDying();

        state.sitAnimation.copyFrom(entity.sitAnimationState);
        state.awakenAnimation.copyFrom(entity.awakenAnimationState);
        state.idleAnimation.copyFrom(entity.idleAnimationState);
        state.walkAnimation.copyFrom(entity.walkAnimationState);
        state.attackHorizontalAnimation.copyFrom(entity.attackHorizontalAnimationState);
        state.attackOverheadAnimation.copyFrom(entity.attackOverheadAnimationState);
        state.phase2Animation.copyFrom(entity.phase2AnimationState);
        state.deathAnimation.copyFrom(entity.deathAnimationState);
        state.attackBarrageAnimation.copyFrom(entity.attackBarrageAnimationState);
    }

    @Override
    protected void scale(VoidMonarchRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    protected void setupRotations(VoidMonarchRenderState state, PoseStack poseStack, float bodyRot, float scale) {
        if (state.isDying) {
            // 死亡跪地期间拦截原版的 90 度侧翻，保持单膝下跪骨骼动画正常播放
            poseStack.rotateDegrees(com.mojang.math.Axis.YP, 180.0F - bodyRot);
            return;
        }
        super.setupRotations(state, poseStack, bodyRot, scale);
    }

    @Override
    public Identifier getTextureLocation(VoidMonarchRenderState state) {
        return state.enraged ? TEXTURE_ENRAGED : TEXTURE;
    }
}
