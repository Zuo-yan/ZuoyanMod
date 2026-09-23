package org.gwfx.zuoyanmod.effect;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.gwfx.zuoyanmod.Zuoyanmod;

public class EffectRegistry {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Zuoyanmod.MODID);

    public static final DeferredHolder<MobEffect, MambaForceDefenseEffect> MAMBA_FORCE_DEFENSE =
            EFFECTS.register("mamba_force_defense", MambaForceDefenseEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> MAMBA_FORCE_ATTACK =
            EFFECTS.register("mamba_force_attack", MambaForceAttackEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> FIGHT_AGAIN =
            EFFECTS.register("fight_again", FightAgainEffect::new);

    public static final DeferredHolder<MobEffect, MolecularDissolutionEffect> MOLECULAR_DISSOLUTION =
            EFFECTS.register("molecular_dissolution", MolecularDissolutionEffect::new);
}