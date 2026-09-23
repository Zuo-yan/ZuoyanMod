package org.gwfx.zuoyanmod.damage;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;


/** 液态暗物质的相位侵蚀伤害：穿透护甲与常规抗性，直接扣除真实生命 */
public final class DarkMatterDamageSource {

    private DarkMatterDamageSource() {}

    public static DamageSource create(Level level) {
        return new DamageSource(
                level.registryAccess()
                        .lookupOrThrow(Registries.DAMAGE_TYPE)
                        .getOrThrow(ZuoyanDamageTypes.DARK_MATTER)
        );
    }
}
