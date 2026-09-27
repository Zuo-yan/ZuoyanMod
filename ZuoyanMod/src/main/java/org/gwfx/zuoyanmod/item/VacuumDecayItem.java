package org.gwfx.zuoyanmod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.gwfx.zuoyanmod.event.VacuumDecayBlackHoleManager;
import org.gwfx.zuoyanmod.event.VacuumDecayEventHandler;

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
 * <b>普朗克解构</b>是怎么做的：1.21.1 里挖掘行为由 {@code DataComponents.TOOL} 组件驱动，
 * 而 {@code Item} 的三个入口（{@link #getDestroySpeed}、{@link #isCorrectToolForDrops}、
 * {@link #mineBlock}）**只是去读那个组件**。所以当"万能工具"要覆盖全部方块时，
 * 与其去凑一个包含全部 mineable 标签的 {@code HolderSet}，直接覆写这三个方法更短也更准
 * ——顺手也就拿到了"挖什么都掉"。
 * <p>
 * <b>无限耐久</b>：本物品在注册时**不设 {@code durability}**，1.21.1 里"没有 max_damage 组件"
 * 就等于不可损坏，所以 {@code hurtAndBreak} 内部直接短路、也不需要任何修复材料。
 * 上面的 {@link #mineBlock} 因此不再扣耐久（保留覆写只为返回 {@code true} 以统计"物品使用次数"）。
 * <p>
 * <b>横扫</b>：本物品挂在 {@code #minecraft:enchantable/sweeping} 上，横扫之刃附得上去，
 * 但 1.21.1 判定"能不能横扫"只看 {@link #canPerformAction}（默认实现一律返回 false），
 * 不覆写就永远不触发 —— 附魔白附。理由与取舍同 {@link UniversalToolItem}。
 */
public class VacuumDecayItem extends Item {

    /** 普朗克解构的挖掘速度：比下界合金镐(9.0)更快，但没到瞬破 */
    public static final float MINING_SPEED = 12.0F;

    /** 可附魔值（原 26.x 注册时挂在 Properties#enchantable(15) 上，1.21.1 走物品覆盖点） */
    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    public VacuumDecayItem(Properties properties) {
        super(properties);
    }

    // ===== 普朗克解构：万能挖掘工具 =====

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return MINING_SPEED;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return true;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        // 无限耐久 → 不调用 hurtAndBreak；返回 true 让"物品使用次数"统计仍然增长
        return true;
    }

    // ===== 横扫：放行 SWORD_SWEEP =====

    /**
     * 1.21.1 里"能不能横扫"只看这一个钩子（{@code Player#isSweepAttack} 调它），
     * 而 {@code Item} 的默认实现一律返回 false，原版剑类靠 {@code ItemAbilities.DEFAULT_SWORD_ACTIONS}
     * 声明自己的能力。本物品能附魔横扫之刃却不是剑，于是附魔给出的
     * {@code sweeping_damage_ratio} 永远用不上，属于"能附魔但一定无效"的骗局。
     * <p>
     * 这里不把物品塞进 {@code #swords} —— 那是给所有模组读的公开语义，一把锤子不该自称是剑，
     * 覆写钩子才是 NeoForge 留这个 {@code ItemAbility} 的本意。
     */
    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        if (itemAbility == ItemAbilities.SWORD_SWEEP) {
            return true;
        }
        return super.canPerformAction(stack, itemAbility);
    }

    // ===== 描述：把全部设定写进来 =====

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

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
