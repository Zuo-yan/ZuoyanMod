package org.gwfx.zuoyanmod.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.entity.RickEntity;

/**
 * 瑞克的渲染器：用玩家形状的人形模型贴自定义皮肤。
 *
 * <p>模型层来自 {@link RickModelLayers#RICK_BODY}（自己烘的 64x64 人形层），
 * 不直接复用 {@code ModelLayers.PLAYER} 的原因见那个类的注释。</p>
 *
 * <p>1.20.1 适配：渲染器没有 render state 分层，{@code getTextureLocation} 直接收实体；
 * 父类加了头部装饰层与鞘翅层，这里再补一个 {@link ItemInHandLayer}（1.20.1 的父类不自带），
 * 让它拿在手里的东西能正常显示。</p>
 */
public class RickRenderer extends HumanoidMobRenderer<RickEntity, HumanoidModel<RickEntity>> {

    /** 皮肤贴图位置：assets/zuoyanmod/textures/entity/rick.png */
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Zuoyanmod.MODID, "textures/entity/rick.png");

    public RickRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(RickModelLayers.RICK_BODY)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(RickEntity entity) {
        return TEXTURE;
    }
}
