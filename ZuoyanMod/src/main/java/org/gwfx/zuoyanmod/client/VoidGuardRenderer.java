package org.gwfx.zuoyanmod.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.entity.VoidGuardEntity;

/**
 * 湮灭侍卫的渲染器：标准人形模型贴侍卫皮肤（暗甲紫纹）。
 *
 * <p>1.21.1 经典渲染器：无 RenderState，{@link HumanoidMobRenderer} 构造时已自动挂
 * 头部装饰层、鞘翅层与手持物品层，无需手动 addLayer。
 */
public class VoidGuardRenderer extends HumanoidMobRenderer<VoidGuardEntity, HumanoidModel<VoidGuardEntity>> {

    /** 皮肤贴图位置：assets/zuoyanmod/textures/entity/void_guard.png */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Zuoyanmod.MODID, "textures/entity/void_guard.png");

    public VoidGuardRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(BossModelLayers.VOID_GUARD_BODY)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(VoidGuardEntity entity) {
        return TEXTURE;
    }
}
