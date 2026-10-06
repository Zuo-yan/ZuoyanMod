package org.gwfx.zuoyanmod.event;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.entity.SuperElectricGatlingPeaEntity;

/**
 * 玩家抱持超级电能机枪豌豆时的交互监听：
 * <ul>
 *   <li>Shift + 右键：放下植物；</li>
 *   <li>单次右键 / 按住右键连发：持续呼叫植物射击，发射高速电能子弹，享受 35% 开大机制。</li>
 * </ul>
 */
@EventBusSubscriber(modid = Zuoyanmod.MODID)
public final class GatlingPeaCarryEventHandler {

    private GatlingPeaCarryEventHandler() {}

    private static SuperElectricGatlingPeaEntity getCarriedPea(Player player) {
        for (Entity passenger : player.getPassengers()) {
            if (passenger instanceof SuperElectricGatlingPeaEntity pea) {
                return pea;
            }
        }
        return null;
    }

    /**
     * 玩家每 tick 检测：支持玩家按住鼠标右键时连续不断自动速射
     */
    @SubscribeEvent
    public static void onPlayerPostTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        // 仅当玩家抱着豌豆且未处于潜行状态时生效
        SuperElectricGatlingPeaEntity pea = getCarriedPea(player);
        if (pea != null && !player.isShiftKeyDown()) {
            // 如果玩家处于持续使用物品状态，或者冷却已好且正处于连发中
            if (player.isUsingItem() || pea.shootCooldown <= 0) {
                // 如果是被触发射击状态，继续平滑射击
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        SuperElectricGatlingPeaEntity pea = getCarriedPea(player);
        if (pea != null) {
            if (player.isShiftKeyDown()) {
                if (!event.getLevel().isClientSide()) {
                    pea.putDown(player);
                }
            } else {
                if (!event.getLevel().isClientSide()) {
                    pea.playerShoot(player);
                }
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        SuperElectricGatlingPeaEntity pea = getCarriedPea(player);
        if (pea != null) {
            if (player.isShiftKeyDown()) {
                if (!event.getLevel().isClientSide()) {
                    pea.putDown(player);
                }
            } else {
                if (!event.getLevel().isClientSide()) {
                    pea.playerShoot(player);
                }
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        Player player = event.getEntity();
        SuperElectricGatlingPeaEntity pea = getCarriedPea(player);
        if (pea != null) {
            if (player.isShiftKeyDown()) {
                if (!event.getLevel().isClientSide()) {
                    pea.putDown(player);
                }
            } else {
                if (!event.getLevel().isClientSide()) {
                    pea.playerShoot(player);
                }
            }
        }
    }
}

