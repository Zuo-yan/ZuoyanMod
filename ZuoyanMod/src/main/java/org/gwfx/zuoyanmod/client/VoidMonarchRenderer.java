package org.gwfx.zuoyanmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.ResourceLocation;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.entity.VoidMonarchEntity;

/**
 * 湮灭君主的渲染器：标准人形模型 + 金冠部件，整体放大 1.8 倍。
 *
 * <p>模型层 {@link BossModelLayers#VOID_MONARCH_BODY}（人形 + 头部金冠环/宝石），
 * 放大后视觉身高 ≈ 3.2 x 1.8 ≈ 5.8 坐标单位——比碰撞箱（1.6x3.6）显大，
 * 正是「君主俯视凡人」的压迫感来源；碰撞箱维持 3.6 高避免卡天花板。
 *
 * <p><b>两阶段皮肤</b>：常态 {@code void_monarch.png}；血量低于 30% 进入狂暴
 * （{@code applyEnrage}）后切 {@code void_monarch_phase2.png}（白红发光脸 +
 * 红纹制服）。狂暴标记经渲染状态 {@link VoidMonarchRenderState} 从实体同步。
 *
 * <p>帽层方块（vanilla hat）在此隐藏：皮肤上帽区只画金冠的 UV，帽方块若渲染
 * 会把冠的金色 UV 带渗到额头上（冠块与帽块共用 texOffs(32,0) 区域）。
 */
public class VoidMonarchRenderer extends HumanoidMobRenderer<VoidMonarchEntity, VoidMonarchRenderer.VoidMonarchRenderState, HumanoidModel<VoidMonarchRenderer.VoidMonarchRenderState>> {

    /** 整体渲染缩放（碰撞箱不变，仅视觉放大） */
    public static final float SCALE = 1.8F;

    /** 常态皮肤：assets/zuoyanmod/textures/entity/void_monarch.png */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Zuoyanmod.MODID, "textures/entity/void_monarch.png");

    /** 狂暴皮肤：assets/zuoyanmod/textures/entity/void_monarch_phase2.png */
    private static final ResourceLocation TEXTURE_ENRAGED =
            ResourceLocation.fromNamespaceAndPath(Zuoyanmod.MODID, "textures/entity/void_monarch_phase2.png");

    public VoidMonarchRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(BossModelLayers.VOID_MONARCH_BODY)), 1.2F);
        this.addLayer(new ItemInHandLayer<>(this));
        this.model.hat.visible = false;
    }

    @Override
    public VoidMonarchRenderState createRenderState() {
        return new VoidMonarchRenderState();
    }

    @Override
    public void extractRenderState(VoidMonarchEntity entity, VoidMonarchRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.enraged = entity.isEnraged();
    }

    @Override
    protected void scale(VoidMonarchRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(VoidMonarchRenderState state) {
        return state.enraged ? TEXTURE_ENRAGED : TEXTURE;
    }

    /** 渲染状态：只带渲染需要的标记（狂暴与否）。 */
    public static class VoidMonarchRenderState extends HumanoidRenderState {
        public boolean enraged;
    }
}
