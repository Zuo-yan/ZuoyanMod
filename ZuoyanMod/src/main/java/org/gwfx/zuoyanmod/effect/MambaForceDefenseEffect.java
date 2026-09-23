package org.gwfx.zuoyanmod.effect;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.gwfx.zuoyanmod.damage.HeartParalysisDamageSource;

public class MambaForceDefenseEffect extends MobEffect {

    public MambaForceDefenseEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF0000);
    }

    // 1.20.1 的方法名是 isDurationEffectTick（26.3 才改名 shouldApplyEffectTickThisTick）
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration == 1; // 倒计时结束最后 1 tick 触发
    }

    // 1.20.1 不传 ServerLevel、返回 void（26.3 才改成那个签名）；Level 从实体身上拿
    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        entity.hurt(HeartParalysisDamageSource.create(entity.level()), Float.MAX_VALUE);
    }
}

