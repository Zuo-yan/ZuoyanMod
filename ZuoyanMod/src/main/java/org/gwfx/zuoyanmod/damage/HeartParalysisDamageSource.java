package org.gwfx.zuoyanmod.damage;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;

public final class HeartParalysisDamageSource {

    private HeartParalysisDamageSource() {}

    public static DamageSource create(Level level) {
        return new DamageSource(
                level.registryAccess()
                        .lookupOrThrow(Registries.DAMAGE_TYPE)
                        .getOrThrow(ZuoyanDamageTypes.HEART_PARALYSIS)
        );
    }
}