package org.gwfx.zuoyanmod.damage;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * 「平行宇宙射线」湮灭伤害：因果律手枪对 Boss 级目标的降级结算。
 *
 * <p>伤害类型为穿透伤害：JSON 里带 {@code bypasses_armor}，无视护甲直接扣血，
 * 用于「Boss 无法被克隆，改为承受高额穿透伤害」的规则落地。
 *
 * <p>两个 {@code create} 重载遵循项目惯例：
 * 带 caster 参数用于死亡消息归因（射手击杀 Boss 时显示射手击杀）；
 * 不带 caster 时为无归因伤害（宁可无归因也不丢结算）。
 *
 * <p><b>1.20.1 适配</b>：
 * <ul>
 *   <li>伤害来源必须持有注册表的 {@code Holder<DamageType>} 而不是 26.3 的
 *       {@code Holder<DamageType>} 直给出参——取值管道从
 *       {@code lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key)}（26.3）换成
 *       {@code registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key)}（1.20.1），
 *       与 {@link HeartParalysisDamageSource} / {@link VacuumDecayDamageSource} 保持一致；</li>
 *   <li>{@link Entity} 归因版构造同为 {@code DamageSource(holder, directEntity, causingEntity)}，
 *       签名没变，caster 依旧同时作为 direct 与 causing 实体；</li>
 *   <li>{@code @Nullable} 用 {@code javax.annotation.Nullable}（本项目 1.20.1 分支的约定，
 *       26.3 主线用的是 {@code org.jetbrains.annotations.Nullable}）。</li>
 * </ul>
 */
public final class MultiverseRayDamageSource {

    private MultiverseRayDamageSource() {}

    /** 无归因版本：伤害来源只有规则本身（平行宇宙的湮灭），没有具体射手 */
    public static DamageSource create(Level level) {
        return new DamageSource(holder(level));
    }

    /** 归因版本：caster 同时作为 directEntity 与 causingEntity，用于死亡消息归因 */
    public static DamageSource create(Level level, @Nullable Entity caster) {
        return new DamageSource(holder(level), caster, caster);
    }

    /** 统一的 Holder 取值：1.20.1 用 registryOrThrow + getHolderOrThrow */
    private static Holder<DamageType> holder(Level level) {
        return level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ZuoyanDamageTypes.MULTIVERSE_RAY);
    }
}
