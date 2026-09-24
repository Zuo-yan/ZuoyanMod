package org.gwfx.zuoyanmod.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.gwfx.zuoyanmod.damage.HeartParalysisDamageSource;

/**
 * 定时处刑：效果倒数到最后一 tick 时，对<b>持有者</b>造成 {@link Float#MAX_VALUE} 的心脏麻痹伤害，必死。
 *
 * <p><b>为什么改名：</b>注册 id 仍是 {@code mamba_force_defense}，lang 里英文叫 Mamba Curse、
 * 中文就叫「心脏麻痹」。但 id 里那个 defense 极具误导性 —— 它<b>不是</b>防御增益，
 * 而是给持有者下的必死诅咒（{@link net.minecraft.world.effect.MobEffectCategory#HARMFUL} 已经说明了这点）。
 * 名字按后果取，免得下次又有人把它当成 buff 来用。
 *
 * <p><b>为什么用"到期才触发"而不是施加时直接结算：</b>这样目标有一段可以自救的时间
 * （空间锚点、名刀司命这类保命手段才有用武之地），施加者也能看到倒计时。
 * 死亡笔记正是靠这一点成立的：写下名字 ≠ 立刻死，中间那段时间才是这个道具的全部内容。
 *
 * <p><b>施加途径（两条，语义相反，别搞混）：</b>
 * ① 死亡笔记（{@code DeathNotePacket}）写给<b>别人</b>；
 * ② 巧乐兹（{@code ChocolateCrispItem}）吃下去是给<b>自己</b>上 —— 也就是说巧乐兹是一根
 * "限时一击必杀 + 定时死亡"的赌博饮料，两个效果同时挂着，中间那段就是它全部的玩法。
 */
public class HeartParalysisEffect extends MobEffect {

    public HeartParalysisEffect() {
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