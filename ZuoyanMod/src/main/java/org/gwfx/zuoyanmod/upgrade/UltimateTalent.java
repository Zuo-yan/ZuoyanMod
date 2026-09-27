package org.gwfx.zuoyanmod.upgrade;

import org.gwfx.zuoyanmod.Config;

import java.util.Locale;

/**
 * 终极天赋：全部 5 条基础能力满级后可选定一项，按 Y 键释放。
 *
 * <p>选定<b>永久不可更改</b>（{@link UpgradeData#talent} 一经写入不再接受第二次）。
 * 每项天赋自带基础冷却秒数（{@link #baseCooldownSeconds}），实际冷却 = 基础值 ×
 * 配置里的全局倍率。
 *
 * <p>效果实现全部复用项目既有组件：
 * <ul>
 *   <li>抓取 → 自写逐 tick 拽拉 + 原版冰冻/缓慢（单目标 5 秒，不走 TimeFreezeManager 的
 *       13×13×13 区域时停——那是绝对零度物品的领域语义，单目标冻结不是它）</li>
 *   <li>击飞 → 原版速度 + 虚弱 III，摔伤交给原版坠落结算</li>
 *   <li>分子离解 → 给"下一次近战"上标记，命中时施加现成的
 *       {@code EffectRegistry.MOLECULAR_DISSOLUTION}（每秒 4 点真伤）3 分钟</li>
 *   <li>黑洞 → 直接调 {@code PrimordialBlackHoleEntity.spawn}，与「原始黑洞」物品
 *       释放的完全同款（同实体、同落点规则、同"每人一个 + 全局上限"检查）</li>
 * </ul>
 */
public enum UltimateTalent {

    /** 抓取：把准星目标拽到身边并冰冻 5 秒（冷却 22s，砍半取整） */
    GRAPPLE_ABSOLUTE_ZERO(22),

    /** 天罚：击飞周围目标升空，摔落 + 虚弱 III 2 分钟（冷却 6s，按需求单独定） */
    SKY_LAUNCH(6),

    /** 灌能：30 秒内下一次近战命中，让目标分子离解 3 分钟（冷却 45s，砍半） */
    MOLECULAR_DISSOCIATION(45),

    /** 以准星落点展开原始黑洞（与原始黑洞物品同款，冷却 60s，砍半） */
    PRIMORDIAL_BLACK_HOLE(60);

    public static final UltimateTalent[] VALUES = values();

    public final int baseCooldownSeconds;

    UltimateTalent(int baseCooldownSeconds) {
        this.baseCooldownSeconds = baseCooldownSeconds;
    }

    /** 实际冷却 tick 数（含配置倍率）。 */
    public int cooldownTicks() {
        return Math.round(this.baseCooldownSeconds * 20.0F
                * (float) Config.upgradeUltimateCooldownMultiplier);
    }

    public String nameKey() {
        return "upgrade.zuoyanmod.talent." + name().toLowerCase(Locale.ROOT) + ".name";
    }

    public String descKey() {
        return "upgrade.zuoyanmod.talent." + name().toLowerCase(Locale.ROOT) + ".desc";
    }
}
