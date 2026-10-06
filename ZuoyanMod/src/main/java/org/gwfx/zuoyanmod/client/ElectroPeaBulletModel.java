package org.gwfx.zuoyanmod.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * 电能豌豆子弹模型（基于 electro_pea_bullet.bbmodel 烘焙）。
 * 包含能量球核心、交叉电能轨道环、四向电弧火花与等离子彗尾。
 */
public class ElectroPeaBulletModel extends EntityModel<ElectroPeaBulletRenderState> {

    private final ModelPart root;
    private final ModelPart core;
    private final ModelPart rings;
    private final ModelPart sparks;
    private final ModelPart tail;

    public ElectroPeaBulletModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucentEmissive);
        this.root = root.getChild("root");
        this.core = this.root.getChild("core");
        this.rings = this.root.getChild("rings");
        this.sparks = this.root.getChild("sparks");
        this.tail = this.root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition part_root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(),
                PartPose.offset(0F, 0.0F, 0F));
        PartDefinition part_core = part_root.addOrReplaceChild("core", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3F, -3F, -3F, 6F, 6F, 6F)
                .texOffs(25, 0).addBox(-2F, -2F, -4F, 4F, 4F, 1F)
                .texOffs(36, 0).addBox(-2F, -2F, 3F, 4F, 4F, 1F)
                .texOffs(47, 0).addBox(-2F, -4F, -2F, 4F, 1F, 4F)
                .texOffs(0, 13).addBox(-2F, 3F, -2F, 4F, 1F, 4F)
                .texOffs(17, 13).addBox(-4F, -2F, -2F, 1F, 4F, 4F)
                .texOffs(28, 13).addBox(3F, -2F, -2F, 1F, 4F, 4F),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition part_rings = part_root.addOrReplaceChild("rings", CubeListBuilder.create()
                .texOffs(39, 13).addBox(-3.5F, -0.5F, -4.5F, 7.0F, 1.0F, 1.0F)
                .texOffs(0, 22).addBox(-3.5F, -0.5F, 3.5F, 7.0F, 1.0F, 1.0F)
                .texOffs(17, 22).addBox(-4.5F, -0.5F, -3.5F, 1.0F, 1.0F, 7.0F)
                .texOffs(34, 22).addBox(3.5F, -0.5F, -3.5F, 1.0F, 1.0F, 7.0F),
                PartPose.offset(0F, 0F, 0F));
        part_rings.addOrReplaceChild("orbit_top_f", CubeListBuilder.create()
                .texOffs(51, 22).addBox(-2.5F, -4.5F, -2.5F, 5.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0.0F, 0.0F, 0.785398F));
        part_rings.addOrReplaceChild("orbit_bot_f", CubeListBuilder.create()
                .texOffs(0, 31).addBox(-2.5F, 3.5F, -2.5F, 5.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0.0F, 0.0F, 0.785398F));
        PartDefinition part_sparks = part_root.addOrReplaceChild("sparks", CubeListBuilder.create(),
                PartPose.offset(0F, 0F, 0F));
        part_sparks.addOrReplaceChild("spark_tl_1", CubeListBuilder.create()
                .texOffs(13, 31).addBox(-5F, -4F, -3F, 2F, 2F, 2F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0.174533F, -0.261799F, 0.349066F));
        part_sparks.addOrReplaceChild("spark_tl_2", CubeListBuilder.create()
                .texOffs(22, 31).addBox(-7F, -6F, -4F, 2F, 2F, 2F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0.261799F, -0.349066F, 0.610865F));
        part_sparks.addOrReplaceChild("spark_tr_1", CubeListBuilder.create()
                .texOffs(31, 31).addBox(3F, -4F, -3F, 2F, 2F, 2F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0.174533F, 0.261799F, -0.349066F));
        part_sparks.addOrReplaceChild("spark_tr_2", CubeListBuilder.create()
                .texOffs(40, 31).addBox(5F, -6F, -4F, 2F, 2F, 2F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0.261799F, 0.349066F, -0.610865F));
        part_sparks.addOrReplaceChild("spark_bl_1", CubeListBuilder.create()
                .texOffs(49, 31).addBox(-5F, 2F, -3F, 2F, 2F, 2F),
                PartPose.offsetAndRotation(0F, 0F, 0F, -0.174533F, -0.261799F, -0.349066F));
        part_sparks.addOrReplaceChild("spark_bl_2", CubeListBuilder.create()
                .texOffs(0, 36).addBox(-7F, 4F, -4F, 2F, 2F, 2F),
                PartPose.offsetAndRotation(0F, 0F, 0F, -0.261799F, -0.349066F, -0.610865F));
        part_sparks.addOrReplaceChild("spark_br_1", CubeListBuilder.create()
                .texOffs(9, 36).addBox(3F, 2F, -3F, 2F, 2F, 2F),
                PartPose.offsetAndRotation(0F, 0F, 0F, -0.174533F, 0.261799F, 0.349066F));
        part_sparks.addOrReplaceChild("spark_br_2", CubeListBuilder.create()
                .texOffs(18, 36).addBox(5F, 4F, -4F, 2F, 2F, 2F),
                PartPose.offsetAndRotation(0F, 0F, 0F, -0.261799F, 0.349066F, 0.610865F));
        PartDefinition part_tail = part_root.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(27, 36).addBox(-1.5F, -1.5F, 0F, 3.0F, 3.0F, 4F),
                PartPose.offset(0F, 0F, 4F));
        part_tail.addOrReplaceChild("tail_core_2", CubeListBuilder.create()
                .texOffs(42, 36).addBox(-1.0F, -1.0F, 0F, 2.0F, 2.0F, 4F),
                PartPose.offsetAndRotation(0F, 0F, 4F, 0.0F, 0.0F, 0.0F));
        part_tail.addOrReplaceChild("tail_core_tip", CubeListBuilder.create()
                .texOffs(55, 36).addBox(-0.5F, -0.5F, 0F, 1.0F, 1.0F, 3F),
                PartPose.offsetAndRotation(0F, 0F, 8F, 0.0F, 0.0F, 0.0F));
        part_tail.addOrReplaceChild("tail_spark_u1", CubeListBuilder.create()
                .texOffs(0, 44).addBox(-0.5F, -3.5F, 0F, 1.0F, 2.0F, 3F),
                PartPose.offsetAndRotation(0F, 0F, 1F, -0.261799F, 0.0F, 0.0F));
        part_tail.addOrReplaceChild("tail_spark_u2", CubeListBuilder.create()
                .texOffs(9, 44).addBox(-0.5F, -4.5F, 0F, 1.0F, 1.5F, 3F),
                PartPose.offsetAndRotation(0F, 0F, 4F, -0.436332F, 0.0F, 0.0F));
        part_tail.addOrReplaceChild("tail_spark_d1", CubeListBuilder.create()
                .texOffs(18, 44).addBox(-0.5F, 1.5F, 0F, 1.0F, 2.0F, 3F),
                PartPose.offsetAndRotation(0F, 0F, 1F, 0.261799F, 0.0F, 0.0F));
        part_tail.addOrReplaceChild("tail_spark_d2", CubeListBuilder.create()
                .texOffs(27, 44).addBox(-0.5F, 3.0F, 0F, 1.0F, 1.5F, 3F),
                PartPose.offsetAndRotation(0F, 0F, 4F, 0.436332F, 0.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(ElectroPeaBulletRenderState state) {
        super.setupAnim(state);

        // 高速旋转动画效果（根据 ageInTicks 与飞行速度动态自转）
        float spin = state.ageInTicks * 0.8F;
        this.core.zRot = -spin * 1.5F;
        this.rings.xRot = (float) Math.sin(spin * 0.5F) * 0.5F;
        this.rings.yRot = spin * 1.2F;
        this.rings.zRot = -spin * 2.0F;

        this.sparks.zRot = -spin * 2.5F;

        float tailWiggle = (float) Math.sin(state.ageInTicks * 1.2F) * 0.15F;
        this.tail.yRot = tailWiggle;
        this.tail.xRot = (float) Math.cos(state.ageInTicks * 1.2F) * 0.15F;
    }
}
