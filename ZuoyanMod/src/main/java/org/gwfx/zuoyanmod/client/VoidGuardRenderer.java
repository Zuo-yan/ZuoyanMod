package org.gwfx.zuoyanmod.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.ResourceLocation;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.entity.VoidGuardEntity;

/**
 * 湮灭侍卫的渲染器：标准人形模型贴侍卫皮肤（暗甲紫纹）。
 */
public class VoidGuardRenderer extends HumanoidMobRenderer<VoidGuardEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    /** 皮肤贴图位置：assets/zuoyanmod/textures/entity/void_guard.png */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Zuoyanmod.MODID, "textures/entity/void_guard.png");

    public VoidGuardRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(BossModelLayers.VOID_GUARD_BODY)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this));
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public ResourceLocation getTextureLocation(HumanoidRenderState state) {
        return TEXTURE;
    }
}
