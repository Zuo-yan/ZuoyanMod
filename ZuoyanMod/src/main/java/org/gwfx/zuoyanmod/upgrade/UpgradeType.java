package org.gwfx.zuoyanmod.upgrade;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.gwfx.zuoyanmod.Config;

import java.util.Locale;

/**
 * 基础能力升级条目：玩家用经验等级逐条购买，每条 {@link #MAX_LEVEL} 级。
 *
 * <p>数值旋钮都集中在这一个枚举里：每级加多少（{@link #perLevel}）、走哪种属性运算
 * （{@link #operation}）、lang 键（按 {@link #name()} 小写取）。减伤不是属性，
 * 走 {@link UpgradeServerEvents} 的伤害事件（护甲结算之后做乘法减免），所以
 * {@link #attribute} 与 {@link #operation} 在这一条上为 null。
 */
public enum UpgradeType {

    /** 每级 +2 生命上限，满级 +20（10 颗心） */
    MAX_HEALTH(Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION, 2.0D),

    /** 每级 +0.5 攻击伤害，满级 +5 */
    ATTACK_DAMAGE(Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION, 0.5D),

    /** 每级 +4% 移动速度（乘法），满级 +40% */
    MOVEMENT_SPEED(Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL, 0.04D),

    /** 每级 +2 护甲值，满级 +20 */
    ARMOR(Attributes.ARMOR, AttributeModifier.Operation.ADDITION, 2.0D),

    /** 每级 -2% 受到伤害（护甲结算后的乘法减免），满级 -20%。不是属性，见类注释。 */
    DAMAGE_REDUCTION(null, null, 0.02D);

    public static final int MAX_LEVEL = 10;
    public static final UpgradeType[] VALUES = values();

    /** 升到下一级的花费：第 n+1 级花 {@code base + step * n} 级经验（默认 3,5,7…21）。 */
    public static int xpCost(int currentLevel) {
        return Config.upgradeXpCostBase + Config.upgradeXpCostStep * currentLevel;
    }

    public final Holder<Attribute> attribute;
    public final AttributeModifier.Operation operation;
    private final double perLevel;

    UpgradeType(Holder<Attribute> attribute,
                AttributeModifier.Operation operation,
                double perLevel) {
        this.attribute = attribute;
        this.operation = operation;
        this.perLevel = perLevel;
    }

    /** 每级加成幅度（MOVEMENT_SPEED / DAMAGE_REDUCTION 是小数比例，其余是点数）。 */
    public double perLevel() {
        return this.perLevel;
    }

    /** 升到 {@code level} 级时的总加成。 */
    public double totalBonus(int level) {
        return this.perLevel * level;
    }

    /** 加成条目的 lang 键（"当前 → 下级"两侧共用，只换数字）。 */
    public String valueKey() {
        return "upgrade.zuoyanmod.type." + name().toLowerCase(Locale.ROOT) + ".value";
    }

    public String nameKey() {
        return "upgrade.zuoyanmod.type." + name().toLowerCase(Locale.ROOT) + ".name";
    }
}
