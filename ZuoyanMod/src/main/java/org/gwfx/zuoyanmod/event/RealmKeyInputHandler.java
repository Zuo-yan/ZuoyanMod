package org.gwfx.zuoyanmod.event;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.network.PacketHandler;
import org.slf4j.Logger;

/**
 * Home 键：切换灵境的显示。
 * 1.20.1 适配：ClientTickEvent.Post→{@code TickEvent.ClientTickEvent}(END)。
 */
@Mod.EventBusSubscriber(modid = Zuoyanmod.MODID, value = Dist.CLIENT)
public final class RealmKeyInputHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private RealmKeyInputHandler() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        if (RealmKeybindHandler.TOGGLE_REALM.consumeClick()) {
            LOGGER.info("[Realm] Home pressed, sending toggle packet");
            PacketHandler.sendToggleRealm();
        }
    }
}
