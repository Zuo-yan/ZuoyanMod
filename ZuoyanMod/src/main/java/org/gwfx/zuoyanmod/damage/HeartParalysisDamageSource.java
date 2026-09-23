package org.gwfx.zuoyanmod.damage;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

public final class HeartParalysisDamageSource {

    private HeartParalysisDamageSource() {}

    public static DamageSource create(Level level) {
        Holder<DamageType> holder = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ZuoyanDamageTypes.HEART_PARALYSIS);
        return new DamageSource(holder);
    }
}
