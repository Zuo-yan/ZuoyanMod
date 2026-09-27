package org.gwfx.zuoyanmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.entity.CausalityBulletEntity;

/**
 * 因果律子弹的渲染器：能量球核心 + 曳光尾迹。
 *
 * <p>视觉设计参考 TACZ（永恒枪械工坊）的 {@code EntityBulletRenderer.renderTracerAmmo}：
 * 子弹头部是一个 billboard 绿色能量球，身后拖着一条随速度拉长的发光能量束，
 * 形成"平行宇宙射线"的视觉冲击。
 *
 * <p>1.21.1 渲染管线说明：经典 {@code EntityRenderer<CausalityBulletEntity>} 单泛型，
 * 无 RenderState 中转——直接在 {@link #render} 里读实体字段插值绘制；
 * billboard 朝向用 {@code entityRenderDispatcher.cameraOrientation()} 转到相机坐标系。
 */
public class CausalityBulletRenderer extends EntityRenderer<CausalityBulletEntity> {

    /** 能量球（弹头）贴图 */
    private static final ResourceLocation BALL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Zuoyanmod.MODID, "textures/entity/causality_bullet.png");

    /** 曳光尾迹贴图：纵向渐变（头部亮、尾部透明） */
    private static final ResourceLocation TRAIL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Zuoyanmod.MODID, "textures/entity/causality_trail.png");

    /** 能量球用 cutout（硬边缘透明） */
    private static final RenderType BALL_TYPE = RenderType.entityCutout(BALL_TEXTURE);

    /** 尾迹用 emissive translucent（发光 + 半透明） */
    private static final RenderType TRAIL_TYPE = RenderType.entityTranslucentEmissive(TRAIL_TEXTURE);

    /** 曳光尾迹基础宽度 */
    private static final float TRAIL_WIDTH = 0.12F;

    /** 尾迹长度系数（参考 TACZ 的 0.85） */
    private static final float TRAIL_LENGTH_FACTOR = 0.85F;

    /** 尾迹最大长度，防超长 */
    private static final float TRAIL_MAX_LENGTH = 3.0F;

    public CausalityBulletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** 满亮：能量体不受环境光影响 */
    @Override
    protected int getBlockLightLevel(CausalityBulletEntity entity, BlockPos blockPos) {
        return 15;
    }

    @Override
    public void render(CausalityBulletEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // —— 曳光尾迹 ——
        renderTrail(entity, partialTicks, poseStack, bufferSource, packedLight);

        // —— 能量球弹头（billboard，始终正对摄像机） ——
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        VertexConsumer buffer = bufferSource.getBuffer(BALL_TYPE);
        buildBallQuad(packedLight, poseStack.last(), buffer);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    /**
     * 绘制曳光尾迹：沿速度反方向延伸的发光绿色能量束。
     *
     * <p>实现思路（仿 TACZ）：
     * 1. 用 yRot/xRot 把 pose 的 +Z 轴对齐到飞行方向
     * 2. 从子弹位置向 -Z（身后）拉伸一个长条 quad
     * 3. 长度 = min(速度 × {@value TRAIL_LENGTH_FACTOR}, {@value TRAIL_MAX_LENGTH})
     * 4. 使用 emissive 贴图产生发光感
     */
    private void renderTrail(CausalityBulletEntity entity, float partialTicks,
                             PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float speed = (float) entity.getDeltaMovement().length();
        if (speed < 0.01F) return;

        float trailLength = Math.min(speed * TRAIL_LENGTH_FACTOR, TRAIL_MAX_LENGTH);
        // 旋转角在插值帧间平滑过渡
        float yRot = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        float xRot = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());

        poseStack.pushPose();
        // 旋转到飞行方向
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
        // 尾迹中心在子弹身后 trailLength/2 处
        poseStack.translate(0.0F, 0.0F, -trailLength / 2.0F);
        // 拉伸：宽 = TRAIL_WIDTH，长 = trailLength
        poseStack.scale(TRAIL_WIDTH, TRAIL_WIDTH, trailLength);
        // 画一个沿 Z 轴的长条 quad（XZ 平面，Y=0），UV：V=1 在头部、V=0 在尾部
        VertexConsumer buffer = bufferSource.getBuffer(TRAIL_TYPE);
        buildTrailQuad(packedLight, poseStack.last(), buffer);
        poseStack.popPose();
    }

    /** 能量球 billboard quad：宽 1.0 高 1.0，锚点中心 */
    private static void buildBallQuad(int light, PoseStack.Pose pose, VertexConsumer buffer) {
        float h = 0.5F;
        vertex(buffer, pose, light, -h, -h, 0.0F, 0, 0);
        vertex(buffer, pose, light,  h, -h, 0.0F, 1, 0);
        vertex(buffer, pose, light,  h,  h, 0.0F, 1, 1);
        vertex(buffer, pose, light, -h,  h, 0.0F, 0, 1);
    }

    /**
     * 尾迹长条 quad：X 方向宽度 [-0.5, 0.5]（拉伸后 = TRAIL_WIDTH），
     * Z 方向长度 [-0.5, 0.5]（拉伸后 = trailLength）。
     * V=0 对应尾部（Z=-0.5），V=1 对应头部（Z=+0.5）。
     */
    private static void buildTrailQuad(int light, PoseStack.Pose pose, VertexConsumer buffer) {
        float w = 0.5F;
        float l = 0.5F;
        // 前侧面（Y=+0.5）
        vertex(buffer, pose, light, -w, 0.0F, -l, 0, 0);
        vertex(buffer, pose, light,  w, 0.0F, -l, 1, 0);
        vertex(buffer, pose, light,  w, 0.0F,  l, 1, 1);
        vertex(buffer, pose, light, -w, 0.0F,  l, 0, 1);
        // 背侧面（Y=-0.5），保证从下方也能看到
        vertex(buffer, pose, light,  w, 0.0F, -l, 1, 0);
        vertex(buffer, pose, light, -w, 0.0F, -l, 0, 0);
        vertex(buffer, pose, light, -w, 0.0F,  l, 0, 1);
        vertex(buffer, pose, light,  w, 0.0F,  l, 1, 1);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose,
                               int light, float x, float y, float z, int u, int v) {
        buffer.addVertex(pose, x, y, z)
                .setColor(0xFFFFFFFF)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(CausalityBulletEntity entity) {
        return BALL_TEXTURE;
    }
}
