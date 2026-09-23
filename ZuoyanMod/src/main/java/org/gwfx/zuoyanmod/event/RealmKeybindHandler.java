package org.gwfx.zuoyanmod.event;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.gwfx.zuoyanmod.Zuoyanmod;

@EventBusSubscriber(modid = Zuoyanmod.MODID, value = Dist.CLIENT)
public final class RealmKeybindHandler {

    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Zuoyanmod.MODID, "zuoyan"));

    // 26.x 使用 SDL scancode：InputConstants.KEY_HOME
    public static final KeyMapping TOGGLE_REALM =
            new KeyMapping("key.zuoyanmod.toggle_realm", InputConstants.KEY_HOME, CATEGORY);

    private RealmKeybindHandler() {}

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_REALM);
    }
}
