package org.gwfx.zuoyanmod.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.gwfx.zuoyanmod.damage.HeartParalysisDamageSource;

public class MambaForceDefenseEffect extends MobEffect {

    public MambaForceDefenseEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF0000);
    }

    // 26.3 新方法名：替代旧版 isDurationEffectTick
    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration == 1; // 倒计时结束最后 1 tick 触发
    }

    // 26.3 新方法签名：接收 ServerLevel 并返回 boolean
    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        entity.hurt(HeartParalysisDamageSource.create(level), Float.MAX_VALUE);
        return true;
    }
}