package org.gwfx.zuoyanmod.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;
import org.gwfx.zuoyanmod.Zuoyanmod;

/**
 * 超级电能机枪豌豆及子弹的模型层定义。
 */
public final class PeaModelLayers {

    public static final ModelLayerLocation SUPER_ELECTRIC_GATLING_PEA = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "super_electric_gatling_pea"), "main");

    public static final ModelLayerLocation ELECTRO_PEA_BULLET = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "electro_pea_bullet"), "main");

    private PeaModelLayers() {}
}

