package org.gwfx.zuoyanmod.damage;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 「平行宇宙射线」湮灭伤害：因果律手枪对 Boss 级目标的降级结算。
 *
 * <p>伤害类型为穿透伤害：JSON 里带 {@code bypasses_armor}，无视护甲直接扣血，
 * 用于「Boss 无法被克隆，改为承受高额穿透伤害」的规则落地。
 *
 * <p>两个 {@code create} 重载遵循项目惯例：
 * 带 caster 参数用于死亡消息归因（射手击杀 Boss 时显示射手击杀）；
 * 不带 caster 时为无归因伤害（宁可无归因也不丢结算）。
 */
public final class MultiverseRayDamageSource {

    private MultiverseRayDamageSource() {}

    /** 无归因版本：伤害来源只有规则本身（平行宇宙的湮灭），没有具体射手 */
    public static DamageSource create(Level level) {
        return new DamageSource(
                level.registryAccess()
                        .lookupOrThrow(Registries.DAMAGE_TYPE)
                        .getOrThrow(ZuoyanDamageTypes.MULTIVERSE_RAY)
        );
    }

    /** 归因版本：caster 同时作为 directEntity 与 causingEntity，用于死亡消息归因 */
    public static DamageSource create(Level level, @Nullable Entity caster) {
        return new DamageSource(
                level.registryAccess()
                        .lookupOrThrow(Registries.DAMAGE_TYPE)
                        .getOrThrow(ZuoyanDamageTypes.MULTIVERSE_RAY),
                caster,
                caster
        );
    }
}
