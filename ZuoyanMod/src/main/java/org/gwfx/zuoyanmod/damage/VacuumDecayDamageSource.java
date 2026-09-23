package org.gwfx.zuoyanmod.damage;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * 「对称破缺」黑洞坍缩的爆炸伤害。
 * <p>
 * 与液态暗物质的相位侵蚀不同，这里是**普通伤害**：正常受护甲、抗性、保护附魔减免，
 * 也不带 `bypasses_armor` 标签——它是一次货真价实的能量释放，不是真伤。
 * <p>
 * 施术者作为 causingEntity 传入，用于死亡消息归因；判断"不再触发未命中逻辑"靠
 * {@code source.is(ZuoyanDamageTypes.VACUUM_DECAY)} 而不是靠 entity 是否为空。
 */
public final class VacuumDecayDamageSource {

    private VacuumDecayDamageSource() {}

    public static DamageSource create(Level level, Entity caster) {
        Holder<DamageType> holder = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ZuoyanDamageTypes.VACUUM_DECAY);
        return new DamageSource(holder, caster, caster);
    }
}
