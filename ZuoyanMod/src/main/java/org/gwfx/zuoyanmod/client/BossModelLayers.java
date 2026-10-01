package org.gwfx.zuoyanmod.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import org.gwfx.zuoyanmod.Zuoyanmod;

/**
 * 湮灭势力（君主 / 侍卫）的人形模型层。
 *
 * <p>基座与 {@link RickModelLayers} 同款：{@code HumanoidModel.createMesh} 烘出的
 * 标准 64x64 人形层，贴图按玩家皮肤布局走。
 *
 * <p>君主的区别在两个附加部件（挂在头部下，跟随转头）：
 * <ul>
 *   <li>{@code crown}——头顶金冠环，占帽子层 UV 区（texOffs(32,0)，8x2x8），
 *       皮肤上不画帽层别的部分，两者共用一个区域互不打架；</li>
 *   <li>{@code gem}——冠前宝石，占 (56,10) 的 2x2x2 小块 UV。</li>
 * </ul>
 */
public final class BossModelLayers {

    public static final ModelLayerLocation VOID_MONARCH_BODY = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "void_monarch"), "main");

    public static final ModelLayerLocation VOID_GUARD_BODY = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "void_guard"), "main");

    private BossModelLayers() {}

    /** 湮灭君主：高精立体独立骨骼（含披风、金冠、虚空光轮双翼与内置湮灭君王之刃）。 */
    public static LayerDefinition createMonarchBodyLayer() {
        return VoidMonarchModel.createBodyLayer();
    }

    /** 湮灭侍卫：普通 64x64 人形层，与瑞克同款。 */
    public static LayerDefinition createGuardBodyLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64);
    }
}
