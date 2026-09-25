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
import net.minecraft.world.phys.Vec3;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.entity.CausalityBulletEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 因果律子弹的渲染器：能量球核心 + 曳光尾迹。
 *
 * <p>视觉设计参考 TACZ（永恒枪械工坊）的 {@code EntityBulletRenderer.renderTracerAmmo}：
 * 子弹头部是一个 billboard 绿色能量球，身后拖着一条随速度拉长的发光能量束，
 * 形成"平行宇宙射线"的视觉冲击。
 *
 * <p><b>1.20.1 适配</b>（26.3 是 RenderPipelines / render state 分层架构，这里全部退回老写法）：
 * <ul>
 *   <li>泛型从 {@code EntityRenderer<CausalityBulletEntity, BulletRenderState>} 退回
 *       单参数 {@code EntityRenderer<CausalityBulletEntity>}；
 *       1.20.1 <b>没有 render state</b>，速度/朝向直接从实体读（并在 {@code render} 里
 *       用 {@code partialTicks} 自己插值），自定义 {@code BulletRenderState} 类可以整块删掉。</li>
 *   <li>绘制入口从 {@code submit(...)} 退回经典的
 *       {@code render(entity, yaw, partialTicks, PoseStack, MultiBufferSource, packedLight)}；
 *       顶点缓冲靠 {@code MultiBufferSource#getBuffer(RenderType)} 自己取。</li>
 *   <li>billboard：26.3 用 {@code poseStack.rotate(camera.orientation)}；
 *       1.20.1 的等价写法是
 *       {@code poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation())}
 *       再跟一个 {@code Axis.YP.rotationDegrees(180.0F)}——后者是 1.20.1 原版 billboard
 *       的固定搭配（{@code DragonFireballRenderer} / {@code ExperienceOrbRenderer} 都这么写），
 *       不加会让 quad 变成背面朝外、被 {@code entityCutout} 的背面剔除吃掉。</li>
 *   <li>顶点 API：{@code addVertex(Pose,p,x,y,z).setColor().setUv()} 这套 26.3 链式 API 在 1.20.1
 *       是 {@code vertex(Matrix4f,x,y,z).color(255,255,255,255).uv(u,v)
 *       .overlayCoords(...).uv2(light).normal(Matrix3f,0,1,0).endVertex()}；
 *       矩阵取法从 {@code PoseStack.Pose} 直接传改成 {@code pose.pose()} / {@code pose.normal()}
 *       （返回 {@code org.joml.Matrix4f} / {@code Matrix3f}，注意 1.20.1 已经是 JOML，
 *       不再是 {@code com.mojang.math} 系）。</li>
 *   <li>{@code Identifier} → {@code ResourceLocation}；
 *       {@code RenderTypes.entityCutout / entityTranslucentEmissive} → {@code RenderType.entityCutout / entityTranslucentEmissive}。</li>
 * </ul>
 */
public class CausalityBulletRenderer extends EntityRenderer<CausalityBulletEntity> {

    /** 能量球（弹头）贴图 */
    private static final ResourceLocation BALL_TEXTURE =
            new ResourceLocation(Zuoyanmod.MODID, "textures/entity/causality_bullet.png");

    /** 曳光尾迹贴图：纵向渐变（头部亮、尾部透明） */
    private static final ResourceLocation TRAIL_TEXTURE =
            new ResourceLocation(Zuoyanmod.MODID, "textures/entity/causality_trail.png");

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
    public ResourceLocation getTextureLocation(CausalityBulletEntity entity) {
        return BALL_TEXTURE;
    }

    @Override
    public void render(CausalityBulletEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // 1.20.1 没有 render state，渲染时直接从实体取速度/朝向并做部分 tick 插值
        Vec3 velocity = entity.getDeltaMovement();
        float yRot = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        float xRot = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());

        // —— 曳光尾迹 ——
        renderTrail(velocity, yRot, xRot, poseStack, buffer, packedLight);

        // —— 能量球弹头（billboard，始终正对摄像机） ——
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer ballConsumer = buffer.getBuffer(BALL_TYPE);
        buildBallQuad(ballConsumer, pose.pose(), pose.normal(), packedLight);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
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
    private void renderTrail(Vec3 velocity, float yRot, float xRot,
                             PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float speed = (float) velocity.length();
        if (speed < 0.01F) return;

        float trailLength = Math.min(speed * TRAIL_LENGTH_FACTOR, TRAIL_MAX_LENGTH);

        poseStack.pushPose();
        // 旋转到飞行方向
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
        // 尾迹中心在子弹身后 trailLength/2 处
        poseStack.translate(0.0F, 0.0F, -trailLength / 2.0F);
        // 拉伸：宽 = TRAIL_WIDTH，长 = trailLength
        poseStack.scale(TRAIL_WIDTH, TRAIL_WIDTH, trailLength);
        // 画一个沿 Z 轴的长条 quad（XZ 平面，Y=0），UV：V=1 在头部、V=0 在尾部
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = buffer.getBuffer(TRAIL_TYPE);
        buildTrailQuad(consumer, pose.pose(), pose.normal(), packedLight);
        poseStack.popPose();
    }

    /** 能量球 billboard quad：宽 1.0 高 1.0，锚点中心 */
    private static void buildBallQuad(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal, int light) {
        float h = 0.5F;
        vertex(buffer, matrix, normal, light, -h, -h, 0.0F, 0, 0);
        vertex(buffer, matrix, normal, light, h, -h, 0.0F, 1, 0);
        vertex(buffer, matrix, normal, light, h, h, 0.0F, 1, 1);
        vertex(buffer, matrix, normal, light, -h, h, 0.0F, 0, 1);
    }

    /**
     * 尾迹长条 quad：X 方向宽度 [-0.5, 0.5]（拉伸后 = TRAIL_WIDTH），
     * Z 方向长度 [-0.5, 0.5]（拉伸后 = trailLength）。
     * V=0 对应尾部（Z=-0.5），V=1 对应头部（Z=+0.5）。
     */
    private static void buildTrailQuad(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal, int light) {
        float w = 0.5F;
        float l = 0.5F;
        // 前侧面（Y=+0.5）
        vertex(buffer, matrix, normal, light, -w, 0.0F, -l, 0, 0);
        vertex(buffer, matrix, normal, light, w, 0.0F, -l, 1, 0);
        vertex(buffer, matrix, normal, light, w, 0.0F, l, 1, 1);
        vertex(buffer, matrix, normal, light, -w, 0.0F, l, 0, 1);
        // 背侧面（Y=-0.5），保证从下方也能看到
        vertex(buffer, matrix, normal, light, w, 0.0F, -l, 1, 0);
        vertex(buffer, matrix, normal, light, -w, 0.0F, -l, 0, 0);
        vertex(buffer, matrix, normal, light, -w, 0.0F, l, 0, 1);
        vertex(buffer, matrix, normal, light, w, 0.0F, l, 1, 1);
    }

    /** 1.20.1 顶点写法：Matrix4f/Matrix3f + 链式 uv2/uv/normal，最后 endVertex() */
    private static void vertex(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
                               int light, float x, float y, float z, int u, int v) {
        buffer.vertex(matrix, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
