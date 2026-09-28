package org.gwfx.zuoyanmod.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.entity.RickEntity;

/**
 * 瑞克的渲染器：用玩家形状的人形模型贴自定义皮肤。
 *
 * <p>模型层来自 {@link RickModelLayers#RICK_BODY}（自己烘的 64x64 人形层），
 * 不直接复用 {@code ModelLayers.PLAYER} 的原因见那个类的注释。
 *
 * <p>父类 {@link HumanoidMobRenderer} 已自动挂上头部装饰层与鞘翅层，
 * 这里再补一个 {@link ItemInHandLayer}，让它拿在手里的东西能正常显示。
 *
 * <p>不标 {@code @OnlyIn(Dist.CLIENT)}：本项目约定靠包名 + Dist.CLIENT 事件订阅
 * 来区分客户端专用代码，加注解只会让 NeoForge 抛 onlyin 警告。
 */
public class RickRenderer extends HumanoidMobRenderer<RickEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    /** 皮肤贴图位置：assets/zuoyanmod/textures/entity/rick.png */
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "textures/entity/rick.png");

    public RickRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(RickModelLayers.RICK_BODY)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this));
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return TEXTURE;
    }
}
