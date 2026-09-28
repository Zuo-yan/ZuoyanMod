package org.gwfx.zuoyanmod.effect;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.gwfx.zuoyanmod.Zuoyanmod;

public class EffectRegistry {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Zuoyanmod.MODID);

    // 注册 id 仍然沿用 mamba_* ：它是对外契约（旧存档里保存的效果、玩家的 /effect 命令、
    // 以及 lang 键 effect.zuoyanmod.mamba_force_* 都是按 id 取的），改名等于让旧存档里的效果失效。
    // 所以名字只改 Java 侧：类名按真实行为取，id 保持不动。
    public static final DeferredHolder<MobEffect, HeartParalysisEffect> HEART_PARALYSIS =
            EFFECTS.register("mamba_force_defense", HeartParalysisEffect::new);

    public static final DeferredHolder<MobEffect, InstantKillEffect> INSTANT_KILL =
            EFFECTS.register("mamba_force_attack", InstantKillEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> FIGHT_AGAIN =
            EFFECTS.register("fight_again", FightAgainEffect::new);

    public static final DeferredHolder<MobEffect, MolecularDissolutionEffect> MOLECULAR_DISSOLUTION =
            EFFECTS.register("molecular_dissolution", MolecularDissolutionEffect::new);
}