package org.gwfx.zuoyanmod.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.entity.SuperElectricGatlingPeaEntity;

/**
 * 超级电能机枪豌豆实体渲染器。
 */
public class SuperElectricGatlingPeaRenderer extends MobRenderer<SuperElectricGatlingPeaEntity, SuperElectricGatlingPeaRenderState, SuperElectricGatlingPeaModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "textures/entity/super_electric_gatling_pea.png");

    public SuperElectricGatlingPeaRenderer(EntityRendererProvider.Context context) {
        super(context, new SuperElectricGatlingPeaModel(context.bakeLayer(PeaModelLayers.SUPER_ELECTRIC_GATLING_PEA)), 0.5F);
    }

    @Override
    public Identifier getTextureLocation(SuperElectricGatlingPeaRenderState state) {
        return TEXTURE;
    }

    @Override
    public SuperElectricGatlingPeaRenderState createRenderState() {
        return new SuperElectricGatlingPeaRenderState();
    }

    @Override
    public void extractRenderState(SuperElectricGatlingPeaEntity entity, SuperElectricGatlingPeaRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.isShooting = entity.isShooting();
       state.isCarried = entity.isCarried();
        state.idleAnimation.copyFrom(entity.idleAnimationState);
        state.shootAnimation.copyFrom(entity.shootAnimationState);
    }
}

