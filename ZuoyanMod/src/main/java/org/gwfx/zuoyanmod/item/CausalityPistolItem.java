package org.gwfx.zuoyanmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import org.gwfx.zuoyanmod.entity.CausalityBulletEntity;
import org.gwfx.zuoyanmod.sound.SoundRegistry;

import javax.annotation.Nullable;
import java.util.List;

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
 *
 * <p><b>1.20.1 适配</b>：
 * <ul>
 *   <li>物品描述：26.3 五参 {@code appendHoverText(stack, TooltipContext, TooltipDisplay,
 *       Consumer<Component>, TooltipFlag)} → 1.20.1 的四参
 *       {@code appendHoverText(stack, @Nullable Level, List<Component>, TooltipFlag)}
 *       （1.20.1 没有 {@code TooltipContext} / {@code TooltipDisplay}，也没有
 *       {@code Consumer} 形式，直接 {@code List#add}，同 {@link KleinBottleItem}）；</li>
 *   <li>右键：1.20.1 的 {@code Item#use} 返回
 *       {@code InteractionResultHolder<ItemStack>}（不是 {@code InteractionResult}），
 *       用 {@code InteractionResultHolder.pass/fail/success(stack)} 包装同一个判定结果；</li>
 *   <li>冷却：1.20.1 的 {@code ItemCooldowns#isOnCooldown} / {@code addCooldown} 收
 *       <b>{@code Item}</b> 而不是 26.3 收 {@code ItemStack}，所以在 {@code stack.getItem()} 上调用；</li>
 *   <li>弹药查找：26.3 的 {@code Inventory#findSlotMatchingItem(ItemStack)} 在 1.20.1
 *       不存在，自己按 {@code ItemStack.isSameItemSameTags} 扫一遍背包（判定语义一致：
 *       物品 + NBT 都要相同）；</li>
 *   <li>{@code @Nullable} 用 {@code javax.annotation.Nullable}（项目 1.20.1 约定）。</li>
 * </ul>
 */
public class CausalityPistolItem extends Item {

    /** 射击冷却（tick）——右键连点无效 */
    public static final int COOLDOWN_TICKS = 15;

    public CausalityPistolItem(Properties properties) {
        super(properties);
    }

    /**
     * 物品描述（1.20.1 四参签名）：
     * 前三行讲机制，第四行单独高亮弹药消耗。
     */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.zuoyanmod.causality_pistol.desc1"));
        tooltip.add(Component.translatable("item.zuoyanmod.causality_pistol.desc2"));
        tooltip.add(Component.translatable("item.zuoyanmod.causality_pistol.desc3"));
        tooltip.add(Component.translatable("item.zuoyanmod.causality_pistol.desc4"));
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
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 冷却中：连点无效
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResultHolder.pass(stack);
        }

        // 生存模式：先检查再消耗 1 枚反物质子弹（创造免费）
        if (!player.isCreative()) {
            int ammoSlot = findAmmoSlot(player.getInventory());
            if (ammoSlot == -1) {
                // 没有弹药：发射失败（无音效无动画，与原版弓无箭一致）
                return InteractionResultHolder.fail(stack);
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

        player.getCooldowns().addCooldown(stack.getItem(), COOLDOWN_TICKS);
        return InteractionResultHolder.success(stack);
    }

    /**
     * 在背包里找第一格反物质子弹：返回槽位号，没有则返回 -1。
     *
     * <p>1.20.1 的 {@link Inventory} 没有 26.3 的 {@code findSlotMatchingItem}，
     * 自己扫一遍；判定条件沿用主线（物品相同且 NBT 相同 → {@code ItemStack.isSameItemSameTags}）。
     */
    private static int findAmmoSlot(Inventory inventory) {
        ItemStack ammo = new ItemStack(ItemRegistry.ANTIMATTER_BULLET.get());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (!candidate.isEmpty() && ItemStack.isSameItemSameTags(candidate, ammo)) {
                return slot;
            }
        }
        return -1;
    }
}
