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

        tooltip.add(Component.literal("§5§l真空衰变"));

        tooltip.add(Component.literal("§b【普朗克解构】"));
        tooltip.add(Component.literal("§7一切方块都能被正确采集"));
        tooltip.add(Component.literal("§8  · 无论它是石头、木头、泥土还是矿石"));

        tooltip.add(Component.literal("§3【负熵灌注】"));
        tooltip.add(Component.literal("§7手持时，自身受到的伤害降低 §f"
                + (int) (VacuumDecayEventHandler.DAMAGE_REDUCTION * 100) + "%"));

        tooltip.add(Component.literal("§5【分子离解】"));
        tooltip.add(Component.literal("§7命中目标时附加 §f"
                + (VacuumDecayEventHandler.DISSOCIATION_TICKS / 20) + " §7秒「分子离解」"));
        tooltip.add(Component.literal("§8  · 每秒 §f4 §8点相位侵蚀真伤（无视护甲与抗性）"));
        tooltip.add(Component.literal("§8  · 全套§b圣辉套装§8免疫该侵蚀伤害"));
        tooltip.add(Component.literal("§8  · 饮用牛奶可清除（清掉就没有引信了）"));

        tooltip.add(Component.literal("§d【对称破缺】"));
        tooltip.add(Component.literal("§7攻击已带「分子离解」的目标时，引爆该效果："));
        tooltip.add(Component.literal("§8  · 在目标位置展开真空衰变泡，持续 §f5 §8秒"));
        tooltip.add(Component.literal("§8  · 把 §f"
                + (int) VacuumDecayBlackHoleManager.PULL_RADIUS + " §8格内除自身外的所有实体拽向中心"));
        tooltip.add(Component.literal("§8  · 坍缩时对 §f"
                + (int) VacuumDecayBlackHoleManager.BLAST_RADIUS + " §8格内的活体造成 §f"
                + (int) VacuumDecayBlackHoleManager.BLAST_DAMAGE + " §8点伤害"));
        tooltip.add(Component.literal("§8  · 自身不受牵引、不被爆炸波及"));

        tooltip.add(Component.literal("§2无限耐久"));
    }
}
