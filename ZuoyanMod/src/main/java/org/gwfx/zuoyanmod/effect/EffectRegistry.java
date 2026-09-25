package org.gwfx.zuoyanmod.effect;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import org.gwfx.zuoyanmod.Zuoyanmod;

public class EffectRegistry {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Zuoyanmod.MODID);

    public static final RegistryObject<HeartParalysisEffect> HEART_PARALYSIS =
            EFFECTS.register("mamba_force_defense", HeartParalysisEffect::new);

    public static final RegistryObject<MobEffect> INSTANT_KILL =
            EFFECTS.register("mamba_force_attack", InstantKillEffect::new);

    public static final RegistryObject<FightAgainEffect> FIGHT_AGAIN =
            EFFECTS.register("fight_again", FightAgainEffect::new);

    public static final RegistryObject<MolecularDissolutionEffect> MOLECULAR_DISSOLUTION =
            EFFECTS.register("molecular_dissolution", MolecularDissolutionEffect::new);
}
