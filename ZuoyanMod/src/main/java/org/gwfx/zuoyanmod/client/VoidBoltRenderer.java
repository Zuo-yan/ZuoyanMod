package org.gwfx.zuoyanmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.entity.VoidBoltEntity;

/**
 * 暗物质螺栓的渲染器：单个 billboard 暗紫能量球（无尾迹，拖尾交给粒子）。
 *
 * <p>与 {@link CausalityBulletRenderer} 同一套 26.3 自定义几何管线，
 * 但只画一个始终正对摄像机的 quad——弹幕数量多，渲染开销要压到最低。
 */
public class VoidBoltRenderer extends EntityRenderer<VoidBoltEntity, EntityRenderState> {

    /** 能量球贴图：assets/zuoyanmod/textures/entity/void_bolt.png */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Zuoyanmod.MODID, "textures/entity/void_bolt.png");

    private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE);

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
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack,
                       SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.scale(SIZE, SIZE, SIZE);
        poseStack.rotate(camera.orientation);
        submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            float h = 0.5F;
            vertex(buffer, pose, -h, -h, 0, 0);
            vertex(buffer, pose, h, -h, 1, 0);
            vertex(buffer, pose, h, h, 1, 1);
            vertex(buffer, pose, -h, h, 0, 1);
        });
        poseStack.popPose();

        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, int u, int v) {
        buffer.addVertex(pose, x, y, 0.0F)
                .setColor(0xFFFFFFFF)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
