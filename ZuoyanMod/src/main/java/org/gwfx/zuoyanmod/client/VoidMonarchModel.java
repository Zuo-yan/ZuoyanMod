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
import net.minecraft.util.Mth;

/**
 * 湮灭君主高精立体模型（基于 void_monarch.bbmodel 烘焙，概念图《湮灭君主·Sovereign of Oblivion》）。
 * 巨型金冠与虚空黑洞核心、金色碎片层肩甲、权杖·湮灭之环、破碎虚空披风与虚空光轮双翼。
 */
public class VoidMonarchModel extends EntityModel<VoidMonarchRenderState> {

    private final ModelPart root;
    private final ModelPart waist;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart body;
    private final ModelPart cape;
    private final ModelPart voidWings;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart sword;
    private final ModelPart scepter;

    private final KeyframeAnimation sitAnimation;
    private final KeyframeAnimation awakenAnimation;
    private final KeyframeAnimation idleAnimation;
    private final KeyframeAnimation walkAnimation;
    private final KeyframeAnimation horizontalSlashAnimation;
    private final KeyframeAnimation overheadSlamAnimation;
    private final KeyframeAnimation phase2TransformAnimation;
    private final KeyframeAnimation deathKneelAnimation;
    private final KeyframeAnimation castBarrageAnimation;

    public VoidMonarchModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        this.root = root.getChild("root");
        this.waist = this.root.getChild("waist");
        this.leftLeg = this.waist.getChild("left_leg");
        this.rightLeg = this.waist.getChild("right_leg");
        this.body = this.waist.getChild("body");
        this.cape = this.body.getChild("cape");
        this.voidWings = this.body.getChild("void_wings");
        this.head = this.body.getChild("head");
        this.leftArm = this.body.getChild("left_arm");
        this.rightArm = this.body.getChild("right_arm");
        this.sword = this.rightArm.getChild("sword");
        this.scepter = this.rightArm.getChild("scepter");

        this.sitAnimation = VoidMonarchAnimations.SIT_DORMANT.bake(root);
        this.awakenAnimation = VoidMonarchAnimations.AWAKEN.bake(root);
        this.idleAnimation = VoidMonarchAnimations.IDLE.bake(root);
        this.walkAnimation = VoidMonarchAnimations.WALK.bake(root);
        this.horizontalSlashAnimation = VoidMonarchAnimations.ATTACK_HORIZONTAL.bake(root);
        this.overheadSlamAnimation = VoidMonarchAnimations.ATTACK_OVERHEAD.bake(root);
        this.phase2TransformAnimation = VoidMonarchAnimations.PHASE2_TRANSFORM.bake(root);
        this.deathKneelAnimation = VoidMonarchAnimations.DEATH_KNEEL.bake(root);
        this.castBarrageAnimation = VoidMonarchAnimations.CAST_BARRAGE.bake(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition part_root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(),
                PartPose.offset(0.0F, 24.0F, 0.0F));
                PartDefinition part_waist = part_root.addOrReplaceChild("waist", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.5F, -2.0F, -2.5F, 9.0F, 3.0F, 5.0F)
                .texOffs(29, 0).addBox(-1.5F, -2.5F, -2.8F, 3.0F, 4.0F, 0.5F)
                .texOffs(38, 0).addBox(-5.0F, 0.0F, -2.6F, 0.8F, 4.0F, 5.2F)
                .texOffs(51, 0).addBox(4.2F, 0.0F, -2.6F, 0.8F, 4.0F, 5.2F),
                        PartPose.offset(0.0F, -11.0F, 0.0F));
                        PartDefinition part_left_leg = part_waist.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-1.9F, 0.0F, -2.0F, 3.8F, 6.0F, 4.0F)
                .texOffs(81, 0).addBox(-1.8F, 6.0F, -1.9F, 3.6F, 4.5F, 3.8F)
                .texOffs(98, 0).addBox(-2.0F, 8.5F, -2.6F, 4.0F, 2.5F, 5.0F)
                .texOffs(117, 0).addBox(-1.5F, 4.5F, -2.6F, 3.0F, 2.5F, 0.8F),
                                PartPose.offset(-2.3F, 0.0F, 0.0F));
                        PartDefinition part_right_leg = part_waist.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 11).addBox(-1.9F, 0.0F, -2.0F, 3.8F, 6.0F, 4.0F)
                .texOffs(17, 11).addBox(-1.8F, 6.0F, -1.9F, 3.6F, 4.5F, 3.8F)
                .texOffs(34, 11).addBox(-2.0F, 8.5F, -2.6F, 4.0F, 2.5F, 5.0F)
                .texOffs(53, 11).addBox(-1.5F, 4.5F, -2.6F, 3.0F, 2.5F, 0.8F),
                                PartPose.offset(2.3F, 0.0F, 0.0F));
                        PartDefinition part_body = part_waist.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(62, 11).addBox(-4.0F, -4.0F, -2.0F, 8.0F, 4.0F, 4.0F)
                .texOffs(87, 11).addBox(-4.5F, -12.0F, -2.5F, 9.0F, 8.0F, 5.0F)
                .texOffs(116, 11).addBox(-2.0F, -10.0F, -2.8F, 4.0F, 4.0F, 0.5F)
                .texOffs(0, 25).addBox(-3.0F, -13.0F, -3.0F, 6.0F, 1.5F, 6.0F)
                .texOffs(25, 25).addBox(-8.0F, -13.5F, -3.2F, 3.5F, 4.5F, 6.4F)
                .texOffs(44, 25).addBox(4.5F, -13.5F, -3.2F, 3.5F, 4.5F, 6.4F)
                .texOffs(12, 85).addBox(-7.5F, -9.5F, -2.5F, 3.0F, 2.0F, 5.0F)
                .texOffs(28, 85).addBox(4.5F, -9.5F, -2.5F, 3.0F, 2.0F, 5.0F),
                                PartPose.offset(0.0F, -1.0F, 0.0F));
                                PartDefinition part_cape = part_body.addOrReplaceChild("cape", CubeListBuilder.create()
                .texOffs(63, 25).addBox(-5.5F, 0.0F, -0.2F, 11.0F, 8.0F, 0.6F)
                .texOffs(88, 25).addBox(-6.0F, 8.0F, 0.4F, 12.0F, 8.0F, 0.6F)
                .texOffs(0, 36).addBox(-6.5F, 16.0F, 1.2F, 13.0F, 8.0F, 0.6F)
                .texOffs(44, 85).addBox(-6.5F, 15.0F, 2.0F, 1.5F, 7.5F, 0.6F)
                .texOffs(46, 85).addBox(-1.0F, 17.0F, 2.4F, 2.0F, 7.0F, 0.6F)
                .texOffs(50, 85).addBox(5.0F, 16.0F, 2.0F, 1.5F, 7.0F, 0.6F),
                                        PartPose.offset(0.0F, -12.0F, 2.8F));
                                PartDefinition part_void_wings = part_body.addOrReplaceChild("void_wings", CubeListBuilder.create()
                .texOffs(29, 36).addBox(-6.0F, -1.5F, -0.2F, 4.0F, 3.0F, 1.0F)
                .texOffs(40, 36).addBox(-14.0F, -3.5F, 0.0F, 9.0F, 3.0F, 0.8F)
                .texOffs(61, 36).addBox(-12.0F, 1.5F, 0.0F, 8.0F, 3.0F, 0.8F)
                .texOffs(80, 36).addBox(2.0F, -1.5F, -0.2F, 4.0F, 3.0F, 1.0F)
                .texOffs(91, 36).addBox(5.0F, -3.5F, 0.0F, 9.0F, 3.0F, 0.8F)
                .texOffs(0, 46).addBox(4.0F, 1.5F, 0.0F, 8.0F, 3.0F, 0.8F),
                                        PartPose.offset(0.0F, -7.5F, 3.0F));
                                PartDefinition part_head = part_body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(19, 46).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
                .texOffs(52, 46).addBox(-2.8F, -8.4F, -4.4F, 5.6F, 5.6F, 0.5F)
                .texOffs(67, 46).addBox(-4.6F, -9.5F, -4.6F, 9.2F, 2.5F, 9.2F)
                .texOffs(104, 46).addBox(-1.0F, -14.5F, -4.7F, 2.0F, 5.0F, 1.0F)
                .texOffs(111, 46).addBox(-1.0F, -12.5F, 3.7F, 2.0F, 3.0F, 1.0F)
                .texOffs(118, 46).addBox(-4.7F, -13.0F, -1.0F, 1.0F, 3.5F, 2.0F)
                .texOffs(0, 63).addBox(3.7F, -13.0F, -1.0F, 1.0F, 3.5F, 2.0F)
                .texOffs(7, 63).addBox(-1.2F, -11.2F, -5.0F, 2.4F, 2.0F, 0.6F)
                .texOffs(0, 85).addBox(-3.2F, -13.5F, -4.7F, 1.0F, 4.0F, 1.0F)
                .texOffs(4, 85).addBox(2.2F, -13.5F, -4.7F, 1.0F, 4.0F, 1.0F)
                .texOffs(8, 85).addBox(-7.5F, -13.0F, 3.8F, 1.0F, 8.0F, 0.6F)
                .texOffs(10, 85).addBox(6.5F, -13.0F, 3.8F, 1.0F, 8.0F, 0.6F),
                                        PartPose.offset(0.0F, -12.0F, 0.0F));
                                PartDefinition part_left_arm = part_body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(14, 63).addBox(-2.0F, -0.5F, -2.0F, 3.0F, 6.0F, 4.0F)
                .texOffs(29, 63).addBox(-2.0F, 5.5F, -2.0F, 3.0F, 6.0F, 4.0F)
                .texOffs(44, 63).addBox(-2.3F, 8.5F, -2.3F, 3.6F, 4.0F, 4.6F),
                                        PartPose.offset(-5.5F, -11.0F, 0.0F));
                                PartDefinition part_right_arm = part_body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(63, 63).addBox(-1.0F, -0.5F, -2.0F, 3.0F, 6.0F, 4.0F)
                .texOffs(78, 63).addBox(-1.0F, 5.5F, -2.0F, 3.0F, 6.0F, 4.0F)
                .texOffs(93, 63).addBox(-1.3F, 8.5F, -2.3F, 3.6F, 4.0F, 4.6F),
                                        PartPose.offset(5.5F, -11.0F, 0.0F));
                                        PartDefinition part_sword = part_right_arm.addOrReplaceChild("sword", CubeListBuilder.create()
                .texOffs(112, 63).addBox(-0.6F, -2.0F, -0.6F, 1.2F, 6.0F, 1.2F)
                .texOffs(117, 63).addBox(-1.0F, 4.0F, -1.0F, 2.0F, 1.5F, 2.0F)
                .texOffs(0, 74).addBox(-3.5F, -3.7F, -1.8F, 7.0F, 1.7F, 3.6F)
                .texOffs(23, 74).addBox(-1.8F, -13.5F, -0.8F, 3.6F, 9.8F, 1.6F)
                .texOffs(36, 74).addBox(-1.6F, -23.5F, -0.8F, 3.2F, 10.0F, 1.6F)
                .texOffs(47, 74).addBox(-1.0F, -30.5F, -0.8F, 2.0F, 7.0F, 1.6F),
                                                PartPose.offset(0.5F, 11.5F, 0.0F));
                                        PartDefinition part_scepter = part_right_arm.addOrReplaceChild("scepter", CubeListBuilder.create()
                .texOffs(54, 85).addBox(-1.5F, -28.0F, -1.5F, 3.0F, 3.0F, 3.0F)
                .texOffs(66, 85).addBox(-2.0F, -29.3F, -0.4F, 4.0F, 1.0F, 0.8F)
                .texOffs(74, 85).addBox(-2.0F, -24.5F, -0.4F, 4.0F, 1.0F, 0.8F)
                .texOffs(74, 86).addBox(-2.8F, -28.5F, -0.4F, 1.0F, 3.7F, 0.8F)
                .texOffs(76, 86).addBox(1.8F, -28.5F, -0.4F, 1.0F, 3.7F, 0.8F)
                .texOffs(0, 96).addBox(-0.6F, -3.5F, -0.7F, 1.2F, 7.0F, 1.4F)
                .texOffs(4, 96).addBox(-0.6F, -13.5F, -0.7F, 1.2F, 10.0F, 1.4F)
                .texOffs(8, 96).addBox(-0.6F, -23.5F, -0.7F, 1.2F, 10.0F, 1.4F),
                                                PartPose.offsetAndRotation(1.5F, 11.5F, 0.0F, 0.0000000F, 0.0000000F, -0.2094395F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(VoidMonarchRenderState state) {
        super.setupAnim(state);
        this.root.resetPose();
        this.waist.resetPose();
        this.leftLeg.resetPose();
        this.rightLeg.resetPose();
        this.body.resetPose();
        this.cape.resetPose();
        this.voidWings.resetPose();
        this.head.resetPose();
        this.leftArm.resetPose();
        this.rightArm.resetPose();
        this.sword.resetPose();
        this.scepter.resetPose();

        // 狂暴阶段：展翅 + 收剑亮杖（一阶段君主之刃，二阶段湮灭之环）
        if (state.enraged) {
            this.voidWings.visible = true;
            this.voidWings.zRot = 0.25F;
            this.sword.visible = false;
            this.scepter.visible = true;
        } else {
            this.voidWings.visible = false;
            this.sword.visible = true;
            this.scepter.visible = false;
        }

        // 死亡跪地动画优先级最高
        if (state.isDying) {
            this.deathKneelAnimation.apply(state.deathAnimation, state.ageInTicks);
            return;
        }

        // 沉眠与起身阶段
        if (!state.awakened) {
            this.sitAnimation.apply(state.sitAnimation, state.ageInTicks);
            return;
        }
        if (state.isAwakening) {
            this.awakenAnimation.apply(state.awakenAnimation, state.ageInTicks);
            return;
        }

        // 头部朝向追踪
        this.head.yRot = state.yRot * ((float) Math.PI / 180.0F);
        this.head.xRot = state.xRot * ((float) Math.PI / 180.0F);

        // 攻击招式判定（0: 无, 1: 横扫挥砍, 2: 双手重劈, 3: 狂暴怒吼, 4: 弹幕施法）
        if (state.attackState == 1) {
            this.horizontalSlashAnimation.apply(state.attackHorizontalAnimation, state.ageInTicks);
        } else if (state.attackState == 2) {
            this.overheadSlamAnimation.apply(state.attackOverheadAnimation, state.ageInTicks);
        } else if (state.attackState == 3) {
            this.phase2TransformAnimation.apply(state.phase2Animation, state.ageInTicks);
        } else if (state.attackState == 4) {
            this.castBarrageAnimation.apply(state.attackBarrageAnimation, state.ageInTicks);
        } else {
            // 基础移动与战备循环
            this.idleAnimation.apply(state.idleAnimation, state.ageInTicks);
            this.walkAnimation.apply(state.walkAnimation, state.ageInTicks);
        }
    }
}