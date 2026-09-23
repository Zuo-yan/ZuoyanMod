package org.gwfx.zuoyanmod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.gwfx.zuoyanmod.event.VacuumDecayBlackHoleManager;
import org.gwfx.zuoyanmod.event.VacuumDecayEventHandler;

import java.util.function.Consumer;

/**
 * 「真空衰变」——万能挖掘锤。四个具名效果：
 * <ol>
 *   <li><b>普朗克解构</b>：一切方块都能被正确采集（万能挖掘工具）</li>
 *   <li><b>负熵灌注</b>：手持时自身受到的伤害降低 80%（见 {@link VacuumDecayEventHandler}）</li>
 *   <li><b>分子离解</b>：命中附加 60 秒引信</li>
 *   <li><b>对称破缺</b>：引爆引信，产生黑洞聚怪 5 秒后坍缩爆炸（见 {@link VacuumDecayBlackHoleManager}）</li>
 * </ol>
 * <p>
 * <b>普朗克解构</b>是怎么做的：26.3 里挖掘行为由 {@code DataComponents.TOOL} 组件驱动，
 * 而 {@code Item} 的三个入口（{@link #getDestroySpeed}、{@link #isCorrectToolForDrops}、
 * {@link #mineBlock}）**只是去读那个组件**。所以当"万能工具"要覆盖全部方块时，
 * 与其去凑一个包含全部 mineable 标签的 {@code HolderSet}（注册期拿不到 registry lookup），
 * 直接覆写这三个方法更短也更准——顺手也就拿到了"挖什么都掉"。
 * <p>
 * <b>无限耐久</b>：本物品在注册时**不设 {@code durability}**，26.x 里"没有 max_damage 组件"
 * 就等于不可损坏，所以 {@code hurtAndBreak} 内部直接短路、也不需要任何修复材料。
 * 上面的 {@link #mineBlock} 因此不再扣耐久（保留覆写只为返回 {@code true} 以统计"物品使用次数"）。
 */
public class VacuumDecayItem extends Item {

    /** 普朗克解构的挖掘速度：比下界合金镐(9.0)更快，但没到瞬破 */
    public static final float MINING_SPEED = 12.0F;

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

    // ===== 描述：把全部设定写进来 =====

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);

        tooltip.accept(Component.literal("§5§l真空衰变"));

        tooltip.accept(Component.literal("§b【普朗克解构】"));
        tooltip.accept(Component.literal("§7一切方块都能被正确采集"));
        tooltip.accept(Component.literal("§8  · 无论它是石头、木头、泥土还是矿石"));

        tooltip.accept(Component.literal("§3【负熵灌注】"));
        tooltip.accept(Component.literal("§7手持时，自身受到的伤害降低 §f"
                + (int) (VacuumDecayEventHandler.DAMAGE_REDUCTION * 100) + "%"));

        tooltip.accept(Component.literal("§5【分子离解】"));
        tooltip.accept(Component.literal("§7命中目标时附加 §f"
                + (VacuumDecayEventHandler.DISSOCIATION_TICKS / 20) + " §7秒「分子离解」"));
        tooltip.accept(Component.literal("§8  · 每秒 §f4 §8点相位侵蚀真伤（无视护甲与抗性）"));
        tooltip.accept(Component.literal("§8  · 全套§b圣辉套装§8免疫该侵蚀伤害"));
        tooltip.accept(Component.literal("§8  · 饮用牛奶可清除（清掉就没有引信了）"));

        tooltip.accept(Component.literal("§d【对称破缺】"));
        tooltip.accept(Component.literal("§7攻击已带「分子离解」的目标时，引爆该效果："));
        tooltip.accept(Component.literal("§8  · 在目标位置展开真空衰变泡，持续 §f5 §8秒"));
        tooltip.accept(Component.literal("§8  · 把 §f"
                + (int) VacuumDecayBlackHoleManager.PULL_RADIUS + " §8格内除自身外的所有实体拽向中心"));
        tooltip.accept(Component.literal("§8  · 坍缩时对 §f"
                + (int) VacuumDecayBlackHoleManager.BLAST_RADIUS + " §8格内的活体造成 §f"
                + (int) VacuumDecayBlackHoleManager.BLAST_DAMAGE + " §8点伤害"));
        tooltip.accept(Component.literal("§8  · 自身不受牵引、不被爆炸波及"));

        tooltip.accept(Component.literal("§2无限耐久"));
    }
}
