package org.gwfx.zuoyanmod.client;

import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * 超级电能机枪豌豆模型（基于 super_electric_gatling_pea.bbmodel 烘焙）。
 * 拥有完整的根叶片、躯干、头部、加特林喷嘴、电能头盔与闪电尖刺结构。
 * 支持平滑待机呼吸律动与高速暴风射击后坐力关键帧动画。
 */
public class SuperElectricGatlingPeaModel extends EntityModel<SuperElectricGatlingPeaRenderState> {

    private final ModelPart root;
    private final ModelPart leaves;
    private final ModelPart stem;
    private final ModelPart head;
    private final ModelPart snout;
    private final ModelPart helmet;
    private final ModelPart lightning;

    private final KeyframeAnimation idleAnimation;
    private final KeyframeAnimation shootAnimation;

    public SuperElectricGatlingPeaModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        this.root = root.getChild("root");
        this.leaves = this.root.getChild("leaves");
        this.stem = this.root.getChild("stem");
        this.head = this.stem.getChild("head");
        this.snout = this.head.getChild("snout");
        this.helmet = this.head.getChild("helmet");
        this.lightning = this.head.getChild("lightning");

        this.idleAnimation = SuperGatlingPeaAnimation.IDLE.bake(root);
        this.shootAnimation = SuperGatlingPeaAnimation.SHOOT.bake(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition part_root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(),
                PartPose.offset(0F, 24.0F, 0F));

        PartDefinition part_leaves = part_root.addOrReplaceChild("leaves", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.5F, -1F, -6F, 5.0F, 1F, 5F)
                .texOffs(32, 0).addBox(-2.5F, -1F, 1F, 5.0F, 1F, 5F)
                .texOffs(64, 0).addBox(-6F, -1F, -2.5F, 5F, 1F, 5.0F)
                .texOffs(96, 0).addBox(1F, -1F, -2.5F, 5F, 1F, 5.0F)
                .texOffs(0, 7).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 1.5F, 3.0F),
                PartPose.offset(0F, 0F, 0F));

        part_leaves.addOrReplaceChild("leaf_front_tip", CubeListBuilder.create()
                .texOffs(21, 0).addBox(-1.5F, -1.2F, -8F, 3.0F, 0.8F, 2F),
                PartPose.offsetAndRotation(0F, 0F, 0F, -0.069813F, 0.0F, 0.0F));
        part_leaves.addOrReplaceChild("leaf_back_tip", CubeListBuilder.create()
                .texOffs(53, 0).addBox(-1.5F, -1.2F, 6F, 3.0F, 0.8F, 2F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0.069813F, 0.0F, 0.0F));
        part_leaves.addOrReplaceChild("leaf_left_tip", CubeListBuilder.create()
                .texOffs(85, 0).addBox(-8F, -1.2F, -1.5F, 2F, 0.8F, 3.0F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0.0F, 0.0F, -0.069813F));
        part_leaves.addOrReplaceChild("leaf_right_tip", CubeListBuilder.create()
                .texOffs(117, 0).addBox(6F, -1.2F, -1.5F, 2F, 0.8F, 3.0F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0.0F, 0.0F, 0.069813F));

        PartDefinition part_stem = part_root.addOrReplaceChild("stem", CubeListBuilder.create(),
                PartPose.offset(0F, -1F, 0F));
        part_stem.addOrReplaceChild("stem_lower", CubeListBuilder.create()
                .texOffs(13, 7).addBox(-1F, -5F, -1F, 2F, 5F, 2F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0.174533F, 0.0F, 0.0F));
        part_stem.addOrReplaceChild("stem_upper", CubeListBuilder.create()
                .texOffs(22, 7).addBox(-1F, -5F, -0.5F, 2F, 5.2F, 2.0F),
                PartPose.offsetAndRotation(0F, -4.5F, -0.5F, -0.20944F, 0.0F, 0.0F));
        part_stem.addOrReplaceChild("neck_collar_f", CubeListBuilder.create()
                .texOffs(31, 7).addBox(-2F, -0.5F, -1F, 4F, 1F, 1.0F),
                PartPose.offset(0F, -8.5F, 0F));
        part_stem.addOrReplaceChild("neck_collar_b", CubeListBuilder.create()
                .texOffs(42, 7).addBox(-2F, -0.5F, 2F, 4F, 1F, 1.0F),
                PartPose.offset(0F, -8.5F, 0F));
        part_stem.addOrReplaceChild("neck_collar_l", CubeListBuilder.create()
                .texOffs(53, 7).addBox(-2.5F, -0.5F, -0.5F, 1.0F, 1F, 3F),
                PartPose.offset(0F, -8.5F, 0F));
        part_stem.addOrReplaceChild("neck_collar_r", CubeListBuilder.create()
                .texOffs(62, 7).addBox(1.5F, -0.5F, -0.5F, 1.0F, 1F, 3F),
                PartPose.offset(0F, -8.5F, 0F));

        PartDefinition part_head = part_stem.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(71, 7).addBox(-4F, -8F, -4F, 8F, 8F, 8F),
                PartPose.offsetAndRotation(0F, -8.5F, -1F, 0.05236F, 0.0F, 0.0F));

        PartDefinition part_snout = part_head.addOrReplaceChild("snout", CubeListBuilder.create()
                .texOffs(104, 7).addBox(-1.8F, -2.0F, -2.5F, 3.6F, 4.0F, 2.5F),
                PartPose.offset(0F, -3.0F, -4.0F));
        part_snout.addOrReplaceChild("snout_barrel", CubeListBuilder.create()
                .texOffs(0, 24).addBox(-1.8F, -2.0F, -3.0F, 3.6F, 4.0F, 3.0F),
                PartPose.offset(0F, 0F, -2.5F));
        part_snout.addOrReplaceChild("snout_flange", CubeListBuilder.create()
                .texOffs(15, 24).addBox(-2.6F, -2.6F, -2.0F, 5.2F, 5.2F, 2.0F),
                PartPose.offset(0F, 0F, -5.5F));
        part_snout.addOrReplaceChild("snout_core", CubeListBuilder.create()
                .texOffs(32, 24).addBox(-1.8F, -1.8F, -1.1F, 3.6F, 3.6F, 1.1F),
                PartPose.offset(0F, 0F, -6.5F));

        PartDefinition part_helmet = part_head.addOrReplaceChild("helmet", CubeListBuilder.create(),
                PartPose.offset(0F, -5.0F, 0F));
        part_helmet.addOrReplaceChild("helmet_dome", CubeListBuilder.create()
                .texOffs(43, 24).addBox(-4.5F, -5.5F, -4.5F, 9.0F, 5.5F, 9.0F),
                PartPose.offset(0F, 0F, 0F));
        part_helmet.addOrReplaceChild("helmet_visor", CubeListBuilder.create()
                .texOffs(105, 24).addBox(-4.6F, -1.6F, -1.0F, 9.2F, 1.6F, 1.0F),
                PartPose.offset(0F, 0.2F, -4.5F));
        part_helmet.addOrReplaceChild("helmet_ear_l", CubeListBuilder.create()
                .texOffs(0, 39).addBox(-1.0F, -2.5F, -4.5F, 1.0F, 5.0F, 9.0F),
                PartPose.offset(-4.2F, 0F, 0F));
        part_helmet.addOrReplaceChild("helmet_ear_r", CubeListBuilder.create()
                .texOffs(17, 39).addBox(0.0F, -2.5F, -4.5F, 1.0F, 5.0F, 9.0F),
                PartPose.offset(4.2F, 0F, 0F));
        part_helmet.addOrReplaceChild("helmet_back_flap", CubeListBuilder.create()
                .texOffs(34, 39).addBox(-4.5F, -2.5F, 0.5F, 9.0F, 4.5F, 1.0F),
                PartPose.offsetAndRotation(0F, 0F, 4.5F, -0.087266F, 0.0F, 0.0F));
        part_helmet.addOrReplaceChild("helmet_strap_l", CubeListBuilder.create()
                .texOffs(55, 39).addBox(-0.6F, -2.0F, -0.5F, 0.6F, 3.5F, 1.0F),
                PartPose.offset(-4.0F, 2.5F, -1.5F));
        part_helmet.addOrReplaceChild("helmet_strap_r", CubeListBuilder.create()
                .texOffs(60, 39).addBox(0.0F, -2.0F, -0.5F, 0.6F, 3.5F, 1.0F),
                PartPose.offset(4.0F, 2.5F, -1.5F));
        part_helmet.addOrReplaceChild("helmet_strap_chin", CubeListBuilder.create()
                .texOffs(65, 39).addBox(-2.0F, 0F, -0.5F, 4.0F, 1.0F, 1.0F),
                PartPose.offset(0F, 3.5F, -2.0F));
        part_helmet.addOrReplaceChild("crest_base", CubeListBuilder.create()
                .texOffs(76, 39).addBox(-0.5F, -2.0F, -1.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offset(0F, -5.5F, 0F));
        part_helmet.addOrReplaceChild("crest_mid", CubeListBuilder.create()
                .texOffs(83, 39).addBox(-0.5F, -2.0F, -1.0F, 1.0F, 2.0F, 1.7F),
                PartPose.offsetAndRotation(0F, -7.0F, -0.5F, 0.139626F, 0.0F, 0.0F));
        part_helmet.addOrReplaceChild("crest_tip", CubeListBuilder.create()
                .texOffs(90, 39).addBox(-0.5F, -1.8F, -0.5F, 1.0F, 1.8F, 1.0F),
                PartPose.offsetAndRotation(0F, -8.8F, -0.3F, -0.087266F, 0.0F, 0.0F));

        PartDefinition part_lightning = part_head.addOrReplaceChild("lightning", CubeListBuilder.create(),
                PartPose.offset(0F, -4.5F, 4.0F));
        part_lightning.addOrReplaceChild("bolt_back_1", CubeListBuilder.create()
                .texOffs(95, 39).addBox(-0.5F, -1.0F, 0.0F, 1.0F, 2.0F, 2.5F),
                PartPose.offset(0F, 0F, 1.0F));
        part_lightning.addOrReplaceChild("bolt_back_2", CubeListBuilder.create()
                .texOffs(102, 39).addBox(-0.5F, -2.5F, 2.0F, 1.0F, 2.5F, 3.0F),
                PartPose.offset(0F, 0F, 1.0F));
        part_lightning.addOrReplaceChild("bolt_back_3", CubeListBuilder.create()
                .texOffs(111, 39).addBox(-0.5F, -4.5F, 4.5F, 1.0F, 2.5F, 3.0F),
                PartPose.offset(0F, 0F, 1.0F));
        part_lightning.addOrReplaceChild("bolt_lu_1", CubeListBuilder.create()
                .texOffs(0, 52).addBox(-3.0F, -1.0F, 0.0F, 3.0F, 2.0F, 2.0F),
                PartPose.offset(-4.0F, 0F, 1.2F));
        part_lightning.addOrReplaceChild("bolt_lu_2", CubeListBuilder.create()
                .texOffs(11, 52).addBox(-5.5F, -3.0F, 0.5F, 3.0F, 2.5F, 2.5F),
                PartPose.offset(-4.0F, 0F, 1.2F));
        part_lightning.addOrReplaceChild("bolt_lu_3", CubeListBuilder.create()
                .texOffs(22, 52).addBox(-8.5F, -5.0F, 1.0F, 3.5F, 2.5F, 2.5F),
                PartPose.offset(-4.0F, 0F, 1.2F));
        part_lightning.addOrReplaceChild("bolt_lu_tip", CubeListBuilder.create()
                .texOffs(35, 52).addBox(-11.0F, -7.0F, 2.0F, 3.0F, 2.0F, 2.0F),
                PartPose.offset(-4.0F, 0F, 1.2F));
        part_lightning.addOrReplaceChild("bolt_ru_1", CubeListBuilder.create()
                .texOffs(46, 52).addBox(0.0F, -1.0F, 0.0F, 3.0F, 2.0F, 2.0F),
                PartPose.offset(4.0F, 0F, 1.2F));
        part_lightning.addOrReplaceChild("bolt_ru_2", CubeListBuilder.create()
                .texOffs(57, 52).addBox(2.5F, -3.0F, 0.5F, 3.0F, 2.5F, 2.5F),
                PartPose.offset(4.0F, 0F, 1.2F));
        part_lightning.addOrReplaceChild("bolt_ru_3", CubeListBuilder.create()
                .texOffs(68, 52).addBox(5.0F, -5.0F, 1.0F, 3.5F, 2.5F, 2.5F),
                PartPose.offset(4.0F, 0F, 1.2F));
        part_lightning.addOrReplaceChild("bolt_ru_tip", CubeListBuilder.create()
                .texOffs(81, 52).addBox(8.0F, -7.0F, 2.0F, 3.0F, 2.0F, 2.0F),
                PartPose.offset(4.0F, 0F, 1.2F));
        part_lightning.addOrReplaceChild("bolt_ll_1", CubeListBuilder.create()
                .texOffs(92, 52).addBox(-2.5F, 1.0F, -1.0F, 2.5F, 2.0F, 2.0F),
                PartPose.offset(-4.0F, 0F, 0F));
        part_lightning.addOrReplaceChild("bolt_ll_2", CubeListBuilder.create()
                .texOffs(101, 52).addBox(-5.0F, 3.0F, 0.0F, 3.0F, 2.5F, 2.0F),
                PartPose.offset(-4.0F, 0F, 0F));
        part_lightning.addOrReplaceChild("bolt_rl_1", CubeListBuilder.create()
                .texOffs(112, 52).addBox(0.0F, 1.0F, -1.0F, 2.5F, 2.0F, 2.0F),
                PartPose.offset(4.0F, 0F, 0F));
        part_lightning.addOrReplaceChild("bolt_rl_2", CubeListBuilder.create()
                .texOffs(0, 57).addBox(2.0F, 3.0F, 0.0F, 3.0F, 2.5F, 2.0F),
                PartPose.offset(4.0F, 0F, 0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(SuperElectricGatlingPeaRenderState state) {
        super.setupAnim(state);

        this.head.yRot = state.yRot * ((float) Math.PI / 180.0F);
        this.head.xRot = state.xRot * ((float) Math.PI / 180.0F);

        this.idleAnimation.apply(state.idleAnimation, state.ageInTicks);
        this.shootAnimation.apply(state.shootAnimation, state.ageInTicks);

        if (state.isCarried) {
            this.leaves.yRot = 0.0F;
            this.root.xRot = 0.05F;
        }
    }
}
