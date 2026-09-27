package org.gwfx.zuoyanmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.entity.VoidBoltEntity;

/**
 * 暗物质螺栓的渲染器：单个 billboard 暗紫能量球（无尾迹，拖尾交给粒子）。
 *
 * <p>与 {@link CausalityBulletRenderer} 同一套自绘几何做法，
 * 但只画一个始终正对摄像机的 quad——弹幕数量多，渲染开销要压到最低。
 */
public class VoidBoltRenderer extends EntityRenderer<VoidBoltEntity> {

    /** 能量球贴图：assets/zuoyanmod/textures/entity/void_bolt.png */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Zuoyanmod.MODID, "textures/entity/void_bolt.png");

    private static final RenderType RENDER_TYPE = RenderType.entityCutout(TEXTURE);

    /** billboard 边长（坐标单位），碰撞箱 0.4，视觉略大更有威胁感 */
    private static final float SIZE = 0.55F;

    public VoidBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** 满亮：能量体不受环境光影响 */
    @Override
    protected int getBlockLightLevel(VoidBoltEntity entity, BlockPos blockPos) {
        return 15;
    }

    @Override
    public void render(VoidBoltEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(SIZE, SIZE, SIZE);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        VertexConsumer buffer = bufferSource.getBuffer(RENDER_TYPE);
        float h = 0.5F;
        vertex(buffer, poseStack.last(), packedLight, -h, -h, 0, 0);
        vertex(buffer, poseStack.last(), packedLight,  h, -h, 1, 0);
        vertex(buffer, poseStack.last(), packedLight,  h,  h, 1, 1);
        vertex(buffer, poseStack.last(), packedLight, -h,  h, 0, 1);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, int light,
                               float x, float y, int u, int v) {
        buffer.addVertex(pose, x, y, 0.0F)
                .setColor(0xFFFFFFFF)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(VoidBoltEntity entity) {
        return TEXTURE;
    }
}
