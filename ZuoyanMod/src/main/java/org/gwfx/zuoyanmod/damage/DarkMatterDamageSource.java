package org.gwfx.zuoyanmod.damage;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;


/** 液态暗物质的相位侵蚀伤害：穿透护甲与常规抗性，直接扣除真实生命 */
public final class DarkMatterDamageSource {

    private DarkMatterDamageSource() {}

    public static DamageSource create(Level level) {
        // 1.20.1 的 DamageType 是注册表 Holder；26.x 的 lookupOrThrow/getOrThrow 在这里是 registryOrThrow/getHolderOrThrow
        Holder<net.minecraft.world.damagesource.DamageType> holder = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ZuoyanDamageTypes.DARK_MATTER);
        return new DamageSource(holder);
    }
}
