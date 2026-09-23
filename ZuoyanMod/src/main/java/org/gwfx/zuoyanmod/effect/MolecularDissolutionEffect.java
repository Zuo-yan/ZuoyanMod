package org.gwfx.zuoyanmod.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.gwfx.zuoyanmod.damage.DarkMatterDamageSource;
import org.gwfx.zuoyanmod.item.HallowedSet;

/**
 * 【分子离解】：暗物质侵蚀效果。**两条施加入口共用这一个效果**：
 * <ol>
 *   <li>液态暗物质接触（{@code DarkMatterEventHandler} 每秒续期，离开液体后残留数秒）</li>
 *   <li>「真空衰变」命中（一次性附加 60 秒，作为「对称破缺」的引信）</li>
 * </ol>
 * 伤害主体：效果存续期间每秒造成 4 点相位侵蚀真伤（无视护甲与抗性），
 * 伤害来源即本效果（同原版凋零/中毒模式）。
 * 视觉上的视野变暗与边缘扭曲由事件层附加的原版 DARKNESS + BLINDNESS 实现。
 * <p>
 * 反制：**全套圣辉套装免疫侵蚀伤害**。效果本身仍会挂在身上（所以「对称破缺」的黑洞
 * 对圣辉套装穿戴者依旧成立），只是不扣血——保持"紫金神装免疫暗物质侵蚀"这条公开承诺
 * 对两条入口都成立。
 */
public class MolecularDissolutionEffect extends MobEffect {

    /** 每秒相位侵蚀伤害（真伤） */
    private static final float DISSOLUTION_DAMAGE = 4.0F;
    /** 伤害节拍：每 20 tick 一跳 */
    private static final int DAMAGE_INTERVAL = 20;

    public MolecularDissolutionEffect() {
        super(MobEffectCategory.HARMFUL, 0x2B0A3D); // 暗紫罗兰
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
        return tickCount % DAMAGE_INTERVAL == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel serverLevel, LivingEntity entity, int amplification) {
        // 全套圣辉套装：免疫侵蚀伤害（不解除效果本身）
        if (HallowedSet.isWearingFull(entity)) {
            return true;
        }
        // 幅度过高时不叠伤（每跳固定 4 点，放大器留给未来扩展）
        entity.hurt(DarkMatterDamageSource.create(serverLevel), DISSOLUTION_DAMAGE);
        return true;
    }
}
