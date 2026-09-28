package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;
import org.gwfx.zuoyanmod.entity.CausalityBulletEntity;
import org.gwfx.zuoyanmod.item.ItemRegistry;
import org.gwfx.zuoyanmod.sound.SoundRegistry;

/**
 * 因果律手枪：瑞克老爷处获得的规则级武器，靠反物质子弹供弹。
 *
 * <p>核心机制是「平行宇宙同位体」——子弹命中实体时<b>不直接结算伤害</b>：
 * <ul>
 *   <li>命中普通生物：从平行宇宙拉一个目标的同位体为你作战，同位体与本体
 *       互相锁定死斗，无掉落无经验，15 秒后在本宇宙消散；</li>
 *   <li>命中玩家或 Boss：无法召唤同位体，改判一道无视护甲的湮灭射线
 *       （巨额伤害，见 {@code MultiverseCloneService.RAY_DAMAGE}）。</li>
 * </ul>
 *
 * <p>消耗规则：生存模式每发消耗背包中 1 枚反物质子弹，不足则无法发射；
 * 创造模式免费。两种模式均施加 {@value COOLDOWN_TICKS} tick 射击冷却，右键连点无效。
 */
public class CausalityPistolItem extends Item {

    /** 射击冷却（tick）——右键连点无效 */
    public static final int COOLDOWN_TICKS = 15;

    public CausalityPistolItem(Properties properties) {
        super(properties);
    }

    /**
     * 物品描述（26.3 五参签名，同 {@link DescribedItem} 先例）：
     * 前三行讲机制，第四行单独高亮弹药消耗。
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("item.zuoyanmod.causality_pistol.desc1"));
        tooltip.accept(Component.translatable("item.zuoyanmod.causality_pistol.desc2"));
        tooltip.accept(Component.translatable("item.zuoyanmod.causality_pistol.desc3"));
        tooltip.accept(Component.translatable("item.zuoyanmod.causality_pistol.desc4"));
    }

    /**
     * 右键发射「平行宇宙射线」。
     *
     * <p>生存模式每发消耗背包中 1 枚反物质子弹（不足则发射失败，无音效无动画，
     * 与原版弓无箭一致）；创造模式免费。
     *
     * <p>子弹不直接结算伤害：命中实体后移交
     * {@link CausalityBulletEntity#onHitEntity} 的同位体/湮灭射线规则。
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 冷却中：连点无效
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }

        // 生存模式：先检查再消耗 1 枚反物质子弹（创造免费）
        if (!player.isCreative()) {
            int ammoSlot = player.getInventory()
                    .findSlotMatchingItem(new ItemStack(ItemRegistry.ANTIMATTER_BULLET.get()));
            if (ammoSlot == -1) {
                // 没有弹药：发射失败（无音效无动画，与原版弓无箭一致）
                return InteractionResult.FAIL;
            }
            player.getInventory().removeItem(ammoSlot, 1);
        }

        // 服务端发射子弹；客户端只负责播放挥手动画
        if (level instanceof ServerLevel serverLevel) {
            CausalityBulletEntity bullet = new CausalityBulletEntity(serverLevel, player);
            // 初速 5.5（约为满蓄力箭速的 1.8 倍，快而不瞬移）、散布 0.2：手枪级精度
            bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 5.5F, 0.2F);
            serverLevel.addFreshEntity(bullet);
            // 射击音：真实手枪射击声（TACZ 沙漠之鹰，GPL-3.0），替代原版烟花爆炸杂凑音
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundRegistry.CAUSALITY_PISTOL_SHOOT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        return InteractionResult.SUCCESS;
    }
}
