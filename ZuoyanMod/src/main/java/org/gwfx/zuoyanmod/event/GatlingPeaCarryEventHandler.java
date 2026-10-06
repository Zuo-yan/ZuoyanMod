package org.gwfx.zuoyanmod.event;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
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
        AABB box = player.getBoundingBox().inflate(3.0D);
        for (SuperElectricGatlingPeaEntity pea : player.level().getEntitiesOfClass(SuperElectricGatlingPeaEntity.class, box)) {
            if (pea.getCarrierId() == player.getId()) {
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
            // 如果玩家处于持续使用物品状态且冷却完毕，自动连续发射
            if (player.isUsingItem() && pea.shootCooldown <= 0 && pea.ultTicksRemaining <= 0) {
                pea.playerShoot(player);
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

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        SuperElectricGatlingPeaEntity pea = getCarriedPea(player);
        if (pea != null) {
            if (!player.isShiftKeyDown()) {
                if (!event.getLevel().isClientSide()) {
                    pea.playerShoot(player);
                }
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        SuperElectricGatlingPeaEntity pea = getCarriedPea(player);
        if (pea != null) {
            if (!event.getEntity().level().isClientSide()) {
                pea.playerShoot(player);
            }
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        SuperElectricGatlingPeaEntity pea = getCarriedPea(player);
        if (pea != null) {
            if (!event.getLevel().isClientSide()) {
                pea.playerShoot(player);
            }
            event.setCanceled(true);
        }
    }
}

