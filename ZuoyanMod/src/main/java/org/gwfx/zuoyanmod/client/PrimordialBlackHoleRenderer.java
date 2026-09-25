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
import org.gwfx.zuoyanmod.entity.PrimordialBlackHoleEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 原始黑洞的渲染器：一张永远正对相机的黑盘（事件视界）+ 一圈绕视线自转的能量涡流。
 *
 * <p>视觉参考 Iron's Spells 'n Spellbooks 的黑洞：它的做法是"billboard 中心贴图 +
 * 外层用原版 {@code RenderType.energySwirl} 叠一层转动的能量"。这里沿用同一套组合，
 * 因为 {@code energySwirl} 的管线自带 <b>ADDITIVE 混合 + 关闭背面剔除</b>，
 * 等于白送一层"发光能量"的观感，不用自己写 shader 也不用自定义 RenderType（少一堆兼容坑）。
 *
 * <p><b>⚠️ 与参考实现最大的差别：涡流必须发四边形，不能发三角形。</b>
 * 参考实现用的是"随机旋转的三角链"，但 {@code energySwirl} 的管线是
 * {@code VertexFormat.Mode.QUADS}，顶点会被**按 4 个一组**解释成四边形。
 * 照抄三角链的话每 4 个顶点会被拼成一片随机四边形，画面会糊成一团。
 * 所以这里改成"一整圈四边形带"（{@link #buildSwirlRing}），再靠纹理里的螺旋纹路提供细节。
 *
 * <h2>1.20.1 适配</h2>
 * 26.3 那套"render state 分层 + {@code RenderPipelines} + {@code SubmitNodeCollector}"在
 * 1.20.1 全部不存在，这里按下述方式等价重写：
 * <ul>
 *   <li>泛型从 {@code EntityRenderer<实体, 自定义RenderState>} 退化为
 *       {@code EntityRenderer<实体>}；{@code createRenderState / extractRenderState / submit}
 *       三个钩子全部取消，额外数据（视觉半径）直接从实体读
 *       —— 客户端实体是通过 {@code SynchedEntityData} 同步的，读得到。</li>
 *   <li>绘制入口从 {@code submit(state, poseStack, collector, camera)} 换成
 *       {@code render(实体, yaw, partialTick, poseStack, buffer, packedLight)}。
 *       1.20.1 的 {@code EntityRenderDispatcher} 在调用前已经把 PoseStack 平移到实体位置
 *       且**没有**施加任何旋转，所以这里可以放心地做 billboard。</li>
 *   <li>"永远正对相机"：26.3 是 {@code poseStack.rotate(camera.orientation)}，
 *       1.20.1 改成 {@code poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation())}
 *       —— 这与原版 {@code renderNameTag} 用的是同一个四元数。</li>
 *   <li>{@code RenderTypes.entityCutout(...)} → {@code RenderType.entityCutoutNoCull(...)}；
 *       {@code RenderTypes.energySwirl(...)} → {@code RenderType.energySwirl(纹理, u, v)}，
 *       两边都是 {@code DefaultVertexFormat.NEW_ENTITY} + QUADS 拓扑，u/v 偏移同样传 0，
 *       所以 RenderType 依旧可以做静态常量。</li>
 *   <li>顶点写入：26.3 是 {@code buffer.addVertex(pose, x, y, z)} 返回 {@code VertexConsumer}
 *       再链式 {@code setColor / setUv / setOverlay / setLight / setNormal}；
 *       1.20.1 是 {@code buffer.vertex(Matrix4f, x, y, z)} + {@code color / uv /
 *       overlayCoords / uv2 / normal(Matrix3f, ...)}，并且**必须显式调 {@code endVertex()}**。</li>
 *   <li>光照：26.3 的 {@code state.lightCoords} 由管线算好；1.20.1 直接复用
 *       {@code render(...)} 传入的 {@code packedLight}，配合覆写
 *       {@code getBlockLightLevel → 15} 达到"能量体满亮"。</li>
 *   <li>剔除箱：26.3 覆写 {@code EntityRenderer#getBoundingBoxForCulling(实体, partialTicks)}；
 *       1.20.1 没有这个钩子，改由实体侧覆写 {@code Entity#getBoundingBoxForCulling()}
 *       （见 {@code PrimordialBlackHoleEntity}）。</li>
 *   <li>{@code Identifier} → {@code ResourceLocation}；动画进度取
 *       {@code entity.tickCount + partialTick}（26.3 的 {@code ageInTicks} 是基类算好的）。</li>
 * </ul>
 */
public class PrimordialBlackHoleRenderer extends EntityRenderer<PrimordialBlackHoleEntity> {

    /** 事件视界贴图（平面贴图：整张图就是那个圆盘） */
    private static final ResourceLocation CORE_TEXTURE =
            new ResourceLocation(Zuoyanmod.MODID, "textures/entity/primordial_black_hole_core.png");

    /** 能量涡流贴图（参数空间贴图：x = 绕圆周的角度，y = 半径方向） */
    private static final ResourceLocation SWIRL_TEXTURE =
            new ResourceLocation(Zuoyanmod.MODID, "textures/entity/primordial_black_hole_swirl.png");

    /**
     * 黑盘：硬边缘透明，适合"实体球体"的剪影。
     * <p>26.3 用的是带背面剔除的 {@code entityCutout}；这里换成
     * {@code entityCutoutNoCull} —— 单张 billboard  quad 的绕序一旦因相机朝向变化被判成背面，
     * 整块黑盘就会消失，关掉剔除更稳（涡流那层本来就是 NO_CULL）。
     */
    private static final RenderType CORE_TYPE = RenderType.entityCutoutNoCull(CORE_TEXTURE);

    /**
     * 涡流：原版能量涡流管线（ADDITIVE + NO_CULL + LIGHTMAP + OVERLAY）。
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

    /** 涡流自转角速度（度/tick）。20 秒能转 9 圈，够快但不会看晕。 */
    private static final float SWIRL_SPIN_DEG_PER_TICK = 9.0F;

    /** 涡流顶点色（ARGB）：淡紫。ADDITIVE 混合下，这个颜色直接决定叠加亮度。 */
    private static final int SWIRL_TINT = 0xFF9B6BFF;

    public PrimordialBlackHoleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** 能量体满亮：不受环境光影响，洞里洞外都是亮的。 */
    @Override
    protected int getBlockLightLevel(PrimordialBlackHoleEntity entity, BlockPos blockPos) {
        return 15;
    }

    @Override
    public ResourceLocation getTextureLocation(PrimordialBlackHoleEntity entity) {
        return CORE_TEXTURE;
    }

    @Override
    public void render(PrimordialBlackHoleEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // 26.3 的 state.ageInTicks 就是 tickCount + partialTicks
        float age = (float) entity.tickCount + partialTick;

        // 张开程度**直接问实体类要**，不要在这里另写一套曲线。
        // 服务端的牵引半径用的是同一个 scaleFactor()，两边共用一个函数才能保证
        // "看着在长大"和"吸力范围在变大"是同一件事 —— 上一版的毛病就是这两条曲线脱钩了
        // （视觉在长大、吸力却是恒定的），玩家一眼就能看出是假的。
        float scale = entity.getVisualRadius() * PrimordialBlackHoleEntity.scaleFactor(age);

        if (scale <= 0.01F) {
            // 还没长出来 / 已经收束完：什么都不画（基类只负责名牌，仍然要走一遍）
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }

        poseStack.pushPose();
        // billboard：把本地坐标系转到相机朝向，之后所有绘制都在"屏幕平面"上做
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        // ---- 事件视界：黑盘 ----
        poseStack.pushPose();
        poseStack.scale(scale * CORE_QUAD_SCALE, scale * CORE_QUAD_SCALE, 1.0F);
        PoseStack.Pose corePose = poseStack.last();
        buildCoreQuad(buffer.getBuffer(CORE_TYPE), corePose.pose(), corePose.normal(), packedLight);
        poseStack.popPose();

        // ---- 能量涡流：绕视线自转的四边形环带 ----
        poseStack.pushPose();
        // 朝相机方向挪一点点：这样即使两根管线的提交顺序变了，深度测试也能保证
        // 涡流永远叠在黑盘之上
        poseStack.translate(0.0F, 0.0F, 0.01F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(age * SWIRL_SPIN_DEG_PER_TICK));
        float inner = scale * SWIRL_INNER;
        float outer = scale * SWIRL_OUTER;
        PoseStack.Pose swirlPose = poseStack.last();
        buildSwirlRing(buffer.getBuffer(SWIRL_TYPE), swirlPose.pose(), swirlPose.normal(), packedLight, inner, outer);
        poseStack.popPose();

        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    /**
     * 黑盘：一个以原点为中心的 1×1 四边形（缩放由调用方通过 poseStack 完成）。
     * <p>顶点顺序沿用本项目 {@code CausalityBulletRenderer#buildBallQuad}（那边验证过朝向正确），
     * 逆时针绕序、法线朝 +Z —— 因为开了 NoCull，即便绕序反了也仍然可见。
     * <p>贴图本身是径向对称的，所以上下翻转与否不影响观感。
     */
    private static void buildCoreQuad(VertexConsumer buffer, Matrix4f pose, Matrix3f normal, int light) {
        float h = 0.5F;
        vertex(buffer, pose, normal, light, -h, -h, 0.0F, 0.0F, 0.0F, 0xFFFFFFFF);
        vertex(buffer, pose, normal, light, h, -h, 0.0F, 1.0F, 0.0F, 0xFFFFFFFF);
        vertex(buffer, pose, normal, light, h, h, 0.0F, 1.0F, 1.0F, 0xFFFFFFFF);
        vertex(buffer, pose, normal, light, -h, h, 0.0F, 0.0F, 1.0F, 0xFFFFFFFF);
    }

    /**
     * 一整圈四边形带（外环 - 内环）。
     *
     * <p><b>每 4 个顶点 = 1 片</b>，这是 {@code energySwirl} 的 QUADS 拓扑要求的，不能改成三角形。
     *
     * <p>UV 的取法：u 沿圆周展开（0→1 绕一圈）、v 沿半径方向（0 = 内圈，1 = 外圈）。
     * 贴图就是按这个参数空间画的 —— 螺旋纹在 u 方向必须能首尾相接，否则环上会出现一道接缝。
     */
    private static void buildSwirlRing(VertexConsumer buffer, Matrix4f pose, Matrix3f normal,
                                       int light, float inner, float outer) {
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

            vertex(buffer, pose, normal, light, cos0 * inner, sin0 * inner, 0.0F, u0, 0.0F, SWIRL_TINT);
            vertex(buffer, pose, normal, light, cos0 * outer, sin0 * outer, 0.0F, u0, 1.0F, SWIRL_TINT);
            vertex(buffer, pose, normal, light, cos1 * outer, sin1 * outer, 0.0F, u1, 1.0F, SWIRL_TINT);
            vertex(buffer, pose, normal, light, cos1 * inner, sin1 * inner, 0.0F, u1, 0.0F, SWIRL_TINT);
        }
    }

    private static void vertex(VertexConsumer buffer, Matrix4f pose, Matrix3f normal, int light,
                               float x, float y, float z, float u, float v, int color) {
        buffer.vertex(pose, x, y, z)
                .color(color)
                .uv(u, v)
                // NO_CULL 的管线其实用不到 overlay，但顶点格式里有这一项，仍要写
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, 0.0F, 0.0F, 1.0F)
                // ⚠️ 1.20.1 必须显式收尾：26.3 的 addVertex 链是自动结束的，这里不是
                .endVertex();
    }
}
