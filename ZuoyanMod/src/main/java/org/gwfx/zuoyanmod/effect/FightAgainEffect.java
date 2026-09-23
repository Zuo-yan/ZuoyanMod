package org.gwfx.zuoyanmod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class FightAgainEffect extends MobEffect {

    /** 1.20.1 的 AttributeModifier 用 UUID 而不是 ResourceLocation（26.x 才改的 ID） */
    public static final UUID ATTACK_DAMAGE_MODIFIER_ID =
            UUID.nameUUIDFromBytes("zuoyanmod:effect.fight_again_damage".getBytes());
    public static final UUID MOVEMENT_SPEED_MODIFIER_ID =
            UUID.nameUUIDFromBytes("zuoyanmod:effect.fight_again_speed".getBytes());

    public FightAgainEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x8B4513);
        // 1.20.1 的签名是 (Attribute, String UUID, 数值, Operation)——第二个参数必须是 UUID 字符串
        this.addAttributeModifier(Attributes.ATTACK_DAMAGE,
                ATTACK_DAMAGE_MODIFIER_ID.toString(), 0.50, AttributeModifier.Operation.MULTIPLY_TOTAL);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED,
                MOVEMENT_SPEED_MODIFIER_ID.toString(), 0.30, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
