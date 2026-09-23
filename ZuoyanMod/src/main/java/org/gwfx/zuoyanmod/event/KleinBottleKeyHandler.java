package org.gwfx.zuoyanmod.event;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;
import org.gwfx.zuoyanmod.network.PacketHandler;

/** 克莱因瓶的 K 键：把背包里的随身终端召出来。 */
@EventBusSubscriber(modid = Zuoyanmod.MODID, value = Dist.CLIENT)
public final class KleinBottleKeyHandler {

    public static final KeyMapping OPEN_KLEIN_BOTTLE =
            new KeyMapping("key.zuoyanmod.open_klein_bottle", InputConstants.KEY_K, RealmKeybindHandler.CATEGORY);

    private KleinBottleKeyHandler() {}

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_KLEIN_BOTTLE);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.screen() != null) {
            return;
        }
        if (OPEN_KLEIN_BOTTLE.consumeClick()) {
            PacketHandler.sendOpenKleinBottle();
        }
    }
}
