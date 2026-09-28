package org.gwfx.zuoyanmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.event.RealmKeybindHandler;
import org.gwfx.zuoyanmod.network.PacketHandler;

/**
 * 升级系统的两个按键（分类复用 {@link RealmKeybindHandler#CATEGORY}，由它统一登记）：
 * <ul>
 *   <li><b>U</b>：打开升级界面（纯客户端 Screen + 顺手要一份档案快照）</li>
 *   <li><b>Y</b>：触发终极天赋（发包给服务端执行，客户端不做权威校验）</li>
 * </ul>
 */
@EventBusSubscriber(modid = Zuoyanmod.MODID, value = Dist.CLIENT)
public final class UpgradeKeyHandler {

    public static final KeyMapping OPEN_UPGRADE =
            new KeyMapping("key.zuoyanmod.open_upgrade", InputConstants.KEY_U, RealmKeybindHandler.CATEGORY);
    public static final KeyMapping TRIGGER_ULTIMATE =
            new KeyMapping("key.zuoyanmod.ultimate", InputConstants.KEY_Y, RealmKeybindHandler.CATEGORY);

    private UpgradeKeyHandler() {}

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        // 分类由 RealmKeybindHandler 登记，这里只登记按键，避免同一个分类被 registerCategory 两次。
        event.register(OPEN_UPGRADE);
        event.register(TRIGGER_ULTIMATE);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.screen() != null) {
            return;
        }
        if (OPEN_UPGRADE.consumeClick()) {
            PacketHandler.sendRequestUpgradeSync();
            minecraft.gui.setScreen(new UpgradeScreen());
        }
        if (TRIGGER_ULTIMATE.consumeClick()) {
            PacketHandler.sendTriggerUltimate();
        }
    }
}
