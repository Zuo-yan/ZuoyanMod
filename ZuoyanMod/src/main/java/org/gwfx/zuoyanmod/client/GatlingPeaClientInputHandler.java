package org.gwfx.zuoyanmod.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.entity.SuperElectricGatlingPeaEntity;
import org.gwfx.zuoyanmod.network.CarriedPeaActionPacket;
import org.gwfx.zuoyanmod.network.PacketHandler;

/**
 * 客户端输入拦截监听：抱持超级电能机枪豌豆时，拦截鼠标按键并直接触发机枪攻击或放下。
 */
@EventBusSubscriber(modid = Zuoyanmod.MODID, value = Dist.CLIENT)
public final class GatlingPeaClientInputHandler {

    private GatlingPeaClientInputHandler() {}

    @SubscribeEvent
    public static void onInteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        SuperElectricGatlingPeaEntity pea = ClientPeaCarryHelper.getLocalCarriedPea();
        if (pea != null) {
            // 使用键（默认鼠标右键）
            if (event.getKeyMapping() == mc.options.keyUse) {
                if (mc.player.isShiftKeyDown()) {
                    PacketHandler.sendCarriedPeaAction(CarriedPeaActionPacket.ACTION_PUT_DOWN);
                } else {
                    PacketHandler.sendCarriedPeaAction(CarriedPeaActionPacket.ACTION_SHOOT);
                }
                event.setCanceled(true);
                event.setSwingHand(false);
            }
            // 攻击键（默认鼠标左键）
            else if (event.getKeyMapping() == mc.options.keyAttack) {
                PacketHandler.sendCarriedPeaAction(CarriedPeaActionPacket.ACTION_SHOOT);
                event.setCanceled(true);
                event.setSwingHand(false);
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
       if (mc.player == null || mc.level == null || (mc.gui != null && mc.gui.screen() != null)) return;

        SuperElectricGatlingPeaEntity pea = ClientPeaCarryHelper.getLocalCarriedPea();
        if (pea != null && !mc.player.isShiftKeyDown()) {
            // 长按鼠标左键或右键持续射击（当冷却完毕且未在开大扫射时）
            boolean isAttacking = mc.options.keyAttack.isDown() || mc.options.keyUse.isDown();
            if (isAttacking && pea.shootCooldown <= 0 && pea.ultTicksRemaining <= 0) {
                PacketHandler.sendCarriedPeaAction(CarriedPeaActionPacket.ACTION_SHOOT);
            }
        }
    }
}
