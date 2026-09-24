package org.gwfx.zuoyanmod.damage;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * 「原始黑洞」坍缩时的爆发伤害。
 *
 * <p>与真空衰变的坍缩爆炸同性质：**普通伤害**，正常吃护甲、抗性、保护附魔，
 * 不带 {@code bypasses_armor}。它是一次能量释放，不是真伤。
 *
 * <p>与 {@code VacuumDecayDamageSource} 的唯一区别是伤害类型不同 ——
 * 单独开一个 {@code zuoyanmod:primordial_black_hole} 是为了让死亡消息说得准
 * （"被原始黑洞的坍缩吞噬"），而不是笼统地显示成真空衰变。
 *
 * <p>施法者会被当作 causingEntity 传进来做归因，但**允许为 null**：
 * 黑洞能活 10 秒，这期间玩家完全可能已经掉线。这时 {@code getPlayer(uuid)} 返回 null，
 * 我们宁可这一下没有归因（死亡消息退化成不带施法者那版），也不跳过伤害结算。
 */
public final class PrimordialBlackHoleDamageSource {

    private PrimordialBlackHoleDamageSource() {}

    public static DamageSource create(Level level, @Nullable Entity caster) {
        return new DamageSource(
                level.registryAccess()
                        .lookupOrThrow(Registries.DAMAGE_TYPE)
                        .getOrThrow(ZuoyanDamageTypes.PRIMORDIAL_BLACK_HOLE),
                caster,
                caster
        );
    }
}
