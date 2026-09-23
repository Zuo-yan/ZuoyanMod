package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.gwfx.zuoyanmod.event.TimeFreezeManager;

import java.util.function.Consumer;

/**
 * 绝对零度：右键释放「玻色-爱因斯坦凝聚」。
 * <p>
 * 把周围 {@link TimeFreezeManager#RADIUS} 格内的温度压到临界温度以下，玻色子坍缩到同一量子基态。
 * 宏观上只有两个效果，别的一概不做：
 * <ol>
 *   <li><b>时停</b>：范围内所有实体静止 {@link TimeFreezeManager#FREEZE_TICKS} tick（15 秒）。</li>
 *   <li><b>相变</b>：范围内的暗物质掉落物就地压成<b>超流体暗物质</b>
 *       （零粘度凝聚态，即原来的液态暗物质，危险属性完全不变）。</li>
 * </ol>
 * 耐久 3 次：三次用尽物品销毁。上限存在的原因是它造的流体是致命环境，
 * 无限次会让 PvP 直接崩坏（见 docs/absolute_zero.md）。
 */
public class AbsoluteZeroItem extends Item {

    /** 耐久 3 次 */
    public static final int MAX_USES = 3;

    public AbsoluteZeroItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        // 场上已有未消散的凝聚场：拒绝，避免连点白白烧掉一次耐久
        if (TimeFreezeManager.hasActiveField(player)) {
            player.sendSystemMessage(Component.literal("§7凝聚场尚未消散"));
            return InteractionResult.FAIL;
        }

        TimeFreezeManager.cast(serverLevel, player);

        ItemStack stack = player.getItemInHand(hand);
        stack.hurtAndBreak(1, player, hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.literal("§3绝对零度"));
        tooltip.accept(Component.literal("§7右键: 释放「玻色-爱因斯坦凝聚」"));
        tooltip.accept(Component.literal("§7· 冻结周围实体 §f15 §7秒"));
        tooltip.accept(Component.literal("§7· 地上的暗物质 → §d超流体暗物质"));
        tooltip.accept(Component.literal("§2耐久: " + MAX_USES + " 次"));
    }
}
