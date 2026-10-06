package org.gwfx.zuoyanmod.mixin.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.gwfx.zuoyanmod.client.ClientPeaCarryHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端玩家模型 Mixin：当玩家抱持超级电能机枪豌豆时，将双手姿势设置为环抱/拿地图般的双手抱持姿态。
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends HumanoidRenderState> {

    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart head;

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void zuoyanmod$poseCarriedPeaArms(T state, CallbackInfo ci) {
        if (state instanceof AvatarRenderState avatarState) {
            if (ClientPeaCarryHelper.isCarryingPea(avatarState.id)) {
                // 双手向上抬起并向内收拢，宛如手拿地图、环抱加特林机枪的双手抱持动作
                this.rightArm.xRot = -0.85F + this.head.xRot * 0.4F;
                this.rightArm.yRot = -0.32F + this.head.yRot * 0.4F;
                this.rightArm.zRot = 0.08F;

                this.leftArm.xRot = -0.85F + this.head.xRot * 0.4F;
                this.leftArm.yRot = 0.32F + this.head.yRot * 0.4F;
                this.leftArm.zRot = -0.08F;
            }
        }
    }
}
