package org.gwfx.zuoyanmod.event;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.advancement.GrassPortalTrigger;
import org.gwfx.zuoyanmod.world.GrassPortalShape;

/**
 * 草原传送门的点燃入口：拦截"打火石右键"。
 *
 * <p>为什么不监听"火被放置"再检查门框：原版打火石放置的火会先触发出
 * 原版下界门的判定链路，事件时序依赖原版实现细节；直接在
 * {@code RightClickBlock} 阶段拦下、形状合法就自己填门。
 * 形状不合法时不拦——打火石照常生火，不干扰任何原版玩法。
 *
 * <p>取消必须只在服务端：客户端 {@code RightClickBlock} 被取消会
 * 连交互包一起吞掉，服务端永远收不到这次点击（单人游戏里就是点不着）。
 * 客户端放行后本地会预测放一格火，服务端填门后的方块同步立刻覆盖它，
 * 最多闪一帧，可接受。
 *
 * <p>点燃成功（门被填满）即触发"多此一举"成就，并消耗 1 点打火石耐久。
 */
@EventBusSubscriber(modid = Zuoyanmod.MODID)
public final class GrassPortalEventHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.FLINT_AND_STEEL) || event.getLevel().isClientSide()) {
            return;
        }

        Level level = event.getLevel();
        // 打火石原本会生成火的那个位置（点击面外侧一格），门框检测从它开始
        BlockPos firePos = event.getHitVec().getBlockPos().relative(event.getHitVec().getDirection());
        Optional<GrassPortalShape> shape = GrassPortalShape.findEmptyPortalShape(level, firePos);
        if (shape.isEmpty()) {
            return;
        }

        event.setCanceled(true);
        if (level instanceof ServerLevel serverLevel) {
            shape.get().createPortalBlocks(serverLevel);
            if (event.getEntity() instanceof ServerPlayer player) {
                stack.hurtAndBreak(1, player,
                        event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                                ? net.minecraft.world.entity.EquipmentSlot.MAINHAND
                                : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
                serverLevel.playSound(
                    player,
                    firePos,
                    SoundEvents.FLINTANDSTEEL_USE,
                    SoundSource.BLOCKS,
                    1.0F,
                    serverLevel.getRandom().nextFloat() * 0.4F + 0.8F
                );
                GrassPortalTrigger.GRASS_PORTAL_IGNITED.get().trigger(player);
            }
        }
    }

    private GrassPortalEventHandler() {}
}
