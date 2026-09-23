package org.gwfx.zuoyanmod.effect;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.gwfx.zuoyanmod.Zuoyanmod;

public class FightAgainEffect extends MobEffect {

    public static final Identifier ATTACK_DAMAGE_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "effect.fight_again_damage");
    public static final Identifier MOVEMENT_SPEED_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "effect.fight_again_speed");

    public FightAgainEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x8B4513);
        // 使用 26.3 的 ADD_MULTIPLIED_TOTAL 以及 Identifier ID
        this.addAttributeModifier(
                Attributes.ATTACK_DAMAGE,
                ATTACK_DAMAGE_MODIFIER_ID,
                0.50,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                MOVEMENT_SPEED_MODIFIER_ID,
                0.30,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }
}