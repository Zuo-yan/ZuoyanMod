package org.gwfx.zuoyanmod.event;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.network.PacketHandler;
import org.slf4j.Logger;

@EventBusSubscriber(modid = Zuoyanmod.MODID, value = Dist.CLIENT)
public final class RealmKeyInputHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private RealmKeyInputHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.screen() != null) {
            return;
        }
        if (RealmKeybindHandler.TOGGLE_REALM.consumeClick()) {
            LOGGER.info("[Realm] Home pressed, sending toggle packet");
            PacketHandler.sendToggleRealm();
        }
    }
}
