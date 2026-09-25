package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.gwfx.zuoyanmod.event.VacuumDecayBlackHoleManager;
import org.gwfx.zuoyanmod.event.VacuumDecayEventHandler;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 「真空衰变」——万能挖掘锤。四个具名效果：
 * <ol>
 *   <li><b>普朗克解构</b>：一切方块都能被正确采集（万能挖掘工具）</li>
 *   <li><b>负熵灌注</b>：手持时自身受到的伤害降低 80%（见 {@link VacuumDecayEventHandler}）</li>
 *   <li><b>分子离解</b>：命中附加 60 秒引信</li>
 *   <li><b>对称破缺</b>：引爆引信，产生黑洞聚怪 5 秒后坍缩爆炸（见 {@link VacuumDecayBlackHoleManager}）</li>
 * </ol>
 * <p>
 * <b>普朗克解构</b>在 1.20.1 里与 26.3 的做法一致：26.x 的挖掘行为由
 * {@code DataComponents.TOOL} 组件驱动，{@code Item} 的三个入口只是去读那个组件；
 * 1.20.1 的 {@code Item#getDestroySpeed / isCorrectToolForDrops / mineBlock}
 * 是真正的判定入口——两个版本都直接覆写这三个方法，覆盖全部方块。
 * <p>
 * <b>无限耐久</b>：注册用的 Tier {@code getUses() = 0} → maxDamage 0 →
 * {@code isDamageableItem} 为 false，{@code hurtAndBreak} 内部短路，也不需要修复材料。
 * 上面的 {@link #mineBlock} 因此不再扣耐久（保留覆写只为返回 {@code true} 以统计"物品使用次数"）。
 * <p>
 * <b>1.20.1 适配（横扫之刃那一处不用补代码）</b>：26.3 的
 * {@code canPerformAction} 覆写放行了 {@code SWORD_SWEEP}，是为了让"能附上横扫之刃"
 * 真的能触发横扫（26.x 的横扫判定问的是物品能力钩子）。1.20.1 的横扫判定走
 * {@code EnchantmentHelper.getSweepingDamageRatio(player)}，<b>只看附魔等级、不认物品类型</b>，
 * 而本物品继承 {@link SwordItem}、附魔可上性本来就对，所以这里一行都不用加。
 */
public class VacuumDecayItem extends SwordItem {

    /** 普朗克解构的挖掘速度：比下界合金镐(9.0)更快，但没到瞬破 */
    public static final float MINING_SPEED = 12.0F;

    public VacuumDecayItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    // ===== 普朗克解构：万能挖掘工具 =====

    @Override
    public float getDestroySpeed(ItemStack stack, net.minecraft.world.level.block.state.BlockState state) {
        return MINING_SPEED;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, net.minecraft.world.level.block.state.BlockState state) {
        return true;
    }

    @Override
    public boolean mineBlock(ItemStack stack, net.minecraft.world.level.Level level,
                             net.minecraft.world.level.block.state.BlockState state,
                             net.minecraft.core.BlockPos pos,
                             net.minecraft.world.entity.LivingEntity owner) {
        // 无限耐久 → 不调用 hurtAndBreak；返回 true 让"物品使用次数"统计仍然增长
        return true;
    }

    // ===== 描述：把全部设定写进来 =====

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        tooltip.add(Component.translatable("item.zuoyanmod.vacuum_decay.desc1"));

        tooltip.add(Component.translatable("item.zuoyanmod.vacuum_decay.desc2"));
        tooltip.add(Component.translatable("item.zuoyanmod.vacuum_decay.desc3"));
        tooltip.add(Component.translatable("item.zuoyanmod.vacuum_decay.desc4"));

        tooltip.add(Component.translatable("item.zuoyanmod.vacuum_decay.desc5"));
        tooltip.add(Component.translatable("item.zuoyanmod.vacuum_decay.neg_entropy",
                (int) (VacuumDecayEventHandler.DAMAGE_REDUCTION * 100)));

        tooltip.add(Component.translatable("item.zuoyanmod.vacuum_decay.desc6"));
        tooltip.add(Component.translatable("item.zuoyanmod.vacuum_decay.dissociation",
                VacuumDecayEventHandler.DISSOCIATION_TICKS / 20));
        tooltip.add(Component.translatable("item.zuoyanmod.vacuum_decay.desc7"));
        tooltip.add(Component.translatable("item.zuoyanmod.vacuum_decay.desc8"));
        tooltip.add(Component.translatable("item.zuoyanmod.vacuum_decay.desc9"));

        tooltip.add(Component.translatable("item.zuoyanmod.vacuum_decay.desc10"));
        tooltip.add(Component.translatable("item.zuoyanmod.vacuum_decay.desc11"));
        tooltip.add(Component.translatable("item.zuoyanmod.vacuum_decay.desc12"));
        tooltip.add(Component.translatable("item.zuoyanmod.vacuum_decay.pull",
                (int) VacuumDecayBlackHoleManager.PULL_RADIUS));
        tooltip.add(Component.translatable("item.zuoyanmod.vacuum_decay.blast",
                (int) VacuumDecayBlackHoleManager.BLAST_RADIUS,
                (int) VacuumDecayBlackHoleManager.BLAST_DAMAGE));
        tooltip.add(Component.translatable("item.zuoyanmod.vacuum_decay.desc13"));

        tooltip.add(Component.translatable("item.zuoyanmod.vacuum_decay.desc14"));
    }
}
