package org.gwfx.zuoyanmod.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;

import org.gwfx.zuoyanmod.Zuoyanmod;

/**
 * 瑞克的玩家形状人形模型。
 *
 * <p>为什么不直接用原版的人形模型层：
 * <ul>
 *   <li>{@code ModelLayers.ZOMBIE} 那层是 {@code HumanoidModel.createMesh(...)}，
 *       只有"帽子"一层薄壳，<b>没有</b>外套/双层袖子/裤腿；</li>
 *   <li>{@code ModelLayers.PLAYER} 那层尺寸对得上，但它是 {@code PlayerModel}，
 *       被硬绑死在玩家渲染上。</li>
 * </ul>
 * 所以这里用 {@code HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F)}
 * 自己烘一层 64x64 的人形模型——它的盒子尺寸/贴图偏移与玩家皮肤布局完全一致，
 * 因此任意标准 64x64 皮肤都能正确贴上去。</p>
 *
 * <p>必须注册到 {@code EntityRenderersEvent.RegisterLayerDefinitions}。</p>
 */
public final class RickModelLayers {

    /** 瑞克皮肤模型层。 */
    public static final ModelLayerLocation RICK_BODY = new ModelLayerLocation(
            new ResourceLocation(Zuoyanmod.MODID, "rick"), "main");

    private RickModelLayers() {}

    public static LayerDefinition createBodyLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64);
    }
}
