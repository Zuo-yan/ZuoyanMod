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
import net.minecraft.world.phys.AABB;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.entity.PrimordialBlackHoleEntity;

/**
 * 原始黑洞的渲染器：一张永远正对相机的黑盘（事件视界）+ 一圈绕视线自转的能量涡流。
 *
 * <p>视觉参考 Iron's Spells 'n Spellbooks 的黑洞：它的做法是"billboard 中心贴图 +
 * 外层用原版 {@code RenderType.energySwirl} 叠一层转动的能量"。这里沿用同一套组合，
 * 因为 {@code energySwirl} 的管线自带 <b>ADDITIVE 混合 + 自发光 + 关闭背面剔除</b>，
 * 等于白送一层"发光能量"的观感，不用自己写 shader 也不用自定义 RenderType（少一堆兼容坑）。
 *
 * <p><b>⚠️ 涡流必须发四边形，不能发三角形。</b>
 * {@code energySwirl} 走的是实体四边形顶点格式，顶点会被**按 4 个一组**解释成四边形。
 * 照抄三角链的话每 4 个顶点会被拼成一片随机四边形，画面会糊成一团。
 * 所以这里改成"一整圈四边形带"（{@link #buildSwirlRing}），再靠纹理里的螺旋纹路提供细节。
 *
 * <p>1.21.1 渲染管线：经典 {@code EntityRenderer<PrimordialBlackHoleEntity>} 单泛型、
 * 无 RenderState——半径、年龄等直接在 {@link #render} 里从实体读取。
 */
public class PrimordialBlackHoleRenderer extends EntityRenderer<PrimordialBlackHoleEntity> {

    /** 事件视界贴图（平面贴图：整张图就是那个圆盘） */
    private static final ResourceLocation CORE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Zuoyanmod.MODID, "textures/entity/primordial_black_hole_core.png");

    /** 能量涡流贴图（参数空间贴图：x = 绕圆周的角度，y = 半径方向） */
    private static final ResourceLocation SWIRL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Zuoyanmod.MODID, "textures/entity/primordial_black_hole_swirl.png");

    /** 黑盘：硬边缘透明，适合"实体球体"的剪影 */
    private static final RenderType CORE_TYPE = RenderType.entityCutout(CORE_TEXTURE);

    /**
     * 涡流：原版能量涡流管线（ADDITIVE + EMISSIVE）。
     * <p>uOffset/vOffset 传 0 —— 那是"纹理坐标整体平移"用的，我们靠几何旋转做自转。
     * 传 0 还有一个好处：RenderType 可以做成静态常量，不必每帧新建对象
     * （每次 new 都会在渲染管线缓存里多一条记录）。
     */
    private static final RenderType SWIRL_TYPE = RenderType.energySwirl(SWIRL_TEXTURE, 0.0F, 0.0F);

    /** 黑盘 quad 的缩放系数：最终边长 = 视觉半径 × 该值。1.05 让盘略大于"半径"本身，边缘更饱满。 */
    private static final float CORE_QUAD_SCALE = 1.05F;

    /** 涡流环带的内/外半径（× 视觉半径）。内圈压在盘缘上，形成一圈紧贴黑球的光环。 */
    private static final float SWIRL_INNER = 0.40F;
    private static final float SWIRL_OUTER = 0.95F;

    /** 环带分段数。48 段在 2~4 格的尺度上已经看不出棱角。 */
    private static final int SWIRL_SEGMENTS = 48;

    /** 涡流自转角速度（度/tick）。10 秒能转 4 圈半，够快但不会看晕。 */
    private static final float SWIRL_SPIN_DEG_PER_TICK = 9.0F;

    /** 涡流顶点色（ARGB）：淡紫。ADDITIVE 混合下，这个颜色直接决定叠加亮度。 */
    private static final int SWIRL_TINT = 0xFF9B6BFF;

    public PrimordialBlackHoleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** 能量体满亮：不受环境光影响，洞里洞里都是亮的。 */
    @Override
    protected int getBlockLightLevel(PrimordialBlackHoleEntity entity, BlockPos blockPos) {
        return 15;
    }

    /**
     * 剔除箱必须手动撑开。
     * <p>本体碰撞箱只有 2×2×2（见 {@code EntityRegistry} 里的注释），而视觉半径到
     * 2.2 格、涡流还要再往外一圈。不撑开的话，玩家贴近时黑洞本体在视锥外，
     * 整团特效会被一起剔掉 —— 表现为"走到跟前黑洞就消失"。
     */
    @Override
    public boolean shouldRender(PrimordialBlackHoleEntity entity, net.minecraft.client.renderer.culling.Frustum camera,
                                double camX, double camY, double camZ) {
        if (!entity.shouldRender(camX, camY, camZ)) {
            return false;
        }
        if (entity.noCulling) {
            return true;
        }
        AABB aabb = entity.getBoundingBox().inflate(PrimordialBlackHoleEntity.VISUAL_RADIUS + 1.5D);
        return camera.isVisible(aabb);
    }

    @Override
    public void render(PrimordialBlackHoleEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float age = entity.tickCount + partialTicks;

        // 张开程度**直接问实体类要**，不要在这里另写一套曲线。
        // 服务端的牵引半径用的是同一个 scaleFactor()，两边共用一个函数才能保证
        // "看着在长大"和"吸力范围在变大"是同一件事 —— 上一版的毛病就是这两条曲线脱钩了
        // （视觉在长大、吸力却是恒定的），玩家一眼就能看出是假的。
        float scale = entity.getVisualRadius() * PrimordialBlackHoleEntity.scaleFactor(age);

        if (scale > 0.01F) {
            poseStack.pushPose();
            // billboard：把本地坐标系转到相机朝向，之后所有绘制都在"屏幕平面"上做
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

            // ---- 事件视界：黑盘 ----
            poseStack.pushPose();
            poseStack.scale(scale * CORE_QUAD_SCALE, scale * CORE_QUAD_SCALE, 1.0F);
            VertexConsumer coreBuffer = bufferSource.getBuffer(CORE_TYPE);
            buildCoreQuad(packedLight, poseStack.last(), coreBuffer);
            poseStack.popPose();

            // ---- 能量涡流：绕视线自转的四边形环带 ----
            poseStack.pushPose();
            // 朝相机方向挪一点点：即使两条管线的批次顺序变了，深度测试也能保证
            // 涡流永远叠在黑盘之上
            poseStack.translate(0.0F, 0.0F, 0.01F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(age * SWIRL_SPIN_DEG_PER_TICK));
            float inner = scale * SWIRL_INNER;
            float outer = scale * SWIRL_OUTER;
            VertexConsumer swirlBuffer = bufferSource.getBuffer(SWIRL_TYPE);
            buildSwirlRing(packedLight, poseStack.last(), swirlBuffer, inner, outer);
            poseStack.popPose();

            poseStack.popPose();
        }

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    /**
     * 黑盘：一个以原点为中心的 1×1 四边形（缩放由调用方通过 poseStack 完成）。
     * <p>顶点顺序沿用本项目 {@code CausalityBulletRenderer#buildBallQuad}（那边验证过朝向正确），
     * 因为 {@code entityCutout} 管线是开背面剔除的，绕序反了会被剔掉看不见。
     * <p>贴图本身是径向对称的，所以上下翻转与否不影响观感。
     */
    private static void buildCoreQuad(int light, PoseStack.Pose pose, VertexConsumer buffer) {
        float h = 0.5F;
        vertex(buffer, pose, light, -h, -h, 0.0F, 0.0F, 0.0F, 0xFFFFFFFF);
        vertex(buffer, pose, light,  h, -h, 0.0F, 1.0F, 0.0F, 0xFFFFFFFF);
        vertex(buffer, pose, light,  h,  h, 0.0F, 1.0F, 1.0F, 0xFFFFFFFF);
        vertex(buffer, pose, light, -h,  h, 0.0F, 0.0F, 1.0F, 0xFFFFFFFF);
    }

    /**
     * 一整圈四边形带（外环 - 内环）。
     *
     * <p><b>每 4 个顶点 = 1 片</b>，这是实体四边形顶点格式的要求，不能改成三角形。
     *
     * <p>UV 的取法：u 沿圆周展开（0→1 绕一圈）、v 沿半径方向（0 = 内圈，1 = 外圈）。
     * 贴图就是按这个参数空间画的 —— 螺旋纹在 u 方向必须能首尾相接，否则环上会出现一道接缝。
     */
    private static void buildSwirlRing(int light, PoseStack.Pose pose,
                                       VertexConsumer buffer, float inner, float outer) {
        float twoPi = (float) (Math.PI * 2.0);
        for (int i = 0; i < SWIRL_SEGMENTS; i++) {
            float a0 = i * twoPi / SWIRL_SEGMENTS;
            float a1 = (i + 1) * twoPi / SWIRL_SEGMENTS;
            float sin0 = Mth.sin(a0);
            float cos0 = Mth.cos(a0);
            float sin1 = Mth.sin(a1);
            float cos1 = Mth.cos(a1);
            float u0 = (float) i / SWIRL_SEGMENTS;
            float u1 = (float) (i + 1) / SWIRL_SEGMENTS;

            vertex(buffer, pose, light, cos0 * inner, sin0 * inner, 0.0F, u0, 0.0F, SWIRL_TINT);
            vertex(buffer, pose, light, cos0 * outer, sin0 * outer, 0.0F, u0, 1.0F, SWIRL_TINT);
            vertex(buffer, pose, light, cos1 * outer, sin1 * outer, 0.0F, u1, 1.0F, SWIRL_TINT);
            vertex(buffer, pose, light, cos1 * inner, sin1 * inner, 0.0F, u1, 0.0F, SWIRL_TINT);
        }
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, int light,
                               float x, float y, float z, float u, float v, int color) {
        buffer.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                // EMISSIVE + NO_OVERLAY 的管线其实用不到 overlay，但顶点格式里有这一项，仍要写
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(PrimordialBlackHoleEntity entity) {
        return CORE_TEXTURE;
    }
}
