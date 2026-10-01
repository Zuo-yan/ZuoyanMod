package org.gwfx.zuoyanmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.entity.ElectroPeaBulletEntity;

/**
 * 电能豌豆子弹实体渲染器。
 * 烘焙 3D 模型渲染，带自身光照（BlockLight=15）与高能电浆发光效果。
 */
public class ElectroPeaBulletRenderer extends EntityRenderer<ElectroPeaBulletEntity, ElectroPeaBulletRenderState> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "textures/entity/electro_pea_bullet.png");

    private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE);

    private final ElectroPeaBulletModel model;

    public ElectroPeaBulletRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ElectroPeaBulletModel(context.bakeLayer(PeaModelLayers.ELECTRO_PEA_BULLET));
    }

    @Override
    protected int getBlockLightLevel(ElectroPeaBulletEntity entity, BlockPos blockPos) {
        // 电能子弹自发光
        return 15;
    }

    @Override
    public ElectroPeaBulletRenderState createRenderState() {
        return new ElectroPeaBulletRenderState();
    }

    @Override
    public void extractRenderState(ElectroPeaBulletEntity entity, ElectroPeaBulletRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.velocity = entity.getDeltaMovement();
        state.yRot = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        state.xRot = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
    }

    @Override
    public void submit(ElectroPeaBulletRenderState state, PoseStack poseStack,
                       SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();

        // 旋转朝向子弹飞行方向
        poseStack.rotateDegrees(Axis.YP, state.yRot - 180.0F);
        poseStack.rotateDegrees(Axis.XP, state.xRot);

        // 缩放尺寸适配（0.45倍，子弹小巧凌厉）
        poseStack.scale(0.45F, 0.45F, 0.45F);

        // 提交 3D 模型
        submitNodeCollector.submitModel(
                this.model,
                state,
                poseStack,
                RENDER_TYPE,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor
        );

        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
}

